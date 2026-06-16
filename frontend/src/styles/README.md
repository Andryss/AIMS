# AIMS UI styles

Mini design system for the AIMS frontend: CSS custom properties, primitives, status chips, and layout-specific rules. No external UI library.

## File layout

| File | Purpose |
|------|---------|
| `tokens.css` | Design tokens: colors, spacing, typography, radii, shadows |
| `primitives.css` | Reusable UI: buttons, cards, sections, forms, text utilities |
| `status.css` | Incident and cleanup status selects/chips |
| `components.css` | App shell, tables, tabs, drawers, modals, pickers |

Import order in `index.tsx`: `tokens` → `primitives` → `status` → `components`.

## Tokens

Use CSS variables instead of hard-coded values:

- **Colors:** `--color-primary`, `--color-text`, `--color-border`, `--color-bg-page`, etc.
- **Spacing:** `--space-1` (4px) through `--space-10` (40px), 8px grid
- **Radius:** `--radius-sm` … `--radius-full`
- **Shadow:** `--shadow-sm`, `--shadow-md`, `--shadow-lg`

## Primitives

### Buttons

Every interactive button must use an explicit class (no global `button` reset):

- `btn btn--primary` — main action
- `btn btn--secondary` — cancel, back
- `btn btn--outline` — secondary actions, links styled as buttons
- `btn btn--ghost` — icon buttons, tabs, low emphasis
- `btn btn--sm` — compact
- `btn btn--block` — full width (e.g. drawer confirm)

### Surfaces

- `card` + `card__header` — page/list containers
- `section` + `section__title` — blocks inside a card (no extra border/shadow)
- `data-list` + `data-list__row` — definition lists for detail views

### Forms

- `form` — vertical stack with token gaps
- `field`, `field__label`, `field__control` — labeled inputs (optional)

### Text

- `text-muted`, `text-error`, `text-pre-wrap`

## Status

- Read-only: `status-chip status-chip--{status}` (e.g. `status-chip--open`)
- Editable: `status-select status-chip--{status}` on `<select>`
- Cleanup: `status-chip--cleanup_{status}`

Wrap in `status-select-wrap` or `status-select-wrap status-select-wrap--modal` for layout.

## Patterns

- **List page:** one `card`, `card__header`, table, `pagination`
- **Detail page:** one `card`, `section` for fields, `section` with `tabs` for comments/history
- **Modal/drawer:** `modal-overlay` / `drawer-overlay` + surface + `modal-actions` or `drawer__footer`

## Adding new UI

1. Prefer existing primitives and tokens.
2. If layout is screen-specific, add rules to `components.css` using tokens.
3. Do not reintroduce global element selectors for `button`, `a`, etc.
