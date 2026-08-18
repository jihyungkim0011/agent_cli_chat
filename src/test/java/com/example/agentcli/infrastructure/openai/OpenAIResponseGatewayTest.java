package com.example.agentcli.infrastructure.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.agentcli.domain.ConversationTurn;
import com.example.agentcli.exception.AgentException;
import com.openai.core.JsonValue;
import com.openai.core.http.Headers;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFunctionWebSearch;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenAIResponseGatewayTest {
    @Test
    void buildsOrderedHistoryWithOneAutomaticWebSearchTool() throws AgentException {
        AtomicReference<ResponseCreateParams> captured = new AtomicReference<>();
        OpenAIResponseGateway gateway = new OpenAIResponseGateway(params -> {
            captured.set(params);
            return new GatewayResponse("최종 답변", false);
        });

        GatewayResponse result = gateway.respond(
                List.of(
                        new ConversationTurn("질문 1", "답변 1"),
                        new ConversationTurn("질문 2", "답변 2")),
                "질문 3");

        ResponseCreateParams params = captured.get();
        assertEquals("gpt-5.4-nano", params.model().orElseThrow().asString());
        assertEquals(1, params.tools().orElseThrow().size());
        assertTrue(params.tools().orElseThrow().getFirst().isWebSearch());
        assertTrue(params.toolChoice().isEmpty());
        assertEquals(
                List.of("user", "assistant", "user", "assistant", "user"),
                messages(params).stream().map(message -> message.role().asString()).toList());
        assertEquals(
                List.of("질문 1", "답변 1", "질문 2", "답변 2", "질문 3"),
                messages(params).stream().map(message -> message.content().asTextInput()).toList());
        assertEquals(new GatewayResponse("최종 답변", false), result);
    }

    @Test
    void extractsAnswerAndWebSearchUsageFromRealSdkOutputItems() throws AgentException {
        ResponseOutputText outputText = ResponseOutputText.builder()
                .annotations(List.of())
                .text("검색 기반 답변")
                .build();
        ResponseOutputMessage message = ResponseOutputMessage.builder()
                .id("message-1")
                .addContent(outputText)
                .status(ResponseOutputMessage.Status.COMPLETED)
                .build();
        ResponseFunctionWebSearch webSearch = ResponseFunctionWebSearch.builder()
                .id("search-1")
                .action(ResponseFunctionWebSearch.Action.Search.builder()
                        .addQuery("오늘 소식")
                        .build())
                .status(ResponseFunctionWebSearch.Status.COMPLETED)
                .build();

        GatewayResponse result = OpenAIResponseGateway.toGatewayResponse(
                ResponseStatus.COMPLETED,
                false,
                false,
                List.of(
                ResponseOutputItem.ofWebSearchCall(webSearch),
                ResponseOutputItem.ofMessage(message)));

        assertEquals("검색 기반 답변", result.answer());
        assertTrue(result.webSearchUsed());
    }

    @Test
    void failedResponseDoesNotReturnPartialText() {
        ResponseOutputMessage partialMessage = ResponseOutputMessage.builder()
                .id("message-partial")
                .addContent(ResponseOutputText.builder()
                        .annotations(List.of())
                        .text("저장하면 안 되는 부분 답변")
                        .build())
                .status(ResponseOutputMessage.Status.INCOMPLETE)
                .build();

        AgentException error = assertThrows(
                AgentException.class,
                () -> OpenAIResponseGateway.toGatewayResponse(
                        ResponseStatus.FAILED,
                        true,
                        false,
                        List.of(ResponseOutputItem.ofMessage(partialMessage))));

        assertEquals("OpenAI 요청을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.", error.getMessage());
    }

    @Test
    void failedWebSearchDoesNotReportSuccessfulToolUsage() {
        ResponseFunctionWebSearch failedSearch = ResponseFunctionWebSearch.builder()
                .id("search-failed")
                .action(ResponseFunctionWebSearch.Action.Search.builder()
                        .addQuery("실패할 검색")
                        .build())
                .status(ResponseFunctionWebSearch.Status.FAILED)
                .build();
        ResponseOutputMessage fallbackMessage = ResponseOutputMessage.builder()
                .id("message-fallback")
                .addContent(ResponseOutputText.builder()
                        .annotations(List.of())
                        .text("검색 없이 만든 부분 답변")
                        .build())
                .status(ResponseOutputMessage.Status.COMPLETED)
                .build();

        AgentException error = assertThrows(
                AgentException.class,
                () -> OpenAIResponseGateway.toGatewayResponse(
                        ResponseStatus.COMPLETED,
                        false,
                        false,
                        List.of(
                                ResponseOutputItem.ofWebSearchCall(failedSearch),
                                ResponseOutputItem.ofMessage(fallbackMessage))));

        assertEquals("웹 검색을 완료하지 못했습니다. 다시 시도해 주세요.", error.getMessage());
    }

    @Test
    void convertsNetworkFailureWithoutLeakingOriginalMessage() {
        OpenAIIoException cause = new OpenAIIoException("Authorization: Bearer secret-value");
        OpenAIResponseGateway gateway = new OpenAIResponseGateway(params -> {
            throw cause;
        });

        AgentException error = assertThrows(
                AgentException.class,
                () -> gateway.respond(List.of(), "질문"));

        assertEquals("네트워크 연결 상태를 확인해 주세요.", error.getMessage());
        assertFalse(error.getMessage().contains("secret-value"));
        assertSame(cause, error.getCause());
    }

    @Test
    void mapsServiceStatusCodesToSafeMessages() {
        Map<Integer, String> expectedMessages = Map.of(
                401, "API 키가 유효하지 않거나 모델 접근 권한이 없습니다.",
                403, "API 키가 유효하지 않거나 모델 접근 권한이 없습니다.",
                429, "결제 상태나 API 사용 한도를 확인해 주세요.",
                503, "OpenAI 서비스에 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

        expectedMessages.forEach((statusCode, expectedMessage) -> {
            StatusException cause = new StatusException(statusCode);
            OpenAIResponseGateway gateway = new OpenAIResponseGateway(params -> {
                throw cause;
            });

            AgentException error = assertThrows(
                    AgentException.class,
                    () -> gateway.respond(List.of(), "질문"));

            assertEquals(expectedMessage, error.getMessage());
            assertFalse(error.getMessage().contains("secret-value"));
            assertSame(cause, error.getCause());
        });
    }

    private List<EasyInputMessage> messages(ResponseCreateParams params) {
        return params.input().orElseThrow().asResponse().stream()
                .map(ResponseInputItem::asEasyInputMessage)
                .toList();
    }

    private static final class StatusException extends OpenAIServiceException {
        private final int statusCode;

        private StatusException(int statusCode) {
            super("Authorization: Bearer secret-value", null);
            this.statusCode = statusCode;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public Headers headers() {
            return Headers.builder().build();
        }

        @Override
        public JsonValue body() {
            return JsonValue.from(Map.of());
        }

        @Override
        public Optional<String> code() {
            return Optional.empty();
        }

        @Override
        public Optional<String> param() {
            return Optional.empty();
        }

        @Override
        public Optional<String> type() {
            return Optional.empty();
        }
    }
}
