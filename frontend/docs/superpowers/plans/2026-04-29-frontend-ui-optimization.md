# Frontend UI/UX Optimization (Tailwind CSS) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the Vue 3 frontend styling from raw CSS to Tailwind CSS and revamp the UI for `App.vue` and `HomeView.vue`.

**Architecture:** We will install Tailwind CSS, configure PostCSS, replace global styles, and systematically refactor components starting from the root layout (`App.vue`) down to the core view (`HomeView.vue`).

**Tech Stack:** Vue 3, Vite, Tailwind CSS v3, PostCSS, Autoprefixer.

---

### Task 1: Install and Configure Tailwind CSS

**Files:**
- Modify: `package.json`
- Create: `tailwind.config.js`
- Create: `postcss.config.js`
- Modify: `src/styles/tokens.css` (or `base.css`)

- [ ] **Step 1: Install dependencies**
Run: `npm install -D tailwindcss postcss autoprefixer`

- [ ] **Step 2: Initialize Tailwind config**
Run: `npx tailwindcss init -p`
Expected: Creates `tailwind.config.js` and `postcss.config.js`.

- [ ] **Step 3: Configure template paths in `tailwind.config.js`**
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

- [ ] **Step 4: Update global CSS**
Modify `src/styles/base.css` to include Tailwind directives, removing old raw styles.
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 5: Verify build works**
Run: `npm run build`
Expected: Passes without errors.

- [ ] **Step 6: Commit**
```bash
git add package.json package-lock.json tailwind.config.js postcss.config.js src/styles/base.css
git commit -m "chore: setup tailwind css"
```

---

### Task 2: Refactor Global Layout (`App.vue`)

**Files:**
- Modify: `src/App.vue`

- [ ] **Step 1: Refactor Template & Styles**
Replace raw CSS classes with Tailwind utility classes in `src/App.vue`. Implement a modern navbar (e.g., sticky top, glassmorphism `bg-white/80 backdrop-blur-md`, flexbox for alignment).

- [ ] **Step 2: Verify Layout**
Run: `npm run dev` and visually inspect the navbar and main container structure. Check that the router-view is rendering correctly inside a max-width container (e.g., `max-w-7xl mx-auto`).

- [ ] **Step 3: Commit**
```bash
git add src/App.vue
git commit -m "style: refactor App.vue layout with tailwind css"
```

---

### Task 3: Refactor Home View (`HomeView.vue`)

**Files:**
- Modify: `src/views/HomeView.vue`

- [ ] **Step 1: Refactor Hero Section**
Update the top section to be a prominent hero area (large text `text-4xl md:text-6xl font-bold`, clear call to actions using Tailwind buttons `bg-indigo-600 text-white rounded-lg px-6 py-3`).

- [ ] **Step 2: Refactor Grid Layout**
Transform the feature lists (Communities, Jobs, etc.) into responsive grids (e.g., `grid grid-cols-1 md:grid-cols-3 gap-6`). Style the cards with Tailwind (`bg-white rounded-xl shadow-sm border border-slate-100 p-6`).

- [ ] **Step 3: Verify View**
Run: `npm run test` to ensure existing component tests pass (if they check for specific text/components, we might need to adjust selectors, but focus on the visual CSS first). Ensure `npm run build` succeeds.

- [ ] **Step 4: Commit**
```bash
git add src/views/HomeView.vue
git commit -m "style: overhaul HomeView visual design with tailwind css"
```