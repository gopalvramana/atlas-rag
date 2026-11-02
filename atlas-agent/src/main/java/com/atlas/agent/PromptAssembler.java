package com.atlas.agent;

import com.atlas.core.model.SearchResult;
import com.atlas.core.model.RetrievedContext;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the final prompt sent to the LLM.
 *
 * Budget allocation:
 *   model context window
 *     − system prompt tokens
 *     − user question tokens
 *     − output reserve
 *   = available budget for retrieved chunks
 *
 * Chunks are selected in rank order (highest first) until the budget is
 * exhausted. Overflow is handled by trimming from the bottom of the ranked
 * list; if no chunks fit at all the caller receives an empty list and must
 * return a structured "cannot answer" response.
 */
@Service
public class PromptAssembler {

    private static final Logger log = LoggerFactory.getLogger(PromptAssembler.class);

    static final String SYSTEM_PROMPT = """
            You are a precise technical assistant for Spring AI documentation.

            Rules:
            1. Answer ONLY using information from the context blocks provided below.
            2. If the context does not contain enough information, say exactly:
               "I don't have enough information in the provided context to answer this question."
            3. Cite the source file and version for every factual claim using the format [source_file @ version].
            4. Do not guess, infer, or use knowledge outside the provided context.
            5. Be concise and direct.
            """;

    private final Encoding tokenizer;
    private final int modelContextWindow;
    private final int outputTokenReserve;

    public PromptAssembler(
            @Value("${atlas.llm.context-window:16385}") int modelContextWindow,
            @Value("${atlas.llm.output-reserve:1500}") int outputTokenReserve) {
        this.modelContextWindow = modelContextWindow;
        this.outputTokenReserve = outputTokenReserve;
        this.tokenizer = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
    }

    public AssembledPrompt assemble(String question, RetrievedContext retrieved) {
        int systemTokens   = countTokens(SYSTEM_PROMPT);
        int questionTokens = countTokens(question);
        int overhead       = systemTokens + questionTokens + outputTokenReserve;
        int chunkBudget    = modelContextWindow - overhead;

        log.info("PromptAssembler: window={} system={} question={} reserve={} → chunk budget={}",
                modelContextWindow, systemTokens, questionTokens, outputTokenReserve, chunkBudget);

        if (chunkBudget <= 0) {
            log.warn("PromptAssembler: no budget left for chunks after overhead={}", overhead);
            return new AssembledPrompt(SYSTEM_PROMPT, question, List.of(), 0,
                    systemTokens, questionTokens, outputTokenReserve, modelContextWindow);
        }

        List<SearchResult> selected = new ArrayList<>();
        int consumed = 0;
        for (SearchResult chunk : retrieved.chunks()) {
            if (consumed + chunk.tokenCount() > chunkBudget) {
                log.debug("PromptAssembler: trimming chunk {} ({} tokens) — budget exhausted",
                        chunk.id(), chunk.tokenCount());
                continue;
            }
            selected.add(chunk);
            consumed += chunk.tokenCount();
        }

        log.info("PromptAssembler: {} / {} chunks selected, {} / {} chunk-budget tokens used",
                selected.size(), retrieved.chunks().size(), consumed, chunkBudget);

        return new AssembledPrompt(SYSTEM_PROMPT, question, selected, consumed,
                systemTokens, questionTokens, outputTokenReserve, modelContextWindow);
    }

    public String buildContextBlock(List<SearchResult> chunks) {
        if (chunks.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        sb.append("--- Retrieved Context ---\n\n");
        for (int i = 0; i < chunks.size(); i++) {
            SearchResult c = chunks.get(i);
            sb.append(String.format("[%d] Source: %s | Version: %s | Chunk: %d%n",
                    i + 1, c.sourceFile(), c.version(), c.chunkIndex()));
            sb.append(c.content()).append("\n\n");
        }
        return sb.toString();
    }

    private int countTokens(String text) {
        return tokenizer.countTokensOrdinary(text);
    }

    public record AssembledPrompt(
            String systemPrompt,
            String question,
            List<SearchResult> selectedChunks,
            int chunkTokensUsed,
            int systemTokens,
            int questionTokens,
            int outputReserve,
            int modelContextWindow) {

        public boolean hasContext() {
            return !selectedChunks.isEmpty();
        }

        public int totalInputTokens() {
            return systemTokens + questionTokens + chunkTokensUsed;
        }
    }
}
