// MV3 service worker. Handles the toolbar icon click and "설정 열기" messages
// from content scripts (content scripts can't call chrome.tabs.create directly).
//
// The dev server's port isn't fixed — Vite falls back to 3001 (or later) if 3000
// is already taken by something else on the machine, which has actually happened
// during development of this extension (a different local app, "PlayOps Web", was
// squatting 3000). A plain "does anything respond on this port" probe isn't enough —
// it opened PlayOps Web instead of us. So the probe checks the response body for a
// marker (<meta name="app-id" content="blog-autowriter">, see frontend/index.html)
// that only our app's HTML actually has.
const WEB_APP_PORTS = [3000, 3001];
const APP_MARKER = 'name="app-id" content="blog-autowriter"';

async function resolveWebAppUrl(path) {
  for (const port of WEB_APP_PORTS) {
    const base = `http://127.0.0.1:${port}`;
    try {
      const res = await fetch(base, { method: 'GET' });
      if (res.ok) {
        const html = await res.text();
        if (html.includes(APP_MARKER)) return base + path;
      }
    } catch (e) {
      // not listening on this port, try the next one
    }
  }
  // Nothing matched — open the first candidate anyway so the user sees a clear
  // connection error instead of silently landing on some unrelated app.
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
