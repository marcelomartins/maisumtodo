import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import ui from '@nuxt/ui/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    ui({
      ui: {
        colors: {
          primary: 'blue',
          secondary: 'green',
          neutral: 'zinc'
        },
        tabs: {
          slots: {
            list: 'relative flex w-full overflow-x-auto overflow-y-hidden p-1 group whitespace-nowrap',
            trigger: 'group relative inline-flex items-center shrink-0 data-[state=inactive]:text-muted hover:data-[state=inactive]:not-disabled:text-default font-medium rounded-md disabled:cursor-not-allowed disabled:opacity-75 transition-colors',
            label: 'whitespace-nowrap overflow-visible text-clip'
          }
        },
        button: {
          slots: {
            base: 'cursor-pointer disabled:cursor-not-allowed aria-disabled:cursor-not-allowed'
          }
        }
      }
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  base: './',
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: '../src/main/resources/META-INF/resources',
    assetsDir: './',
    emptyOutDir: true, // also necessary
  },
})
