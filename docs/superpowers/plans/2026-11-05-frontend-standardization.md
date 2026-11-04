# Frontend Standardization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Implement all nine audit recommendations without changing domain behavior.

**Architecture:** Small shared hooks and UI primitives own repeated mechanics; domain components retain fields, permissions and workflows. Keep compatibility barrels while splitting API and types by domain. Independent list, modal and history areas have separate file owners.

**Tech Stack:** Existing React 19, TypeScript 4.9, React Router 6, CRA/Jest/Testing Library; no new packages.

**Spec:** `docs/superpowers/specs/2026-11-05-frontend-standardization-design.md`

## Global Constraints

- No backend/API changes or new packages.
- Preserve existing access rules, Russian copy, URL filters and return navigation.
- Preserve stale GET/save guards, history page boundaries, file retry reuse, conflict handling and unchanged timestamp precision.
- Work on `codex/frontend-standardization`; keep the user's unrelated untracked backend file untouched.
- Existing behavior tests are regression coverage for mechanical extractions; add behavioral tests for new shared lifecycle mechanisms before implementation.

## Review Focus

- Switching token/entity during a pending request must not update a different card.
- A filter change must reset page and ignore the previous request.
- First history entry on a later page must compare against its preceding snapshot.
- A blocked modal must reject Escape, backdrop and duplicate submit consistently.
- Context extraction must preserve role-specific navigation and domain-specific transition authorization.

### Task 1: Lists and search (list owner)

**Files:** `hooks/useListSearchParams.ts`, `hooks/usePaginatedResource.ts`, `hooks/useDebouncedSearch.ts`; list tabs and picker/search components; corresponding tests.

**Interfaces:** Domain API imports continue through `api/client`. Generic hooks accept typed loaders and explicit scope keys. URL hook exposes params, normalized page, setFilter and setPage. No changes to modal/history files, API/types, or App.

- [x] Write meaningful hook tests for stale responses, filter/page updates and debounce; run to establish RED.
- [x] Extract shared mechanics and migrate equipment, tenders, detention, movements, aliens, incidents and monitoring lists, plus repeated picker search.
- [x] Use the existing Pagination in every list, preserving each screen's visibility policy.
- [x] Run affected list/picker tests and report command/result.

Task 1 verification: new hook contracts plus account-switch and employee-selection regressions were exercised through RED/GREEN runs (`/tmp/aims-list-red.log`, `/tmp/aims-list-page-red.log`, `/tmp/aims-list-employee-red.log`). Focused command `CI=true npm test -- --watchAll=false --runInBand src/hooks/listHooks.test.tsx src/components/EquipmentTab.test.tsx src/components/TendersTab.test.tsx src/components/DetentionRequestsTab.test.tsx src/components/EquipmentMovementsTab.test.tsx src/components/AlienKnowledgeBaseTab.test.tsx src/components/IncidentsTab.test.tsx src/components/MonitoringAlertsTab.test.tsx src/components/AlienPickerDrawer.test.tsx src/components/UserPickerDrawer.test.tsx src/components/EmployeeSearch.test.tsx` passed 11 suites / 87 tests (`/tmp/aims-list-focused.log`). A full shared-checkout suite run passed 72 suites / 627 tests before the final employee-selection regression was added (`/tmp/aims-list-full.log`); final integration rerun remains Task 7. Final owned-file ESLint, TypeScript (`npx tsc --noEmit`) and diff whitespace checks passed.

### Task 2: Modals, mutations and statuses (modal owner)

**Files:** `components/ui/Modal.tsx`, `components/ui/ConfirmReasonDialog.tsx`, `components/ui/StatusSelect.tsx`, `hooks/useMutation.ts`, editor/status components and domain transition modules; corresponding tests.

**Interfaces:** `Modal` takes open, title, onClose, busy, children, className. `useMutation` owns a scope key, busy guard, error and stale completion checks. Existing domain component props and API barrel imports remain compatible. Do not edit IncidentDetailPage, list tabs, histories, PickerDrawerShell, API/types or App.

- [x] Test blocked backdrop/Escape, focus restoration and stale/duplicate mutation behavior; establish RED.
- [x] Extract Modal and confirmation UI; migrate editor/receipt/participant dialogs and status dialogs, preserving upload/conflict behavior.
- [x] Move transition authorization into pure domain functions and reuse a presentational StatusSelect.
- [x] Run affected modal/status tests and report command/result.

