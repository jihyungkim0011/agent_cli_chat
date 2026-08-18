package com.example.agentcli.application;

import com.example.agentcli.exception.AgentException;

@FunctionalInterface
public interface AgentPort {
    AgentResult respond(String userInput) throws AgentException;
}
