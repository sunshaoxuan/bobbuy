import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 1400,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            return 'vendor';
          }
          if (id.includes('/src/components/ChatWidget')) {
            return 'chat-flow';
          }
          if (id.includes('/src/components/ProcurementDashboard')) {
            return 'procurement-dashboard';
          }
          if (id.includes('/src/pages/StockMaster')) {
            return 'stock-master';
          }
          if (id.includes('/src/pages/ZenAuditView')) {
            return 'audit-view';
          }
          return undefined;
        }
      }
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        headers: {
          'Origin': 'http://localhost:8080',
          'Referer': 'http://localhost:8080/'
        }
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
});
