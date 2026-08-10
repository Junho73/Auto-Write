# Playwright Blog Autowriter

Claude API로 블로그 글을 생성하고, Playwright로 Velog(또는 로컬 모의 블로그)에 자동 발행/예약하는 도구.

## 실행 준비

```bash
export ANTHROPIC_API_KEY=sk-ant-...   # 필수. 없으면 백엔드 기동 실패.
```

```bash
cd backend && mvn spring-boot:run   # http://localhost:8080
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## Velog 연결 (최초 1회)

Velog는 이메일 인증(매직링크/코드) 로그인만 지원해서 아이디/비번 자동입력이 불가능합니다.
대신 대시보드의 "Velog 연결하기" 버튼을 누르면 헤드풀 브라우저 창이 뜨는데, 그 창에서
**직접 로그인**을 완료한 뒤 "로그인 완료, 세션 저장" 버튼을 누르면 세션(쿠키)이
`backend/data/velog-session.json`에 저장됩니다. 이후 예약/즉시 포스팅은 이 세션을 재사용합니다.
세션이 만료되면 자동으로 실패 처리되니(`SESSION_EXPIRED`) 다시 연결해주세요.

## Velog 발행 셀렉터 (2026-08-10 실제 계정으로 검증 완료)

`VelogAutomationService`의 셀렉터는 실제 로그인 세션에서 직접 확인해 반영했습니다:
- 제목/태그는 모달 없이 글쓰기 화면에 바로 있는 `textarea`/`input` (플레이스홀더 "제목을 입력하세요" / "태그를 입력하세요")
- 본문은 CodeMirror 5 에디터라 `fill()`이 안 먹어서, `document.querySelector('.CodeMirror').CodeMirror.setValue(text)`를
  `page.evaluate`로 직접 호출합니다 (Velog의 실시간 미리보기 패널까지 갱신되는 것 확인함)
- "출간하기"를 누르면 같은 페이지에 두 번째 "출간하기" 버튼이 있는 패널이 나타나는데, 이 최종 확인 버튼 뒤에
  **Cloudflare Turnstile 캡차 위젯**이 붙어있습니다. 진짜 쿠키 세션이면 대부분 무상호작용으로 통과되지만,
  Turnstile이 인터랙션을 요구하면(자동화가 의심스러워 보일 때) 자동화는 이를 절대 우회 시도하지 않고
  `/write`에서 URL이 안 바뀐 채로 20초 뒤 명확히 실패 처리됩니다 — 이 경우 스크린샷을 확인하고 필요하면
  직접 로그인 세션을 재연결하거나 수동으로 발행해주세요.
- Velog가 화면을 바꾸면 이 셀렉터들도 깨질 수 있습니다. `target: MOCK`으로는 셀렉터와 무관하게
  전체 파이프라인(생성→예약→발행→이력)을 그대로 테스트할 수 있습니다.

## 스타일 프리셋

콘텐츠는 실제 블로그 글을 스크래핑하지 않고, 회고형/트러블슈팅형/튜토리얼형/기술설명형
4가지 구조·톤 가이드(`StylePresetService`)를 프롬프트에 반영해 생성합니다.

## AI 모델 선택

생성 화면과 예약 등록 화면에서 `claude-haiku-4-5`(빠르고 저렴)와 `claude-sonnet-5`(고품질)
중 하나를 골라 사용할 수 있습니다. `ClaudeService`는 `output_config.format`(구조화된 출력)으로
호출해서 title/tags/content가 항상 유효한 JSON으로 오도록 보장합니다.
