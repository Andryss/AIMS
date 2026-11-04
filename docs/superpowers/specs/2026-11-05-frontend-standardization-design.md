# Frontend standardization

The user approved implementing all recommendations from the frontend audit in this chat.

## Intent

Reduce duplicated infrastructure while keeping the existing workflows, permissions, Russian copy, API contracts and
navigation intact. Keep domain-specific fields and rules explicit.

## Approach

Extract small shared hooks and presentational components, then migrate their existing consumers. Do not introduce a
configurable CRUD framework or new dependencies. Keep explicit typed API functions, split transport and domain modules,
and preserve the client barrel for compatibility with existing callers and test doubles.

## Scope

- Shared list URL parsing, debounced search, loading, stale-response handling and pagination.
- Modal shell, reason confirmation, guarded mutation lifecycle and domain transition functions.
- Paged history loading with preceding snapshot, timeline and diff rendering/comparison.
- Incident detail sections and hooks for comments, assignments, history and cleanup.
- Session/access context and data-driven navigation using the existing access rules.
- Shared `Page<T>`, `PageQuery` and `HistoryEntry<T>`, domain-specific types and API modules.
- Consistent Pagination, Card/CardHeader, EditIcon, neutral shared CSS names and utilities.
- Shared anchored overlay positioning/delays while preserving tooltip/dialog semantics.
- Removal of the unused IncidentViewModal.

## Constraints

No backend/API changes or new packages. Preserve URL filters and return links, lazy incident history, old-save and
old-GET guards, history page boundaries, file retry reuse, status conflicts, exact unchanged timestamps, and
user-specific access restrictions. Existing tests remain valid unless a presentation-only structure changes. Verify the
full frontend suite, TypeScript and production build.

## Alternatives

A universal CRUD renderer would obscure domain behavior; a file-only reorganization would leave duplicated behavior.
Small shared mechanisms with explicit domain components are the chosen approach from the audit.