### Task 3: History infrastructure (history owner)

**Files:** `hooks/usePagedHistory.ts`, `components/ui/HistoryTimeline.tsx`, `components/ui/HistoryDiffList.tsx`, shared diff utility; EquipmentHistory, EquipmentMovementHistory, TenderHistory, DetentionRequestHistory, EquipmentHistoryChanges and their diff utilities/tests.

**Interfaces:** Keep existing domain component props and `api/client` imports. Generic loader returns a structural paged result and preceding entry. Shared diff values retain discriminated entity references; field definitions remain domain-specific. Do not edit IncidentDetailPage, list/modal files, App, API/types.

- [x] Test stale history requests and preceding snapshot behavior; establish RED.
- [x] Extract paged loading, timeline, diff row rendering and repeated field comparison; migrate four histories.
- [x] Run affected history/diff tests and report command/result.

### Task 4: Domain transport structure and shared utilities (root)

**Files:** `types/*.ts`, `types.ts` compatibility barrel; `api/http.ts`, domain API files, `api/client.ts` compatibility barrel; `utils/dateTime.ts`, `utils/params.ts`, label/util consumers.

**Interfaces:** Preserve all existing exported names and HTTP paths/payloads. Add `Page<T>`, `PageQuery`, `HistoryEntry<TSnapshot>` in common types. Export `formatDateTime`, `positiveId`, enum/page parsers from neutral utility files.

- [x] Run existing API/formatting tests as baseline; add normalization boundary tests and establish RED for shared parsers.
- [x] Extract common types, split domain types/API, migrate neutral date/id utilities without changing local-input precision.
- [x] Run API/util tests and TypeScript.

### Task 5: Auth, navigation and incident composition (root)

**Files:** `auth/AuthContext.tsx`, `auth/access.ts`, `auth/navigation.ts`, `hooks/useAuthSession.ts`; App, AppHeader, IncidentDetailPage and new incident section/hook files.

**Interfaces:** Keep exported domain component props compatible with tests. `useAuth` provides token/profile/access; derive access using existing roles/permissions, then share it with navigation/routes. Extract incident behavior with explicit typed section props; root composes sections.

- [x] Add access/navigation authorization tests, run RED.
- [x] Extract session/access, reduce App prop plumbing and map navigation configuration.
- [x] Extract comments/history/assignments/cleanup from IncidentDetailPage, preserving loading and invalidation behavior.
- [x] Run App, incident detail/editor, and role/access tests.

### Task 6: UI consistency, anchored overlays and cleanup (root)

**Files:** `ui/EditIcon.tsx`, Card consumers, `hooks/useAnchoredOverlay.ts`, AlienSummary, ThreatCalculationDetails, PickerDrawerShell, shared CSS and README; remove IncidentViewModal.

**Interfaces:** Keep semantic tooltip/dialog markup with domain components; overlay hook owns timers and placement only. Card forwards section attributes including aria-busy. Neutral CSS names apply to shared primitives.

- [x] Use existing overlay/focus tests as regression coverage; test shared overlay lifecycle before extraction.
- [x] Reuse Card/CardHeader/EditIcon, generalize drawer close guards, rename shared CSS classes, remove unused modal.
- [x] Run focused UI and overlay tests.

### Task 7: Integration and review (root)

- [x] Review changed files and shared interfaces; resolve integration issues.
- [x] Run `CI=true npm test -- --watchAll=false --runInBand`, `npx tsc --noEmit`, `npm run build` in frontend.
- [x] Obtain independent whole-change review and address important findings with behavioral regression tests.
- [x] Record final checks and completion status; leave changes reviewable in the working branch.

## Execution ledger

- Plan/design derive from the audit approved by the user's instruction to implement all changes.
- Worktree ruling: use the shared checkout on a new branch; only unrelated untracked backend metadata existed, and preserving the user's current workspace avoids a second checkout with separate dependencies.
- Baseline frontend suite running; log `/tmp/aims-frontend-baseline.log`.

