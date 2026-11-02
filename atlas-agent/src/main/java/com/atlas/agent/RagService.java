package com.atlas.agent;

import com.atlas.core.model.RagResponse;
import com.atlas.core.model.RetrievedContext;
import com.atlas.retrieval.search.RetrievalPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the full RAG flow:
 *   RetrievalPipeline → PromptAssembler → ChatModel → RagResponse
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String CANNOT_ANSWER =
            "I don't have enough information in the provided context to answer this question.";

    private final RetrievalPipeline retrievalPipeline;
    private final PromptAssembler promptAssembler;
    private final ChatModel chatModel;

    public RagService(
            RetrievalPipeline retrievalPipeline,
            PromptAssembler promptAssembler,
            ChatModel chatModel) {
        this.retrievalPipeline = retrievalPipeline;
        this.promptAssembler   = promptAssembler;
        this.chatModel         = chatModel;
    }

    public RagResponse ask(String question) {
        log.info("RagService: question='{}'", truncate(question, 80));

        RetrievedContext retrieved = retrievalPipeline.retrieve(question);
        PromptAssembler.AssembledPrompt assembled = promptAssembler.assemble(question, retrieved);

        if (!assembled.hasContext()) {
            log.warn("RagService: no chunks fit in prompt budget — returning cannot-answer");
            return new RagResponse(
                    CANNOT_ANSWER,
                    List.of(),
                    new RagResponse.TokenUsage(assembled.totalInputTokens(), 0, assembled.totalInputTokens()));
        }

        String contextBlock = promptAssembler.buildContextBlock(assembled.selectedChunks());
        String userContent  = contextBlock + "\nQuestion: " + question;

        Prompt prompt = new Prompt(
                List.of(new SystemMessage(assembled.systemPrompt()), new UserMessage(userContent)),
                OpenAiChatOptions.builder()
                        .temperature(0.1)
                        .maxTokens(4096)
                        .build());

        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();

        var usage = response.getMetadata().getUsage();
        int promptTokens     = usage != null ? (int) usage.getPromptTokens()     : assembled.totalInputTokens();
        int completionTokens = usage != null ? (int) usage.getCompletionTokens() : 0;
        int totalTokens      = usage != null ? (int) usage.getTotalTokens()       : promptTokens + completionTokens;

        log.info("RagService: answer generated — prompt={} completion={} total={} tokens",
                promptTokens, completionTokens, totalTokens);

        List<RagResponse.Citation> citations = assembled.selectedChunks().stream()
                .map(c -> new RagResponse.Citation(c.sourceFile(), c.version(), c.chunkIndex()))
                .toList();

        return new RagResponse(answer, citations, new RagResponse.TokenUsage(promptTokens, completionTokens, totalTokens));
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
