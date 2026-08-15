// ISOLATED world — runs on https://*.tistory.com/manage/*
// UNVERIFIED selectors — Tistory's real markdown-mode DOM hasn't been checked live yet
// (unlike Velog, calibrated 2026-08-10 against a real account). These are best-guess
// candidates; if none match, the banner shows a clear error instead of silently failing,
// and the failure is reported to the backend so it shows up in the web app's history too.
// TODO calibrate against a real Tistory 새 글 작성 (마크다운 모드) page and replace/extend
// the candidate lists below.
(function () {
  const API = 'http://localhost:8091';
  const TARGET = 'TISTORY';

  const TITLE_SELECTORS = [
    '#post-title-inp',
    'input[placeholder*="제목"]',
    'textarea[placeholder*="제목"]',
  ];
  const TAG_SELECTORS = [
    '#tagText',
    'input[placeholder*="태그"]',
  ];

  function findFirst(selectors) {
    for (const sel of selectors) {
      const el = document.querySelector(sel);
      if (el) return el;
    }
    return null;
  }

  function setNativeValue(el, value) {
    const proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
    const setter = Object.getOwnPropertyDescriptor(proto, 'value').set;
    setter.call(el, value);
    el.dispatchEvent(new Event('input', { bubbles: true }));
  }

  function fillTitleAndTags(title, tagsCsv) {
    const titleEl = findFirst(TITLE_SELECTORS);
    if (!titleEl) {
      throw new Error('제목 입력란을 찾지 못했습니다 — 티스토리 셀렉터 캘리브레이션이 필요합니다.');
    }
    setNativeValue(titleEl, title);

    if (tagsCsv && tagsCsv.trim()) {
      const tagEl = findFirst(TAG_SELECTORS);
      if (tagEl) {
        tagsCsv.split(',').map((t) => t.trim()).filter(Boolean).forEach((tag) => {
          setNativeValue(tagEl, tag);
          tagEl.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
        });
      }
      // 태그 입력란을 못 찾아도 제목/본문은 이미 채워지므로 치명적 오류로 취급하지 않음.
    }
  }

  function fillBody(content) {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('본문 채우기 응답 시간 초과')), 5000);
      function onMessage(event) {
        if (event.source !== window || !event.data || event.data.source !== 'blog-autowriter-response') return;
        if (event.data.type !== 'FILL_BODY_DONE') return;
        clearTimeout(timeout);
        window.removeEventListener('message', onMessage);
        event.data.ok ? resolve() : reject(new Error(event.data.error || '본문 채우기 실패'));
      }
      window.addEventListener('message', onMessage);
      window.postMessage({ source: 'blog-autowriter', type: 'FILL_BODY', text: content }, '*');
    });
  }

  async function handleFill(shadow, pending) {
    window.__blogAutowriterOverlay.setFilling(shadow);
    try {
      fillTitleAndTags(pending.title, pending.tags);
      await fillBody(pending.content);
      await fetch(`${API}/api/extension/${pending.id}/filled`, { method: 'POST' });
      window.__blogAutowriterOverlay.setFilled(shadow);
      // No auto publish-detection for Tistory yet (URL pattern after publish unconfirmed) —
      // the user marks it published from the web app's history list instead.
    } catch (err) {
      window.__blogAutowriterOverlay.setError(shadow, err.message || '채우기 중 오류가 발생했습니다.');
      fetch(`${API}/api/extension/${pending.id}/failed`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: err.message }),
      }).catch(() => {});
    }
  }

  fetch(`${API}/api/extension/pending?target=${TARGET}`)
    .then((res) => (res.status === 204 ? null : res.json()))
    .then((pending) => {
      if (!pending) return;
      window.__blogAutowriterOverlay.showConfirm(pending.title, (shadow) => handleFill(shadow, pending));
    })
    .catch(() => {}); // backend not running — silently skip, this is an optional assist
})();
