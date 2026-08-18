package com.example.agentcli.infrastructure.openai;

public record GatewayResponse(String answer, boolean webSearchUsed) {}
