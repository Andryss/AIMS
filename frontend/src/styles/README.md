# AIMS UI styles

Mini design system for the AIMS frontend: CSS custom properties, primitives, status chips, and layout-specific rules. No external UI library.

## File layout

| File | Purpose |
|------|---------|
| `tokens.css` | Design tokens: colors (incl. status), spacing, typography, radii, shadows |
| `primitives.css` | Reusable UI: buttons, cards, sections, forms, loading, empty states |
| `status.css` | Incident and cleanup status selects/chips |
| `layout.css` | App shell, header, login, profile, mobile incident cards |
| `overlays.css` | Drawers, modals, notifications, file upload |
| `data-display.css` | Tables, tabs, comments, history, pickers, chips |

Import order in `index.tsx`: `tokens` → `primitives` → `status` → `layout` → `overlays` → `data-display`.

## Dev styleguide

In development, open `/dev/ui` for a live catalog of buttons, status chips, forms, and states.

## React primitives (`components/ui/`)

- `Button` — variants and loading state
- `Card` / `CardHeader`
- `FormField` — label, control, error, a11y ids
- `StatusChip` — read-only status display
- `Spinner`, `LoadingBlock`, `EmptyState`

## Tokens

Use CSS variables instead of hard-coded values:

- **Colors:** `--color-primary`, `--color-text`, status tokens `--status-*-bg/text/border`
- **Spacing:** `--space-1` (4px) through `--space-10` (40px), 8px grid
- **Radius:** `--radius-sm` … `--radius-full`
- **Shadow:** `--shadow-sm`, `--shadow-md`, `--shadow-lg`

Hex literals belong only in `tokens.css` (rgba allowed in shadows/overlays elsewhere).

## Primitives

### Buttons

Every interactive button must use an explicit class (no global `button` reset):

- `btn btn--primary` — main action
- `btn btn--secondary` — cancel, back
- `btn btn--outline` — secondary actions, links styled as buttons
- `btn btn--ghost` — icon buttons, tabs, low emphasis
- `btn btn--sm` — compact
- `btn btn--block` — full width (e.g. drawer confirm)
- `btn.is-loading` — in-flight action with spinner

Prefer the `Button` React component for new code.

### Surfaces

- `card` + `card__header` — page/list containers
- `section` + `section__title` — grouped content blocks

### Forms

- `field` + `field__label` + `field__control` + `field__error`
- Use `FormField` wrapper for consistent a11y

### States

- `loading-block`, `spinner`, `skeleton` — loading feedback
- `empty-state` — empty lists
- `alert alert--error` — API/validation errors

## Adding new styles

1. Prefer an existing token; add to `tokens.css` if missing.
2. If layout is screen-specific, add rules to `layout.css`, `overlays.css`, or `data-display.css`.
3. Document new patterns in the dev styleguide at `/dev/ui`.
