import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 650,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('/react/') || id.includes('/react-dom/') || id.includes('/react-router-dom/') || id.includes('/scheduler/')) {
              return 'framework';
            }
            if (id.includes('@ant-design/icons') || id.includes('@ant-design/icons-svg')) {
              return 'antd-icons';
            }
            if (
              id.includes('/antd/es/form') ||
              id.includes('/antd/es/input') ||
              id.includes('/antd/es/input-number') ||
              id.includes('/antd/es/select') ||
              id.includes('/antd/es/date-picker') ||
              id.includes('/antd/es/upload') ||
              id.includes('/antd/es/checkbox') ||
              id.includes('/antd/es/radio')
            ) {
              return 'antd-forms';
            }
            if (
              id.includes('/antd/es/table') ||
              id.includes('/antd/es/list') ||
              id.includes('/antd/es/card') ||
              id.includes('/antd/es/tabs') ||
              id.includes('/antd/es/collapse') ||
              id.includes('/antd/es/timeline') ||
              id.includes('/antd/es/progress') ||
              id.includes('/antd/es/result') ||
              id.includes('/antd/es/statistic') ||
              id.includes('/antd/es/badge') ||
              id.includes('/antd/es/tag') ||
              id.includes('/antd/es/breadcrumb') ||
              id.includes('/antd/es/drawer') ||
              id.includes('/antd/es/avatar') ||
              id.includes('/antd/es/empty')
            ) {
              return 'antd-display';
            }
            if (
              id.includes('/antd/es/modal') ||
              id.includes('/antd/es/message') ||
              id.includes('/antd/es/notification') ||
              id.includes('/antd/es/spin') ||
              id.includes('/antd/es/alert')
            ) {
              return 'antd-feedback';
            }
            if (id.includes('/antd/es/')) {
              return 'antd-core';
            }
            if (id.includes('/rc-') || id.includes('/@babel/runtime/') || id.includes('/@ctrl/')) {
              return 'antd-ecosystem';
            }
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
      }
    }
  }
});
