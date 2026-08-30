/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ['class', '[data-theme="dark"]'],
  content: [
    './src/**/*.{js,ts,jsx,tsx,vue}',
    './index.html',
  ],
  theme: {
    extend: {
      colors: {
        canvas: '#09090B',
        surface: {
          DEFAULT: '#18181B',
          elevated: '#202024',
          hover: '#27272A',
          inset: '#0C0C0E',
        },
        border: {
          subtle: '#27272A',
          DEFAULT: '#3F3F46',
          hover: '#52525B',
          strong: '#71717A',
        },
        text: {
          primary: '#FAFAFA',
          secondary: '#A1A1AA',
          muted: '#71717A',
          dimmed: '#52525B',
        },
        brand: {
          50: '#F5F3FF',
          100: '#EDE9FE',
          200: '#DDD6FE',
          300: '#C4B5FD',
          400: '#A78BFA',
          500: '#8B5CF6',
          DEFAULT: '#7C3AED',
          hover: '#6D28D9',
          active: '#5B21B6',
          900: '#4C1D95',
          950: '#2E1065',
        },
        status: {
          success: {
            DEFAULT: '#10B981',
            bg: 'rgba(16, 185, 129, 0.1)',
            border: 'rgba(16, 185, 129, 0.3)',
          },
          running: {
            DEFAULT: '#3B82F6',
            bg: 'rgba(59, 130, 246, 0.12)',
            border: 'rgba(59, 130, 246, 0.35)',
          },
          failed: {
            DEFAULT: '#EF4444',
            bg: 'rgba(239, 68, 68, 0.1)',
            border: 'rgba(239, 68, 68, 0.3)',
          },
          pending: {
            DEFAULT: '#EAB308',
            bg: 'rgba(234, 179, 8, 0.1)',
            border: 'rgba(234, 179, 8, 0.3)',
          },
          queued: {
            DEFAULT: '#A1A1AA',
            bg: 'rgba(161, 161, 170, 0.08)',
            border: 'rgba(161, 161, 170, 0.2)',
          },
          skipped: {
            DEFAULT: '#6B7280',
            bg: 'rgba(107, 114, 128, 0.08)',
            border: 'rgba(107, 114, 128, 0.2)',
          },
          offline: {
            DEFAULT: '#52525B',
            bg: 'rgba(82, 82, 91, 0.1)',
            border: 'rgba(82, 82, 91, 0.25)',
          },
          retry: {
            DEFAULT: '#F97316',
            bg: 'rgba(249, 115, 22, 0.1)',
            border: 'rgba(249, 115, 22, 0.3)',
          },
        },
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace'],
      },
      borderRadius: {
        bento: '16px',
        card: '12px',
        btn: '8px',
      },
      boxShadow: {
        bento: '0 10px 30px -10px rgba(0, 0, 0, 0.6), 0 1px 2px 0 rgba(0, 0, 0, 0.4)',
        'bento-hover': '0 20px 40px -12px rgba(0, 0, 0, 0.8)',
        'glow-brand': '0 0 24px rgba(124, 58, 237, 0.3)',
        'glow-success': '0 0 18px rgba(16, 185, 129, 0.3)',
        'glow-running': '0 0 18px rgba(59, 130, 246, 0.3)',
        'glow-failed': '0 0 18px rgba(239, 68, 68, 0.3)',
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'spin-linear': 'spin 1.5s linear infinite',
      },
    },
  },
  plugins: [],
};
