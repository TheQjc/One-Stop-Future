# Frontend UI/UX Optimization Design Spec (Tailwind CSS)

## 1. Overview
The goal of this project is to modernize and upgrade the UI/UX of the "One-Stop Future" campus platform frontend. The project currently uses Vue 3 with raw CSS. We will migrate the styling architecture to Tailwind CSS to achieve a modern, clean, and highly customizable aesthetic, specifically targeting a C-end user experience.

## 2. Aesthetic & Visual Guidelines
- **Framework**: Tailwind CSS v3
- **Style**: Modern, minimalist, and clean (Glassmorphism touches where appropriate).
- **Color Palette**:
  - Primary: Indigo/Blue (`indigo-600`, `blue-500`) for actions and emphasis.
  - Neutral/Text: Slate/Gray (`slate-900` for headings, `slate-500` for secondary text, `slate-50` for backgrounds).
- **Typography & Spacing**:
  - Increased whitespace for a breathable layout.
  - Rounded corners (`rounded-xl` or `rounded-2xl` for cards, `rounded-lg` for buttons).
  - Soft shadows (`shadow-sm`, `shadow-md`) to establish depth and hierarchy.

## 3. Technical Architecture
- **Dependencies**: Install `tailwindcss`, `postcss`, and `autoprefixer`.
- **Configuration**: Initialize `tailwind.config.js` and `postcss.config.js`.
- **Global Styles**: Replace contents of `src/styles/tokens.css` and `src/styles/base.css` with Tailwind directives (`@tailwind base; @tailwind components; @tailwind utilities;`).

## 4. Implementation Phases

### Phase 1: Global Infrastructure & Layout
- Install and configure Tailwind CSS.
- Update global CSS files.
- Refactor `App.vue`: Implement a modern responsive navigation bar (header) and global layout container.

### Phase 2: Core View Overhaul (HomeView)
- Refactor `src/views/HomeView.vue`.
- Create a modern "Hero" section with a strong call-to-action.
- Implement a responsive grid layout for feature cards (e.g., Communities, Jobs, Resources).

### Phase 3: Component Standardization
- Standardize common UI elements across the application using Tailwind utility classes or `@apply` (if necessary, though utility-first is preferred).
- Focus on Buttons, Cards, and Form Inputs.

## 5. Success Criteria
- Tailwind CSS is successfully integrated and building correctly via Vite.
- `HomeView.vue` and `App.vue` are fully refactored and visually upgraded.
- No raw CSS layouts remain in the refactored components.
- The application remains fully functional and responsive on both desktop and mobile views.