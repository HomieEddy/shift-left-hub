# Shift-Left Knowledge Hub - UX/UI Document (UXD)

## 1. The Design Philosophy: Functional Animation
Avoid heavy third-party animation libraries. Leverage **Native Angular Animations** (`@angular/animations`) combined with **Tailwind CSS**. Motion is used strictly to guide the user's eye, explain state transitions, and reduce cognitive load.

## 2. Brand Identity (v2.0)

### Color Palette
- **Primary:** Warm slate/charcoal (`slate-50` through `slate-900`) — neutral, professional, domain-agnostic
- **Accent:** Amber (`amber-600 #d97706`) — warm, approachable, attention-guiding
- **Success:** Emerald tones for resolution/completion states
- **Background:** White/off-white surfaces with slate shadows

### Typography
- **Stack:** System default sans-serif via Tailwind (Inter, SF Pro, Segoe UI)
- **Headings:** Bold, generous tracking for hierarchy
- **Body:** Comfortable line-height for knowledge content readability

### Icons
- **Favicon:** SVG open book in slate-700 on transparent background
- **UI Icons:** Lucide icon set — consistent, clean, recognizable

## 3. Key Interaction Signatures (The "Wow" Moments)

### The Workspace Switcher
- **Tech:** Dropdown component, signal-based state, workspace context reload
- **UX:** Click workspace icon/name in nav → dropdown lists user's workspaces with icons → selection reloads entire UI for new workspace context

### The Search Cascade
- **Tech:** `stagger()`, `query()`, RxJS `debounceTime`
- **UX:** Article and document result cards gracefully slide in from the bottom with a slight fade, staggered by 50ms.

### The Escalation Morph
- **Tech:** Route Transitions, State Morphing
- **UX:** Clicking "No, this didn't help" seamlessly morphs the chat container into the Ticket Escalation form.

### The Document Upload Flow
- **Tech:** Drag-and-drop zone, async status polling, progress indicators
- **UX:** Drop zone highlights on drag-over. Upload progress shows 5-stage pipeline status. Ready state shows document available for search.

### The Agent Dashboard Reordering
- **Tech:** `[@listAnimation]`, `:enter` / `:leave`
- **UX:** Resolving a ticket causes the row to flash green, shrink, and slide out. Remaining tickets glide up to fill the gap.

## 4. Layout Structure

### Navigation Bar
- **Left:** Logo/name + workspace switcher dropdown
- **Center (when signed in):** Knowledge Base, Assistant, Tickets
- **Right:** Language switcher, user menu (profile, settings, logout)

### Dashboard (signed-in landing)
- Unified workspace home with quick-access cards for KB, Assistant, Documents
- Role-appropriate sections revealed based on workspace role (admin/member/read-only)

### Admin Panel
- Tabbed workspace detail: Members, LLM Config, Documents, Settings
- Workspace list with search, icon display, member counts

## 5. Bilingual Layout Constraints (i18n)
French text strings are often 15-20% longer than English.
* Avoid fixed-width containers.
* Use flexible layout strategies (Tailwind utility classes).
* Ensure Angular native animations are calculated dynamically (`height: '*'`) rather than hardcoded pixel values.
* All translations managed via `@angular/localize` XLF files.
