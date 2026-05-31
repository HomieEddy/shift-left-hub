# Shift-Left Knowledge Hub - UX/UI Document (UXD)

## 1. The Design Philosophy: Functional Animation
Avoid heavy third-party animation libraries. Leverage **Native Angular Animations** (`@angular/animations`) combined with **Tailwind CSS**. Motion will be used strictly to guide the user's eye, explain state transitions, and reduce cognitive load.

## 2. Key Interaction Signatures (The "Wow" Moments)

### The "Shift-Left" Search Cascade
* **Tech:** `stagger()`, `query()`, RxJS `debounceTime`
* **UX:** Article cards gracefully slide in from the bottom with a slight fade, staggered by 50ms, rather than snapping instantly onto the screen.

### The Escalation Morph
* **Tech:** Route Transitions, State Morphing
* **UX:** Clicking "No, this didn't help" seamlessly morphs the article container into the Ticket Escalation form, keeping the user grounded in the same space.

### The Agent Dashboard Reordering
* **Tech:** `[@listAnimation]`, `:enter` / `:leave`
* **UX:** Resolving a ticket causes the row to flash green, shrink, and slide out. Remaining tickets glide up to fill the gap.

### The AI Assistant Expansion
* **Tech:** `state('* => void')`, `height: auto`
* **UX:** Chat interface expands smoothly from an empty state. A CSS typing indicator pulses while the Spring Boot backend streams the LLM response.

## 3. Color & Typography Strategy
* **Primary Palette:** Deep Slates and Corporate Blues (`slate-800`, `blue-600`) for enterprise trust.
* **Accent Palette:** Soft Emeralds (`emerald-500`) for resolution/success, Ambers (`amber-500`) for pending KCS drafts.
* **Typography:** System default sans-serif stack via Tailwind.

## 4. Bilingual Layout Constraints (i18n)
French text strings are often 15-20% longer than English.
* Avoid fixed-width containers.
* Use flexible layout strategies (Tailwind utility classes).
* Ensure Angular native animations are calculated dynamically (`height: '*'`) rather than hardcoded pixel values.