// ISOLATED world — runs on https://velog.io/write*
// Selectors verified against a real logged-in account:
//   title field: [placeholder="제목을 입력하세요"] — Velog switched this from
//     <input> to an auto-resizing <textarea> at some point after the original
//     2026-08-10 calibration, so the selector no longer constrains the tag.
//   tags field:  [placeholder="태그를 입력하세요"] (fill one, press Enter, repeat)
//   body editor: CodeMirror 5 — filled via page-inject.js (MAIN world), not here.
(function () {
  const API = 'http://localhost:8091';
  const TARGET = 'VELOG';

  function fillTitleAndTags(title, tagsCsv) {
    const titleField = document.querySelector('[placeholder="제목을 입력하세요"]');
    if (!titleField) {
      throw new Error('제목 입력란을 찾지 못했습니다. Velog 화면 구조가 바뀌었을 수 있습니다.');
    }
    window.__blogAutowriterSetNativeValue(titleField, title);

    if (tagsCsv && tagsCsv.trim()) {
      const tagField = document.querySelector('[placeholder="태그를 입력하세요"]');
      if (tagField) {
        tagsCsv.split(',').map((t) => t.trim()).filter(Boolean).forEach((tag) => {
          window.__blogAutowriterSetNativeValue(tagField, tag);
          tagField.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
        });
      }
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

  // Best-effort: Velog navigates away from /write once a post actually publishes.
  // If this doesn't fire (site changed, or the user backs out), the web app's
  // manual "발행 완료로 표시" button is the fallback — this is never required.
  function watchForPublish(id) {
    const start = Date.now();
    const interval = setInterval(() => {
      if (!location.href.includes('/write')) {
        clearInterval(interval);
        fetch(`${API}/api/extension/${id}/published`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ publishedUrl: location.href }),
        }).catch(() => {});
      } else if (Date.now() - start > 10 * 60 * 1000) {
        clearInterval(interval);
      }
    }, 1500);
  }

  async function handleFill(shadow, pending) {
    window.__blogAutowriterOverlay.setFilling(shadow);
    try {
      fillTitleAndTags(pending.title, pending.tags);
      await fillBody(pending.content);
      await fetch(`${API}/api/extension/${pending.id}/filled`, { method: 'POST' });
      window.__blogAutowriterOverlay.setFilled(shadow);
      watchForPublish(pending.id);
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
