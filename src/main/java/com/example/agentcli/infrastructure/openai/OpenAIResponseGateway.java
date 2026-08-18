package com.example.agentcli.infrastructure.openai;

import com.example.agentcli.domain.ConversationTurn;
import com.example.agentcli.exception.AgentException;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFunctionWebSearch;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.WebSearchTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class OpenAIResponseGateway implements ResponseGateway {
    private static final String MODEL = "gpt-5.4-nano";

    private final ResponseCaller caller;

    public OpenAIResponseGateway(OpenAIClient client) {
        Objects.requireNonNull(client, "client");
        this.caller = params -> {
            Response response = client.responses().create(params);
            return toGatewayResponse(
                    response.status().orElse(null),
                    response.error().isPresent(),
                    response.incompleteDetails().isPresent(),
                    response.output());
        };
    }

    OpenAIResponseGateway(ResponseCaller caller) {
        this.caller = Objects.requireNonNull(caller, "caller");
    }

    @Override
    public GatewayResponse respond(List<ConversationTurn> history, String userInput)
            throws AgentException {
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(userInput, "userInput");

        try {
            return caller.create(createParams(history, userInput));
        } catch (RuntimeException exception) {
            throw mapException(exception);
        }
    }

    static GatewayResponse toGatewayResponse(
            ResponseStatus status,
            boolean hasError,
            boolean incomplete,
            List<ResponseOutputItem> output) throws AgentException {
        if (hasError || ResponseStatus.FAILED.equals(status)) {
            throw new AgentException("OpenAI 요청을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (incomplete || !ResponseStatus.COMPLETED.equals(status)) {
            throw new AgentException("OpenAI가 최종 답변 생성을 완료하지 못했습니다. 다시 시도해 주세요.");
        }

        List<ResponseFunctionWebSearch> webSearchCalls = output.stream()
                .filter(ResponseOutputItem::isWebSearchCall)
                .map(ResponseOutputItem::asWebSearchCall)
                .toList();
        if (webSearchCalls.stream()
                .anyMatch(call -> !ResponseFunctionWebSearch.Status.COMPLETED.equals(call.status()))) {
            throw new AgentException("웹 검색을 완료하지 못했습니다. 다시 시도해 주세요.");
        }

        String answer = output.stream()
                .filter(ResponseOutputItem::isMessage)
                .map(ResponseOutputItem::asMessage)
                .flatMap(message -> message.content().stream())
                .filter(content -> content.isOutputText())
                .map(content -> content.asOutputText().text())
                .collect(Collectors.joining("\n"))
                .trim();
        if (answer.isEmpty()) {
            throw new AgentException("유효한 답변을 처리하지 못했습니다.");
        }

        boolean webSearchUsed = !webSearchCalls.isEmpty();
        return new GatewayResponse(answer, webSearchUsed);
    }

    private ResponseCreateParams createParams(
            List<ConversationTurn> history,
            String userInput) {
        List<ResponseInputItem> input = new ArrayList<>();
        for (ConversationTurn turn : history) {
            input.add(message(EasyInputMessage.Role.USER, turn.userMessage()));
            input.add(message(EasyInputMessage.Role.ASSISTANT, turn.assistantMessage()));
        }
        input.add(message(EasyInputMessage.Role.USER, userInput));

        return ResponseCreateParams.builder()
                .model(MODEL)
                .inputOfResponse(input)
                .addTool(WebSearchTool.builder()
                        .type(WebSearchTool.Type.WEB_SEARCH)
                        .build())
                .build();
    }

    private ResponseInputItem message(EasyInputMessage.Role role, String text) {
        return ResponseInputItem.ofEasyInputMessage(EasyInputMessage.builder()
                .role(role)
                .content(text)
                .build());
    }

    private AgentException mapException(RuntimeException exception) {
        if (exception instanceof OpenAIServiceException serviceException) {
            int statusCode = serviceException.statusCode();
            if (statusCode == 401 || statusCode == 403) {
                return new AgentException(
                        "API 키가 유효하지 않거나 모델 접근 권한이 없습니다.",
                        exception);
            }
            if (statusCode == 429) {
                return new AgentException("결제 상태나 API 사용 한도를 확인해 주세요.", exception);
            }
            if (statusCode >= 500) {
                return new AgentException(
                        "OpenAI 서비스에 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
                        exception);
            }
        }
        if (exception instanceof OpenAIIoException) {
            return new AgentException("네트워크 연결 상태를 확인해 주세요.", exception);
        }
        if (exception instanceof OpenAIInvalidDataException) {
            return new AgentException("유효한 답변을 처리하지 못했습니다.", exception);
        }
        if (exception instanceof OpenAIException) {
            return new AgentException("OpenAI 요청을 처리하지 못했습니다.", exception);
        }
        return new AgentException("예상하지 못한 오류가 발생했습니다.", exception);
    }

    @FunctionalInterface
    interface ResponseCaller {
        GatewayResponse create(ResponseCreateParams params) throws AgentException;
    }
}
