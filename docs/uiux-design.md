# UI/UX DESIGN SYSTEM GUIDELINES

## 1. Design Philosophy: "Structural Minimalism"
- **Core Principle:** Content-first. The UI should be invisible; the data should be the hero.
- **Aesthetic:** Clean, monochromatic, professional, and "Vercel-like".
- **Vibe:** Engineering-focused, precise, trustworthy, and airy.

## 2. Color Palette & Theming
- **Strict Monochromatic Scheme:**
  - Use ONLY the `zinc` or `slate` color palette from Tailwind CSS.
  - **NO** vibrant colors for backgrounds or borders.
  - **Accents:** Use pure black (`zinc-950`) or pure white for primary actions.
  - **Semantic Colors:** Use muted versions of red/green/yellow only for critical states (errors, success), never for decoration.
- **Dark/Light Mode:**
  - Fully support both modes using Tailwind's `dark:` modifier and CSS variables.
  - **Light Mode:** Background pure white (`#ffffff`), Surface `zinc-50`.
  - **Dark Mode:** Background `zinc-950` (`#09090b`), Surface `zinc-900`.
  - **Borders:** Subtle high-contrast borders (`border-zinc-200` light / `border-zinc-800` dark) are preferred over background color shifts.

## 3. Typography (The Hierarchy Driver)
- **Font Family:** Use a clean Sans-serif (Inter, Geist Sans, or SF Pro).
- **Headings:**
  - Use `tracking-tight` (negative letter spacing) for all headings (h1-h3) to create a modern, compact feel.
  - Weight: `font-semibold` or `font-bold`.
- **Body Text:**
  - Use `text-zinc-500` to `text-zinc-600` for secondary text to reduce visual noise.
  - Only use `text-zinc-950` (or white in dark mode) for primary content.
- **Size Scale:** Stick to a strict modular scale. Avoid arbitrary pixel values.

## 4. Components & Layout (Shadcn/ui Style)
- **Radius:** Use `rounded-md` or `rounded-lg`. Avoid fully rounded buttons unless for pill-shapes.
- **Shadows:** Minimal or none. Prefer **1px borders** to define separation. If shadows are needed, use `shadow-sm` or distinct `shadow-lg` for modals only.
- **Spacing (Whitespace):**
  - Use generous padding. "Airy" interfaces feel more premium.
  - Stick to the 4px grid system (p-4, p-6, p-8, gap-4, gap-6).
- **Buttons:**
  - Primary: Solid Black/White (`bg-primary text-primary-foreground`).
  - Secondary: Outline (`border border-input bg-background hover:bg-accent`).
  - Ghost: For low-priority actions (`hover:bg-accent hover:text-accent-foreground`).

## 5. Micro-Interactions
- **Hover States:** Subtle background shifts (e.g., transparent to `bg-zinc-100` dark: `bg-zinc-800`).
- **Transitions:** Always add `transition-all duration-200 ease-in-out` to interactive elements. Avoid jarring instant changes.

## 6. Implementation Rules for AI
- Always use `shadcn/ui` components when available.
- Use `lucide-react` for icons (stroke width 1.5px or 2px max).
- Prioritize **Composition** over Inheritance.
- When generating UI, do not add decorative "fluff" (no blobs, no gradients unless subtle mesh, no heavy drop shadows).