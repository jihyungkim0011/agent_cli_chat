package com.example.agentcli.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.agentcli.application.AgentPort;
import com.example.agentcli.application.AgentResult;
import com.example.agentcli.exception.AgentException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AgentCliTest {
    @Test
    void continuesAfterAgentFailure() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        AgentPort agent = input -> {
            if (calls.getAndIncrement() == 0) {
                throw new AgentException("일시적 오류");
            }
            return new AgentResult("두 번째 답변", false);
        };

        String output = runCli("첫 질문\n둘째 질문\n/exit\n", agent);

        assertTrue(output.contains("[오류] 일시적 오류"));
        assertTrue(output.contains("에이전트> 두 번째 답변"));
        assertEquals(2, calls.get());
    }

    @Test
    void continuesAfterUnexpectedFailureWithoutLeakingItsMessage() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        AgentPort agent = input -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("Authorization: Bearer secret-value");
            }
            return new AgentResult("복구 후 답변", false);
        };

        String output = runCli("첫 질문\n둘째 질문\n/exit\n", agent);

        assertTrue(output.contains("[오류] 예상하지 못한 오류가 발생했습니다."));
        assertTrue(output.contains("에이전트> 복구 후 답변"));
        assertFalse(output.contains("secret-value"));
        assertEquals(2, calls.get());
    }

    @Test
    void exitAndBlankLinesDoNotCallAgent() throws IOException {
        AtomicInteger calls = new AtomicInteger();

        runCli("\n   \n/exit\n", input -> {
            calls.incrementAndGet();
            return new AgentResult("호출되면 안 됨", false);
        });

        assertEquals(0, calls.get());
    }

    @Test
    void printsToolNoticeWhenWebSearchWasUsed() throws IOException {
        String output = runCli(
                "최신 뉴스\n/exit\n",
                input -> new AgentResult("검색 기반 답변", true));

        assertTrue(output.contains("[도구] 웹 검색을 사용했습니다."));
        assertTrue(output.contains("에이전트> 검색 기반 답변"));
    }

    private String runCli(String input, AgentPort agent) throws IOException {
        StringWriter output = new StringWriter();
        AgentCli cli = new AgentCli(
                new BufferedReader(new StringReader(input)),
                new PrintWriter(output, true),
                agent);

        cli.run();

        return output.toString();
    }
}
