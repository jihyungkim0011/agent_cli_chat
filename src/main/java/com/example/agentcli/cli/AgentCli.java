package com.example.agentcli.cli;

import com.example.agentcli.application.AgentPort;
import com.example.agentcli.application.AgentResult;
import com.example.agentcli.exception.AgentException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

public final class AgentCli {
    private final BufferedReader input;
    private final PrintWriter output;
    private final AgentPort agent;

    public AgentCli(BufferedReader input, PrintWriter output, AgentPort agent) {
        this.input = Objects.requireNonNull(input, "input");
        this.output = Objects.requireNonNull(output, "output");
        this.agent = Objects.requireNonNull(agent, "agent");
    }

    public void run() throws IOException {
        String line;
        while ((line = input.readLine()) != null) {
            String userInput = line.trim();
            if (userInput.equals("/exit")) {
                return;
            }
            if (userInput.isEmpty()) {
                continue;
            }

            try {
                AgentResult result = agent.respond(userInput);
                if (result.webSearchUsed()) {
                    output.println("[도구] 웹 검색을 사용했습니다.");
                }
                output.println("에이전트> " + result.answer());
            } catch (AgentException exception) {
                output.println("[오류] " + exception.getMessage());
            } catch (RuntimeException exception) {
                output.println("[오류] 예상하지 못한 오류가 발생했습니다.");
            }
            output.flush();
        }
    }
}
