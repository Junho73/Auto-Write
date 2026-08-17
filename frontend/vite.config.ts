import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Relative base: the built HTML references its own JS/CSS/favicon with a
  // relative path instead of an absolute "/..." one. Absolute paths would
  // resolve against chrome-extension://<id>/ (the origin root) rather than
  // chrome-extension://<id>/webapp/ (where index.html actually lives), so
  // loading it as chrome-extension://<id>/webapp/index.html would 404 on
  // every asset.
  base: './',
  build: {
    // Bundled straight into the Chrome extension (chrome-extension://<id>/webapp/)
    // instead of served from a dev-server port — sidesteps WSL2 NAT localhost
    // forwarding hijacking whatever port we pick (see project history: it silently
    // routed 127.0.0.1:3000/:8080 into a WSL-hosted Docker service).
    outDir: '../extension/webapp',
    emptyOutDir: true,
  },
})
