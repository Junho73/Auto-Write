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

## ⚠️ 실제 Velog 발행 전 셀렉터 보정 필요

`VelogAutomationService`의 글쓰기 화면 셀렉터(`// TODO calibrate` 표시된 부분)는 실제
로그인 세션 없이는 확인할 수 없어 추측으로 작성돼 있습니다. 실제 계정으로 연결한 뒤,

```bash
npx playwright codegen velog.io
```

또는 브라우저 devtools로 실제 글쓰기/발행 모달의 셀렉터를 확인해서 코드에 반영해주세요.
`target: MOCK`으로는 셀렉터 보정 없이 전체 파이프라인(생성→예약→발행→이력)을 그대로 테스트할 수 있습니다.

## 스타일 프리셋

콘텐츠는 실제 블로그 글을 스크래핑하지 않고, 회고형/트러블슈팅형/튜토리얼형/기술설명형
4가지 구조·톤 가이드(`StylePresetService`)를 프롬프트에 반영해 생성합니다.

## AI 모델 선택

생성 화면과 예약 등록 화면에서 `claude-haiku-4-5`(빠르고 저렴)와 `claude-sonnet-5`(고품질)
중 하나를 골라 사용할 수 있습니다. `ClaudeService`는 `output_config.format`(구조화된 출력)으로
호출해서 title/tags/content가 항상 유효한 JSON으로 오도록 보장합니다.
