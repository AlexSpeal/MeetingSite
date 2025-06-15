import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    define: {
        global: 'globalThis'
    },
    plugins: [react()],
    server: {
        port: 3000,
        proxy: {
            '/secured': {
                target: 'http://localhost:8189',
                changeOrigin: true,
                secure: false,
            },
        },
    },
});
