// MAIN world — runs in the page's own JS context so it can reach page-defined
// globals (like a CodeMirror editor instance) that an ISOLATED-world content
// script can't touch directly. Talks to the content script via postMessage.
(function () {
  function setCodeMirrorValue(text) {
    const cm = document.querySelector('.CodeMirror');
    if (cm && cm.CodeMirror) {
      cm.CodeMirror.setValue(text);
      return true;
    }
    return false;
  }

  function setPlainTextarea(text) {
    const textarea = document.querySelector('textarea');
    if (!textarea) return false;
    const setter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value').set;
    setter.call(textarea, text);
    textarea.dispatchEvent(new Event('input', { bubbles: true }));
    return true;
  }

  window.addEventListener('message', (event) => {
    if (event.source !== window || !event.data || event.data.source !== 'blog-autowriter') return;
    if (event.data.type !== 'FILL_BODY') return;

    let ok = false;
    let error = null;
    try {
      ok = setCodeMirrorValue(event.data.text) || setPlainTextarea(event.data.text);
      if (!ok) error = '본문 에디터를 찾지 못했습니다 (CodeMirror도, textarea도 없음) — 셀렉터 캘리브레이션이 필요합니다.';
    } catch (e) {
      error = e.message;
    }

    window.postMessage({ source: 'blog-autowriter-response', type: 'FILL_BODY_DONE', ok, error }, '*');
  });
})();
