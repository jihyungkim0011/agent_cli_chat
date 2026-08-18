# Java Agent CLI

Java 21 터미널에서 OpenAI Responses API와 연속 대화하는 최소 에이전트입니다. 모델은 `gpt-5.4-nano`를 사용하며, 최신 정보가 필요할 때 모델이 내장 `web_search` 도구를 자동으로 선택합니다.

## 요구 사항

- Java 21
- OpenAI API 키

## API 키 설정

프로젝트 루트의 `.env.local`에 `OPENAI_API_KEY`를 설정합니다. `.env.local`이 없거나 해당 값이 비어 있으면 프로세스 환경의 `OPENAI_API_KEY`를 사용합니다.

`.env.local`은 `.gitignore`에 포함되어 있습니다. 키 값을 소스 코드, 문서, 테스트에 기록하지 마세요.

## 실행

```bash
./gradlew run --console=plain
```

질문을 한 줄씩 입력합니다. 빈 줄은 무시하며 `/exit`를 입력하면 종료합니다. 웹 검색이 실행된 답변 앞에는 다음 문구가 표시됩니다.

```text
[도구] 웹 검색을 사용했습니다.
```

API 또는 네트워크 요청이 실패하면 오류 이유를 정리해 출력하고 다음 입력을 계속 받습니다.

## 테스트

```bash
./gradlew test
```

단위 테스트는 외부 API를 호출하지 않습니다.

## 메모리 정책

성공한 사용자 질문과 최종 답변을 한 쌍으로 묶어 최근 10쌍만 프로세스 메모리에 보관합니다. 실패한 요청은 저장하지 않으며 프로그램을 종료하면 모든 대화가 사라집니다.

## 범위

이 프로젝트는 스트리밍, 장기 저장소, 다중 사용자, 웹 UI, 음성·이미지 입력, 추가 도구를 포함하지 않습니다.

OpenAI 모델 및 API 계약은 [GPT-5.4 nano 모델 문서](https://developers.openai.com/api/docs/models/gpt-5.4-nano), [Responses API 안내](https://developers.openai.com/api/docs/guides/migrate-to-responses), [웹 검색 도구 안내](https://developers.openai.com/api/docs/guides/tools-web-search)를 참고합니다.
