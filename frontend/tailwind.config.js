/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Brand
        primary: '#4F46E5',
        'primary-hover': '#4338CA',
        'primary-light': '#EEF2FF',
        // Status
        success: '#10B981',
        'success-light': '#ECFDF5',
        warning: '#F59E0B',
        'warning-light': '#FFFBEB',
        danger: '#EF4444',
        'danger-light': '#FEF2F2',
        // Surfaces
        'bg-base': '#FAFAFA',
        'surface': '#FFFFFF',
        'border': '#E5E7EB',
        'border-focus': '#4F46E5',
        // Text
        'text-primary': '#111827',
        'text-secondary': '#6B7280',
        'text-muted': '#9CA3AF',
        // Dark mode surfaces
        'dark-bg': '#0F1115',
        'dark-surface': '#1A1C23',
        'dark-border': '#2D3748',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['Geist Mono', 'ui-monospace', 'monospace'],
        display: ['Geist', 'Inter', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        'xs': ['11px', { lineHeight: '16px' }],
      },
      boxShadow: {
        'card': '0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)',
        'card-hover': '0 4px 12px rgba(0,0,0,0.08), 0 2px 4px rgba(0,0,0,0.04)',
        'modal': '0 20px 60px rgba(0,0,0,0.15)',
      },
      borderRadius: {
        'DEFAULT': '8px',
        'sm': '6px',
        'md': '8px',
        'lg': '12px',
        'xl': '16px',
        'full': '9999px',
      },
      animation: {
        'pulse-slow': 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'fadeIn': 'fadeIn 0.2s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(4px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
}
