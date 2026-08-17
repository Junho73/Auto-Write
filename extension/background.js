// MV3 service worker. Handles the toolbar icon click and "설정 열기" messages
// from content scripts (content scripts can't call chrome.tabs.create directly).
//
// The dashboard is bundled into the extension itself (extension/webapp/, built by
// `cd frontend && npm run build`) rather than fetched from a dev-server port — no
// more guessing which port Vite landed on, and no exposure to WSL2 NAT localhost
// forwarding silently routing 127.0.0.1:<port> into a WSL-hosted Docker service
// (that's what was actually causing PlayOps Web to open instead of this app).
function webAppUrl(view) {
  const path = view ? `webapp/index.html?view=${view}` : 'webapp/index.html';
  return chrome.runtime.getURL(path);
}

chrome.action.onClicked.addListener(() => {
  chrome.tabs.create({ url: webAppUrl() });
});

chrome.runtime.onMessage.addListener((message) => {
  if (message && message.type === 'OPEN_SETTINGS') {
    chrome.tabs.create({ url: webAppUrl('settings') });
  }
});
