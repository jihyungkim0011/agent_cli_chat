package com.example.agentcli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.agentcli.domain.ConversationTurn;
import com.example.agentcli.domain.ShortTermMemory;
import com.example.agentcli.exception.AgentException;
import com.example.agentcli.infrastructure.openai.GatewayResponse;
import com.example.agentcli.infrastructure.openai.ResponseGateway;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChatAgentTest {
    @Test
    void passesHistoryAndStoresSuccessfulTurn() throws AgentException {
        ShortTermMemory memory = new ShortTermMemory(10);
        memory.add("이전 질문", "이전 답변");
        AtomicReference<List<ConversationTurn>> capturedHistory = new AtomicReference<>();
        AtomicReference<String> capturedInput = new AtomicReference<>();
        ResponseGateway gateway = (history, input) -> {
            capturedHistory.set(history);
            capturedInput.set(input);
            return new GatewayResponse("새 답변", true);
        };

        AgentResult result = new ChatAgent(memory, gateway).respond("새 질문");

        assertEquals(
                List.of(new ConversationTurn("이전 질문", "이전 답변")),
                capturedHistory.get());
        assertEquals("새 질문", capturedInput.get());
        assertEquals(new AgentResult("새 답변", true), result);
        assertEquals(List.of(
                new ConversationTurn("이전 질문", "이전 답변"),
                new ConversationTurn("새 질문", "새 답변")), memory.snapshot());
    }

    @Test
    void doesNotStoreFailedTurn() {
        ShortTermMemory memory = new ShortTermMemory(10);
        ResponseGateway gateway = (history, input) -> {
            throw new AgentException("API 실패");
        };

        assertThrows(
                AgentException.class,
                () -> new ChatAgent(memory, gateway).respond("실패 질문"));

        assertTrue(memory.snapshot().isEmpty());
    }

    @Test
    void rejectsBlankAnswerWithoutStoringTurn() {
        ShortTermMemory memory = new ShortTermMemory(10);
        ResponseGateway gateway = (history, input) -> new GatewayResponse("   ", false);

        AgentException exception = assertThrows(
                AgentException.class,
                () -> new ChatAgent(memory, gateway).respond("질문"));

        assertEquals("유효한 답변을 처리하지 못했습니다.", exception.getMessage());
        assertTrue(memory.snapshot().isEmpty());
    }
}
