package com.example.agentcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    @Test
    void stopsStartupWithSafeMessageWhenKeyIsMissing(@TempDir Path tempDir) {
        StringWriter output = new StringWriter();

        int exitCode = Main.run(
                tempDir,
                Map.of(),
                new BufferedReader(new StringReader("/exit\n")),
                new PrintWriter(output, true));

        assertEquals(1, exitCode);
        assertTrue(output.toString().contains("[오류] OPENAI_API_KEY 설정이 필요합니다."));
    }

    @Test
    void exitsWithoutApiCallWhenExitIsFirstInput(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve(".env.local"), "OPENAI_API_KEY=test-key\n");
        StringWriter output = new StringWriter();

        int exitCode = Main.run(
                tempDir,
                Map.of(),
                new BufferedReader(new StringReader("/exit\n")),
                new PrintWriter(output, true));

        assertEquals(0, exitCode);
    }
}
