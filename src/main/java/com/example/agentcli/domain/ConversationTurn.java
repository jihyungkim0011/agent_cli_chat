package com.example.agentcli.domain;

import java.util.Objects;

public record ConversationTurn(String userMessage, String assistantMessage) {
    public ConversationTurn {
        Objects.requireNonNull(userMessage, "userMessage");
        Objects.requireNonNull(assistantMessage, "assistantMessage");
    }
}