- Task 3: complete — extracted `usePagedHistory`, `HistoryTimeline`, `HistoryDiffList` and typed `compareHistoryFields`; migrated equipment, movement, tender and detention histories without changing their field definitions, reasons or permission-aware links. `HistoryPage<T>` aliases the shared `Page<T>`.
- Task 3: RED→GREEN demonstrated old-scope snapshot leakage, returning to a previously paged entity and keeping same-entity reference labels available during pagination. Added seven lifecycle tests plus timeline boundary/value-rendering and generic structured-diff coverage.
- Task 3: verification — `CI=true npm test -- --watchAll=false --runInBand History historyDiff` → 7 suites / 28 tests passed; `npx tsc --noEmit` → passed. Full suite during parallel work → 71 suites passed / 1 failed; 625 tests passed / 2 failed, both in the modal owner's `useMutation.test.tsx` (`rejects a retained submit callback from a different scope`; `does not let a finished session clear a retry or replace its error`). Logs: `/tmp/aims-history-focused-final.log`, `/tmp/aims-history-tsc-final.log`, `/tmp/aims-history-full.log`.

- Task 2 complete: shared Modal, ConfirmReasonDialog, StatusSelect and scoped useMutation now serve all editor/receipt/participant/status dialogs and winner selection; transition authorization lives in pure domain modules. RED established for duplicate submits, stale editor state, retained stale callbacks and stale retry completion. Focused verification: 15 suites / 127 tests passed (`CI=true npm test -- --watchAll=false --runInBand --testPathPattern='(EditorModal|TenderReceiptModal|StatusSelect|TenderParticipants|useMutation|ui/Modal).test'`, `/tmp/aims-modal-focused.log`); owned-file ESLint passed. Existing act warnings remain. Full suite deferred to integration; concurrent root incident composition had TypeScript errors when checked.

### Final integration evidence

- All nine audit areas implemented; compatibility barrels preserve existing API/type exports.
- IncidentDetailPage is 414 lines (previously 1219), composing independent sections and resource hooks.
- Independent whole-change review found one introduced P2: an optional unread-counter request delayed profile availability. A new test failed first, then passed after profile publication was made independent again. No other introduced P1/P2 findings.
- Additional cleanup drawer regression failed first when an old incident save updated the next drawer; migrating it to the shared scoped mutation lifecycle fixed it.
- `CI=true npm run test:coverage -- --runInBand`: 76 suites, 639 tests passed; configured coverage thresholds passed. Log `/tmp/aims-final-coverage.log`.
- `./node_modules/.bin/tsc --noEmit`: passed. Log `/tmp/aims-final-types.log`.
- `npm run build`: compiled successfully. Log `/tmp/aims-final-build.log` (existing outdated Browserslist data notice only).
- Production ESLint: no issues. Repository-wide ESLint: 115 existing test errors and 3 warnings, versus HEAD's 116 errors and 6 warnings; comparison found no introduced lint findings. Logs `/tmp/aims-eslint-baseline.json`, `/tmp/aims-eslint-final.json`.
- `git diff --check`: passed.
- Changes left uncommitted on `codex/frontend-standardization` for review. No backend changes, dependency changes, merge, push or deployment.

### Follow-up: repository-wide ESLint cleanup

- User requested resolving the remaining 115 test lint errors and 3 warnings.
- Replaced direct DOM traversal with Testing Library queries, made permission expectations unconditional, waited for observable async completion, and removed unused imports. Existing 639 test cases are retained.
- Added accessible form names and a real file-input label; two styling-only wrapper assertions use stable test IDs. ESLint rules/configuration were not relaxed or suppressed.
- Added `npm run lint`, covering `src` and `scripts` with JS/JSX/TS/TSX/MJS extensions and `--max-warnings=0`; command passed.
- Final verification: 76 suites / 639 tests with coverage passed, TypeScript passed, production build compiled successfully, whitespace checks passed.
- Logs: `/tmp/aims-testlint-final-eslint.log`, `/tmp/aims-testlint-final-tests.log`, `/tmp/aims-testlint-final-types.log`, `/tmp/aims-testlint-final-build.log`.
- Independent focused review found no confirmed P1/P2 in inspected cleanup; its combined diff output was truncated, so it did not inspect every test. Root and implementers reviewed their changes and all integration checks passed.
