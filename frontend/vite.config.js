import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiPrefix = env.VITE_API_PREFIX || env.VITE_API_BASENET_URL || '/api/backAll'
  const proxyTarget = env.VITE_API_PROXY_TARGET || env.VITE_API_BASE_URL || 'http://localhost:9000'

  return {
    plugins: [
      vue(),
      vueDevTools(),
      AutoImport({
        imports: [
          'vue',
          'vue-router',
          {
            pinia: ['defineStore', 'storeToRefs'],
          },
        ],
        dts: 'src/auto-imports.d.ts',
        vueTemplate: true,
        eslintrc: {
          enabled: false,
        },
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      open: false,
      proxy: {
        [apiPrefix]: {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(new RegExp(`^${apiPrefix}`), ''),
          secure: false,
        },
      },
    },
  }
})
