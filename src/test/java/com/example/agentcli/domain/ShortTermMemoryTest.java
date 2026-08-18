package com.example.agentcli.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ShortTermMemoryTest {
    @Test
    void returnsTurnsInConversationOrder() {
        ShortTermMemory memory = new ShortTermMemory(10);
        memory.add("질문 1", "답변 1");
        memory.add("질문 2", "답변 2");

        assertEquals(List.of(
                new ConversationTurn("질문 1", "답변 1"),
                new ConversationTurn("질문 2", "답변 2")), memory.snapshot());
    }

    @Test
    void removesOldestTurnWhenCapacityIsExceeded() {
        ShortTermMemory memory = new ShortTermMemory(10);
        IntStream.rangeClosed(1, 11).forEach(i -> memory.add("질문 " + i, "답변 " + i));

        assertEquals(10, memory.snapshot().size());
        assertEquals("질문 2", memory.snapshot().getFirst().userMessage());
        assertEquals("질문 11", memory.snapshot().getLast().userMessage());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ShortTermMemory(0));
    }
}
