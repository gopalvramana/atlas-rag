package com.atlas.agent.tools;

import com.atlas.core.model.SearchResult;
import com.atlas.retrieval.search.RetrievalPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exposes RetrievalPipeline as an LLM-callable tool.
 *
 * Spring AI reads the @Tool annotation and generates a JSON Schema tool
 * definition (name + description + parameter schema) that is sent to the
 * model alongside the user's question. The model decides when to call this
 * tool and what query to pass. Spring AI executes the method, feeds the
 * result back to the model, and continues the loop.
 */
@Component
public class SearchDocsTool {

    private static final Logger log = LoggerFactory.getLogger(SearchDocsTool.class);

    private final RetrievalPipeline retrievalPipeline;

    public SearchDocsTool(RetrievalPipeline retrievalPipeline) {
        this.retrievalPipeline = retrievalPipeline;
    }

    @Tool(description = """
            Search the Spring AI documentation for relevant content.
            Use this tool when the question is about Spring AI concepts,
            configuration, APIs, or code examples.
            Returns the most relevant documentation chunks for the given query.
            """)
    public String searchDocs(
            @ToolParam(description = "The search query — rephrase the user question as a focused search query") String query) {

        log.info("SearchDocsTool: query='{}'", query);

        List<SearchResult> chunks = retrievalPipeline.retrieve(query).chunks();

        if (chunks.isEmpty()) {
            log.warn("SearchDocsTool: no chunks found for query='{}'", query);
            return "No relevant documentation found for: " + query;
        }

        log.info("SearchDocsTool: returning {} chunks", chunks.size());
        return chunks.stream()
                .map(c -> "[%s @ %s, chunk %d]\n%s".formatted(
                        c.sourceFile(), c.version(), c.chunkIndex(), c.content()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
