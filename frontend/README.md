# AIMS Frontend

React single-page application for the MIB Alien Incident Management System (AIMS). The production build is copied into the Spring Boot backend and served under `/public` (see `npm run build:backend`).

See also: [project README](../README.md) · [backend README](../backend/README.md)

## Prerequisites

- Node.js 20+ (matches [CI workflow](../.github/workflows/frontend-test.yml))
- npm (lockfile: `package-lock.json`)

For local API calls during development, run the [backend](../backend/README.md) on port 8080 (`proxy` in `package.json` forwards to it).

## Scripts

| Command | Description |
|---------|-------------|
| `npm start` | Dev server at [http://localhost:3000](http://localhost:3000) |
| `npm test` | Jest in watch mode (via Create React App) |
| `npm run test:coverage` | Tests with coverage thresholds (`CI=true` for non-interactive) |
| `npm run build` | Production bundle to `build/` |
| `npm run build:backend` | Build and copy static assets into `backend/src/main/resources/static/public/` |

## Technology stack

### Runtime (shipped in the production SPA)

| Library | Version | Role |
|---------|---------|------|
| [React](https://react.dev/) | 19.2.6 | UI library |
| [react-dom](https://react.dev/) | 19.2.6 | DOM rendering |
| [react-router-dom](https://reactrouter.com/) | 6.30.4 | Client-side routing |
| [web-vitals](https://github.com/GoogleChrome/web-vitals) | 2.1.4 | Optional performance metrics (if enabled) |

The built bundle includes transitive dependencies of the packages above (for example `@remix-run/router` from React Router). All **direct** production npm dependencies use permissive open-source licenses.

### Build and development (not shipped to end users)

| Tool | Version | Role |
|------|---------|------|
| [TypeScript](https://www.typescriptlang.org/) | 4.9.5 | Static typing |
| [Create React App](https://create-react-app.dev/) / [react-scripts](https://www.npmjs.com/package/react-scripts) | 5.0.1 | Dev server, webpack/babel toolchain, Jest runner |
| [Testing Library](https://testing-library.com/) | see `package-lock.json` | Component tests (`@testing-library/react` 16.3.2, etc.) |

CRA pulls a large transitive tree (Babel, webpack, ESLint, etc.) used only at build and test time. Those packages are **not** included in the static files deployed with the backend JAR.

## Licenses and commercial use

This section describes **project policy** for dependency licensing. It is not legal advice; confirm requirements with your organization if needed.

### Allowed in production / runtime artifacts

Licenses that permit commercial use without requiring your application code to be open-sourced:

- MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, ISC, 0BSD, Unlicense, CC0-1.0

### Allowed only as build or test tools

Tools that do not ship inside the production SPA (documented separately; may use copyleft licenses):

- EPL-1.0 / EPL-2.0 (e.g. some Jest/Eclipse-related tooling in the dev tree)
- LGPL-2.1 (only when used as a build-time tool, not linked into the shipped bundle)
- MPL-2.0 (e.g. `axe-core` pulled by dev/test tooling — not part of the runtime `dependencies` tree)

### Not acceptable in production `dependencies`

GPL, AGPL, or LGPL libraries that would be distributed as part of the commercial product runtime. None are declared as direct production dependencies.

### Direct dependency license summary

| Library | Version | License | Commercial use |
|---------|---------|---------|----------------|
| react | 19.2.6 | MIT | Yes |
| react-dom | 19.2.6 | MIT | Yes |
| react-router-dom | 6.30.4 | MIT | Yes |
| web-vitals | 2.1.4 | Apache-2.0 | Yes |

### Re-running the license audit

Production npm dependencies only:

```bash
cd frontend
npx license-checker --production --excludePrivatePackages --summary
```

Full dev + build tree (expect additional permissive and build-only licenses):

```bash
npx license-checker --excludePrivatePackages --summary
```

As of the last audit, production dependencies reported only **MIT** and **Apache-2.0**.
