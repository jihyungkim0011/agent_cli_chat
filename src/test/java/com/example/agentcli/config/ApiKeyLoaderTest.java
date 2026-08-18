package com.example.agentcli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.agentcli.exception.AgentException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiKeyLoaderTest {
    @Test
    void localEnvFileTakesPrecedenceOverProcessEnvironment(@TempDir Path tempDir)
            throws IOException, AgentException {
        Files.writeString(tempDir.resolve(".env.local"), "OPENAI_API_KEY=local-key\n");

        String key = new ApiKeyLoader().load(
                tempDir,
                Map.of("OPENAI_API_KEY", "environment-key"));

        assertEquals("local-key", key);
    }

    @Test
    void removesMatchingQuotesFromLocalValue(@TempDir Path tempDir)
            throws IOException, AgentException {
        Files.writeString(tempDir.resolve(".env.local"), "OPENAI_API_KEY=\"quoted-key\"\n");

        assertEquals("quoted-key", new ApiKeyLoader().load(tempDir, Map.of()));
    }

    @Test
    void usesProcessEnvironmentWhenLocalKeyIsAbsent(@TempDir Path tempDir)
            throws AgentException {
        assertEquals(
                "environment-key",
                new ApiKeyLoader().load(
                        tempDir,
                        Map.of("OPENAI_API_KEY", "environment-key")));
    }

    @Test
    void missingKeyHasSafeMessage(@TempDir Path tempDir) {
        AgentException error = assertThrows(
                AgentException.class,
                () -> new ApiKeyLoader().load(tempDir, Map.of()));

        assertEquals("OPENAI_API_KEY 설정이 필요합니다.", error.getMessage());
    }
}
