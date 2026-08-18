package com.example.agentcli.application;

import com.example.agentcli.domain.ShortTermMemory;
import com.example.agentcli.exception.AgentException;
import com.example.agentcli.infrastructure.openai.GatewayResponse;
import com.example.agentcli.infrastructure.openai.ResponseGateway;
import java.util.Objects;

public final class ChatAgent implements AgentPort {
    private final ShortTermMemory memory;
    private final ResponseGateway gateway;

    public ChatAgent(ShortTermMemory memory, ResponseGateway gateway) {
        this.memory = Objects.requireNonNull(memory, "memory");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @Override
    public AgentResult respond(String userInput) throws AgentException {
        GatewayResponse response = gateway.respond(memory.snapshot(), userInput);
        if (response.answer() == null || response.answer().isBlank()) {
            throw new AgentException("유효한 답변을 처리하지 못했습니다.");
        }

        memory.add(userInput, response.answer());
        return new AgentResult(response.answer(), response.webSearchUsed());
    }
}
