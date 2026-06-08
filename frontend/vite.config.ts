import vue from '@vitejs/plugin-vue';
import { fileURLToPath } from 'node:url';
import type {} from 'vite-ssg';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  base: '/static/',
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  ssgOptions: {
    script: 'async',
    formatting: 'minify',
    includedRoutes: (paths) => paths.filter((path) => path === '/'),
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
      '/health': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'happy-dom',
    environmentOptions: {
      happyDOM: {
        url: 'http://localhost:3000/',
      },
    },
    env: {
      SSR: '',
    },
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    setupFiles: ['src/test/setup.ts'],
  },
});
