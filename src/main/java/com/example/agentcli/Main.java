package com.example.agentcli;

import com.example.agentcli.application.ChatAgent;
import com.example.agentcli.cli.AgentCli;
import com.example.agentcli.config.ApiKeyLoader;
import com.example.agentcli.domain.ShortTermMemory;
import com.example.agentcli.exception.AgentException;
import com.example.agentcli.infrastructure.openai.OpenAIResponseGateway;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        int exitCode = run(
                Path.of("").toAbsolutePath(),
                System.getenv(),
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                new PrintWriter(
                        new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                        true));
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(
            Path workingDirectory,
            Map<String, String> environment,
            BufferedReader input,
            PrintWriter output) {
        try {
            String apiKey = new ApiKeyLoader().load(workingDirectory, environment);
            OpenAIClient client = OpenAIOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();
            ChatAgent agent = new ChatAgent(
                    new ShortTermMemory(10),
                    new OpenAIResponseGateway(client));
            new AgentCli(input, output, agent).run();
            return 0;
        } catch (AgentException exception) {
            output.println("[오류] " + exception.getMessage());
            return 1;
        } catch (IOException exception) {
            output.println("[오류] 터미널 입력을 읽지 못했습니다.");
            return 1;
        }
    }
}
