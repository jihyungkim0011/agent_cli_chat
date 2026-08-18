package com.example.agentcli.domain;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class ShortTermMemory {
    private final int capacity;
    private final Deque<ConversationTurn> turns = new ArrayDeque<>();

    public ShortTermMemory(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public void add(String userMessage, String assistantMessage) {
        turns.addLast(new ConversationTurn(userMessage, assistantMessage));
        if (turns.size() > capacity) {
            turns.removeFirst();
        }
    }

    public List<ConversationTurn> snapshot() {
        return List.copyOf(turns);
    }
}
