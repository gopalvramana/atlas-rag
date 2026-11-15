package com.atlas.api;

import com.atlas.agent.AtlasAgent;
import com.atlas.core.model.AgentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AgentController {

    private final AtlasAgent atlasAgent;

    public AgentController(AtlasAgent atlasAgent) {
        this.atlasAgent = atlasAgent;
    }

    @PostMapping("/agent")
    public ResponseEntity<AgentResponse> ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AgentResponse response = atlasAgent.ask(request.question());
        return ResponseEntity.ok(response);
    }
}
