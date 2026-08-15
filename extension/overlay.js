// Shared banner UI for the Blog Autowriter content scripts (ISOLATED world).
// Uses a shadow DOM host so its styles never collide with the host page's CSS.
(function () {
  // Sets a value on a React-controlled <input> or <textarea> via the native
  // property setter (plain `el.value =` doesn't trigger React's change
  // detection), then dispatches a real input event so React picks it up.
  // Shared because both Velog and Tistory use auto-resizing <textarea>
  // fields for what looks like plain single-line inputs.
  window.__blogAutowriterSetNativeValue = function (el, value) {
    const proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
    const setter = Object.getOwnPropertyDescriptor(proto, 'value').set;
    setter.call(el, value);
    el.dispatchEvent(new Event('input', { bubbles: true }));
  };

  function createHost() {
    const host = document.createElement('div');
    host.id = 'blog-autowriter-overlay-host';
    host.style.position = 'fixed';
    host.style.bottom = '20px';
    host.style.right = '20px';
    host.style.zIndex = '2147483647';
    const shadow = host.attachShadow({ mode: 'open' });

    shadow.innerHTML = `
      <style>
        .card {
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
          background: #161e31;
          color: #f3f4f6;
          border: 1px solid rgba(255,255,255,0.12);
          border-radius: 12px;
          padding: 14px 16px;
          width: 300px;
          box-shadow: 0 8px 24px rgba(0,0,0,0.35);
          font-size: 13px;
          line-height: 1.5;
        }
        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
        .title { font-weight: 700; }
        .settings-btn {
          background: transparent; border: none; color: #9ca3af; cursor: pointer;
          font-size: 14px; padding: 2px 4px; line-height: 1;
        }
        .settings-btn:hover { color: #f3f4f6; }
        .desc { color: #9ca3af; margin-bottom: 10px; word-break: break-word; }
        .row { display: flex; gap: 8px; }
        button.primary, button.ghost {
          border: none; border-radius: 8px; padding: 6px 12px; font-size: 12px;
          font-weight: 600; cursor: pointer;
        }
        .primary { background: #6366f1; color: white; }
        .ghost { background: rgba(255,255,255,0.08); color: #f3f4f6; }
        button:disabled { opacity: 0.6; cursor: default; }
      </style>
      <div class="card">
        <div class="header">
          <div class="title">Blog Autowriter</div>
          <button class="settings-btn" id="ba-settings" title="설정 열기">⚙</button>
        </div>
        <div class="desc" id="ba-desc"></div>
        <div class="row" id="ba-actions"></div>
      </div>
    `;
    document.documentElement.appendChild(host);
    shadow.getElementById('ba-settings').addEventListener('click', () => {
      chrome.runtime.sendMessage({ type: 'OPEN_SETTINGS' });
    });
    return shadow;
  }

  function render(shadow, desc, buttons) {
    shadow.getElementById('ba-desc').textContent = desc;
    const actions = shadow.getElementById('ba-actions');
    actions.innerHTML = '';
    buttons.forEach(({ label, kind, onClick, disabled }) => {
      const btn = document.createElement('button');
      btn.textContent = label;
      btn.className = kind === 'primary' ? 'primary' : 'ghost';
      if (disabled) btn.disabled = true;
      btn.addEventListener('click', onClick);
      actions.appendChild(btn);
    });
  }

  function remove(shadow) {
    shadow.host.remove();
  }

  window.__blogAutowriterOverlay = {
    showConfirm(postTitle, onFill, onDismiss) {
      const shadow = createHost();
      render(shadow, `대기 중인 글 "${postTitle}"을 이 페이지에 채울까요?`, [
        { label: '채우기', kind: 'primary', onClick: () => onFill(shadow) },
        { label: '닫기', kind: 'ghost', onClick: () => { remove(shadow); onDismiss && onDismiss(); } },
      ]);
      return shadow;
    },
    setFilling(shadow) {
      render(shadow, '채우는 중...', [{ label: '채우는 중...', kind: 'primary', disabled: true, onClick: () => {} }]);
    },
    setFilled(shadow) {
      render(shadow, '채워짐 — 확인 후 발행 버튼을 직접 눌러주세요.', [
        { label: '닫기', kind: 'ghost', onClick: () => remove(shadow) },
      ]);
    },
    setPublished(shadow) {
      render(shadow, '발행 완료로 자동 기록되었습니다!', [
        { label: '닫기', kind: 'ghost', onClick: () => remove(shadow) },
      ]);
    },
    setError(shadow, message) {
      render(shadow, message, [{ label: '닫기', kind: 'ghost', onClick: () => remove(shadow) }]);
    },
  };
})();
