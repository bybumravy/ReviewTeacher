import { defineConfig, mergeConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default mergeConfig(
  defineConfig({ plugins: [react()] }),
  defineConfig({
    test: {
      environment: 'jsdom',
      setupFiles: './src/setupTests.js',
      globals: true,
    },
  })
);
