// MV3 service worker. Handles the toolbar icon click and "설정 열기" messages
// from content scripts (content scripts can't call chrome.tabs.create directly).
//
// The dev server's port isn't fixed — Vite falls back to 3001 (or later) if 3000
// is already taken by something else on the machine, which has actually happened
// during development of this extension. Rather than hardcode one port and silently
// open the wrong app, probe both and use whichever responds.
const WEB_APP_PORTS = [3000, 3001];

async function resolveWebAppUrl(path) {
  for (const port of WEB_APP_PORTS) {
    const base = `http://127.0.0.1:${port}`;
    try {
      const res = await fetch(base, { method: 'GET' });
      if (res.ok) return base + path;
    } catch (e) {
      // not listening on this port, try the next one
    }
  }
  // Nothing responded — open the first candidate anyway so the user sees a
  // clear connection error instead of nothing happening.
  return `http://127.0.0.1:${WEB_APP_PORTS[0]}${path}`;
}

chrome.action.onClicked.addListener(async () => {
  const url = await resolveWebAppUrl('/?view=settings');
  chrome.tabs.create({ url });
});

chrome.runtime.onMessage.addListener((message) => {
  if (message && message.type === 'OPEN_SETTINGS') {
    resolveWebAppUrl('/?view=settings').then((url) => chrome.tabs.create({ url }));
  }
});
