package com.example.agentcli.config;

import com.example.agentcli.exception.AgentException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class ApiKeyLoader {
    private static final String KEY_NAME = "OPENAI_API_KEY";

    public String load(Path workingDirectory, Map<String, String> environment)
            throws AgentException {
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Objects.requireNonNull(environment, "environment");

        String localValue = loadLocalValue(workingDirectory.resolve(".env.local"));
        if (isPresent(localValue)) {
            return localValue;
        }

        String environmentValue = environment.get(KEY_NAME);
        if (isPresent(environmentValue)) {
            return environmentValue.trim();
        }

        throw new AgentException("OPENAI_API_KEY 설정이 필요합니다.");
    }

    private String loadLocalValue(Path envFile) throws AgentException {
        if (!Files.isRegularFile(envFile)) {
            return null;
        }

        try {
            for (String line : Files.readAllLines(envFile)) {
                String normalized = line.trim();
                if (normalized.startsWith("export ")) {
                    normalized = normalized.substring("export ".length()).trim();
                }
                int separator = normalized.indexOf('=');
                if (separator < 0 || !normalized.substring(0, separator).trim().equals(KEY_NAME)) {
                    continue;
                }
                return removeMatchingQuotes(normalized.substring(separator + 1).trim());
            }
            return null;
        } catch (IOException exception) {
            throw new AgentException(".env.local 파일을 읽지 못했습니다.", exception);
        }
    }

    private String removeMatchingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
