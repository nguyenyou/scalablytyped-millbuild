## Full Rewrite in TypeScript

### Goal
Rebuild the converter as a TypeScript-first toolchain that:
- Uses the official TypeScript Program/TypeChecker for parsing/resolution.
- Implements all transforms in TypeScript.
- Emits Scala source code directly from TypeScript (no Scala runtime required).
- Provides a modern CLI, strong test coverage, and integration hooks for sbt/Mill.

### Non-Goals (initially)
- Feature parity for every historical edge case on day one. Focus on top libraries, then backfill.
- Reusing the Scala parser, Phase 1, or Scala-side codegen.

---

## Architecture Overview
- packages/core: IR definitions, codecs, utilities
- packages/analyzer: TS Program/TypeChecker wrapper, module/exports graph, symbol resolution
- packages/transforms-ts: TS-IR transforms (formerly Phase 1 responsibilities)
- packages/codegen-scala: Scala AST model and Scala-emitter
- packages/cli: End-user CLI (Node-based)
- packages/plugins: Integration adapters (sbt, Mill, bare API)
- packages/integration-tests: Golden tests runner + fixtures

### Data Flow
1) Analyzer builds a TypeScript `Program` and `TypeChecker` from inputs (files + tsconfig).
2) Analyzer walks `SourceFile`s, querying the checker to produce IR (normalized, resolved trees).
3) Transforms (TS-side) mutate/normalize IR to satisfy Scala constraints.
4) Codegen emits `.scala` files with flavour-specific rules and naming.
5) CLI writes files and optional project scaffolding; plugins integrate into builds.

---

## IR Design
- Keep a single normalized IR that encodes declarations, types, modules, exports, namespaces, and comments.
- Prefer a pragmatic, minimal IR tailored to Scala emission rather than mirroring TS AST 1:1.
- Key requirements:
  - Fully qualified names for all exported entities
  - Resolved `typeof`/type queries
  - Namespace/class/interface/module merges unified
  - Enum constant values and literal types
  - Annotation/comments and JS location hints (global/module/both)
  - Stable identifiers for cross-file references

Suggested structure:
- File: comments, directives, members, codePath
- Decl: interface, class, enum, function, var, module/namespace, type alias, export nodes
- Type: primitives, refs, unions/intersections, function/callable, object-literal, mapped/indexed, type params/args

---

## Analyzer (TypeScript Program Wrapper)
- Inputs: entry files, tsconfig (baseUrl, paths, typeRoots, lib, strict flags).
- Responsibilities:
  - Build Program once per run; cache and incremental recompile for watch mode.
  - Resolve module graph, export graphs (including `export * as`), default/named exports.
  - Symbol qualification and unique naming (source of truth for FQNs).
  - Compute constant enums, literal types, common JSDoc/doc comments.
  - Merge namespaces/modules/interfaces/classes per TS rules.
  - Provide a clean, testable API that returns IR (no I/O side effects).

Tech: `typescript` compiler API (optionally `ts-morph` for ergonomics).

---

## Transforms (TS-side)
Reimplement only what is necessary for Scala emission; remove anything the TS checker already guarantees.

Likely required:
- Normalize callable members to methods where needed for Scala overrides
- Split methods by union-parameter explosion limits
- Intersection rejiggering to avoid pathological Scala types
- Var-to-namespace bridging for Scala packaging
- Extract/expand callables for better method surface in Scala
- Defaulted type arguments realization where necessary
- Library-specific tweaks (kept minimal, moved behind feature-flags/tests)

Likely unnecessary (TS handles):
- Export-star rewriting, qualify references, resolve type queries, enum inference

---

## Scala Code Generation
- Build a tiny Scala AST model in TypeScript aimed at emission (packages, types, classes, traits, objects, imports, methods, vals, types, type params, etc.).
- Implement flavour strategies (Normal, Slinky, Japgolly) as pluggable rules.
- Name cleaning, erasure-aware overload grouping, inheritance completion.
- Pretty-printer designed for stability (minimize diffs).

Deliverables:
- codegen-scala emits:
  - `Map<RelPath, string>` of file contents
  - Optional metadata files (e.g., bundler manifests) behind flags

---

## CLI and Plugins
- CLI commands:
  - `analyze`: build Program and print IR stats/diagnostics
  - `generate`: produce Scala sources to a target directory
  - `diff`: compare generated output to a golden set
  - `doctor`: environment validation (Node, TS, memory, paths)
- Plugins:
  - sbt/Mill adapters invoke CLI in-process or via Node; collect generated sources; hook into compile lifecycle.

---

## Project Layout (Monorepo)
- Yarn/PNPM workspaces or npm workspaces
- `packages/core`
- `packages/analyzer`
- `packages/transforms-ts`
- `packages/codegen-scala`
- `packages/cli`
- `packages/plugins/{sbt,mill}` (thin launchers)
- `packages/integration-tests`
- `fixtures/` for test inputs and goldens

Build:
- TypeScript + ESLint + Prettier + tsup/tsc build + vitest/jest for tests

---

## Testing Strategy
- Unit tests: IR nodes, transforms, codegen utilities
- Analyzer tests: reduced TS files covering language features
- Golden tests: run `generate` against curated libraries (subset of DefinitelyTyped + common real-world libs). Store goldens; diff on CI.
- Performance tests: large libraries (React, Material UI, Monaco, etc.)

---

## Migration/Parity Strategy
1) Implement core analyzer + minimal IR for interfaces/types/enums/functions.
2) Implement codegen for Normal flavour; generate small libraries.
3) Add transforms for Scala constraints; reach parity for a curated test set.
4) Expand to namespaces/modules, mapped/indexed types, complex unions/intersections.
5) Add flavours (Slinky/Japgolly), React-specific logic.
6) Grow CI matrix and golden sets; iterate until acceptable coverage.

---

## Milestones (Indicative)
- M0 (Week 1): Repo skeleton, packages, build, CI, coding standards
- M1 (Weeks 2–3): Analyzer v1 (Program build, export graph), IR v1 (interfaces/types/enums)
- M2 (Weeks 4–5): Codegen v1 (Normal flavour), small-lib goldens green
- M3 (Weeks 6–7): Transforms for Scala constraints; method normalization/splitting; more types
- M4 (Weeks 8–10): Modules/namespaces merges; mapped/indexed types; large-lib perf pass
- M5 (Weeks 11–12): Flavours (Slinky/Japgolly), React suite
- M6 (Weeks 13+): sbt/Mill plugins, docs, migration guide; broaden golden suite

---

## Risks and Mitigations
- TS API complexity: encapsulate with `packages/analyzer` and maintain strict unit tests.
- Emission stability: implement a deterministic pretty-printer with snapshot tests.
- Performance: single Program per run, incremental rebuilds, cache IR by content hash.
- Feature gaps: prioritize high-impact libraries; triage the long tail with reduced repros.

---

## Deliverables
- CLI (`npx stts`) capable of reading a project and generating Scala sources
- Programmatic API for build tools
- Documentation: quickstart, configuration, troubleshooting, contribution guide
- Golden test suite with baseline outputs and a diff runner

---

## Implementation Notes
- Pin `typescript` version; expose it in logs and metadata
- Support `tsconfig` inheritance, `paths` and `baseUrl` resolution, `types` and `typeRoots`
- Keep IR versioned to allow evolution without breaking consumers
- Include structured diagnostics (codes, locations, suggestions)


