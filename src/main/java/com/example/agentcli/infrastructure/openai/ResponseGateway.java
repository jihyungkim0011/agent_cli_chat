package com.example.agentcli.infrastructure.openai;

import com.example.agentcli.domain.ConversationTurn;
import com.example.agentcli.exception.AgentException;
import java.util.List;

@FunctionalInterface
public interface ResponseGateway {
    GatewayResponse respond(List<ConversationTurn> history, String userInput) throws AgentException;
}
