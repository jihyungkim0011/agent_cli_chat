package com.example.agentcli.application;

import java.util.Objects;

public record AgentResult(String answer, boolean webSearchUsed) {
    public AgentResult {
        Objects.requireNonNull(answer, "answer");
    }
}
