/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'indigo': {
          50: '#f4ecde',
          500: '#c54f2d',
          600: '#c54f2d',
          700: '#98371e',
        },
        'slate': {
          50: 'rgba(255, 251, 244, 0.92)',
          100: 'rgba(24, 38, 63, 0.14)',
          200: 'rgba(24, 38, 63, 0.24)',
          500: '#50607b',
          900: '#18263f',
        }
      }
    },
  },
  plugins: [],
}
