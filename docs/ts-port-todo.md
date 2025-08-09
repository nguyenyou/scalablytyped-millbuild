## TypeScript Port – Detailed TODO

This checklist tracks the end-to-end implementation of the TypeScript-based converter. Use TDD for each feature: add failing test, implement, make green.

### Legend
- [ ] not started
- [~] in progress
- [x] done

---

### M0 – Bootstrap and Green CI
- [x] Create `tools/ts-service` workspace with TS + Vitest
- [x] Minimal analyzer skeleton and first tests
- [ ] Add GitHub Actions CI: Node 18/20, cache `node_modules`, run `npm test` in `tools/ts-service`
- [ ] Code quality: ESLint + Prettier, basic rules

Acceptance: CI green on PRs; base tests pass locally and in CI.

---

### M1 – Analyzer Infrastructure (TS Program/TypeChecker)
- [x] Build Program from entry files
- [x] Resolve exports including re-exports and `export *`
- [ ] Load and apply `tsconfig.json` (baseUrl, paths, typeRoots, lib)
- [ ] Honor TS directives and ambient hints: `/// <reference ...>`, `export as namespace`, `/* amd-module name */`
- [ ] Support TS project references (tsconfig `references`)
- [ ] Normalize CommonJS patterns (default export interop) for parity with `HandleCommonJsModules`
- [ ] Diagnostics pipeline (collect TS diagnostics; map to structured JSON)
- [ ] Watch/incremental mode (optional, behind flag)
 - [ ] Resolve cross-file and npm package imports via TS resolver (include non-entry files reachable from entries)
 - [ ] Respect package.json `types`/`typings` and `@types` resolution rules

TDD targets:
- [ ] Re-exports coverage: default, named, `export * as`
- [ ] Module path resolution with `baseUrl` + `paths` aliasing
- [ ] Diagnostics for missing modules and syntax errors
 - [ ] Cross-file import from sibling module is included in IR
 - [ ] Importing types from npm package works (first-party `types` and `@types`)

Acceptance: Analyzer returns deterministic export sets and paths under tsconfig scenarios.

---

### M2 – IR v1 (Schema + Encoding)
- [x] Seed IR types (`ProjectIR`, `FileIR`)
- [ ] Define v1 IR for declarations: interface, class, enum, function, var, module/namespace, type alias, export nodes
- [ ] Define v1 IR for types: primitives, refs, unions/intersections, function types, object literals, index/mapped types, generics
- [ ] Represent JS location (global/module/both) and global vs module mapping
- [ ] Represent directives and module augmentation metadata
- [ ] Version the IR (`irVersion`) and add JSON codecs

TDD targets:
- [ ] Fixtures covering each decl kind and type node
- [ ] Round-trip tests (IR encode/decode stability)

Acceptance: IR captures sufficient info for codegen; tests enforce stability.

---

### M3 – Declarations Extraction (Checker-backed)
- [ ] Extract declarations with fully-qualified export names
- [ ] Resolve `typeof` type queries
- [ ] Merge namespaces/modules/interfaces/classes per TS rules (incl. declaration merging and `declare module` augmentation)
- [ ] Const enum values and literal types

TDD targets:
- [ ] `typeof` against values; default exports; namespace merges
- [ ] Const enums produce literal values in IR

Acceptance: IR matches expected shapes for complex declaration patterns.

---

### M4 – Transforms (Scala Constraints Oriented)
- [ ] Normalize callable properties → methods (override-friendly)
- [ ] Split methods by union-parameter explosion thresholds
- [ ] Intersection rejiggering to avoid pathological Scala types
- [ ] `var`→namespace bridging where required
- [ ] Defaulted type arguments materialization (when beneficial)
- [ ] Library-specific knobs (flags) for edge cases
- [ ] Extract interfaces from object literal types where needed for emission (`ExtractInterfaces`)
- [ ] Expand callable types to method forms (`ExpandCallables`)
- [ ] Rewrite `this` types to Scala-representable forms (`RewriteTypeThis`)
- [ ] Drop/adjust properties not representable in Scala (`DropProperties`)
- [ ] Infer missing return types via checker (`InferReturnTypes`)
- [ ] Expand unions derived from `keyof` where needed (`UnionTypesFromKeyOf`)

TDD targets:
- [ ] Override scenarios that fail without normalization
- [ ] Large unions produce bounded number of overloads

Acceptance: Transformed IR compiles to valid Scala in representative cases.

---

### M5 – Scala Codegen (TypeScript)
- [ ] Implement minimal Scala AST (packages, imports, types, traits/classes/objects, methods, vals)
- [ ] Normal flavour codegen: package layout, naming, imports
- [ ] Name cleaning and conflict resolution
- [ ] Overload grouping with erasure-aware signatures (`CombineOverloads`)
- [ ] Inheritance completion (where needed pre-emit)
- [ ] Pretty-printer (deterministic, low-diff)
- [ ] Union-to-inheritance conversion and union length limiting (`UnionToInheritance`, `LimitUnionLength`)
- [ ] Remove multiple inheritance and deduplicate members (`RemoveMultipleInheritance`, `Deduplicator`)
- [ ] Fake/boxed literal types where required (`FakeLiterals`)
- [ ] Optional name shortening consistent with legacy (`ShortenNames`)

TDD targets:
- [ ] Snapshot tests of emitted Scala for small fixtures
- [ ] Name-collision and import-dedup tests

Acceptance: Emits compilable Scala for Normal flavour on core fixtures.

---

### M6 – Flavours and React Support
- [ ] Add Slinky flavour rules
- [ ] Add Japgolly flavour rules
- [ ] React components identification and companion generation

TDD targets:
- [ ] React fixture suites for both flavours

Acceptance: Flavour outputs compile in demo projects.

---

### M7 – Build Integration and CLI
- [ ] CLI commands: `analyze`, `generate`, `diff`, `doctor`
- [ ] Output layout configuration (target dir structure)
- [ ] sbt/Mill thin plugins invoking CLI and wiring sources
- [ ] Configuration: Scala version(s), Scala.js version, flavour selection
- [ ] Optional bundler manifest generation (Scala.js bundler metadata)
- [ ] Optional README/metadata generation akin to current ProjectReadme

TDD targets:
- [ ] E2E test: run CLI on fixtures → files on disk; diff against goldens

Acceptance: Simple projects integrate with sbt/Mill and compile.

---

### M8 – Performance and Caching
- [ ] One Program per project; batch analysis
- [ ] Cache IR per file content-hash; reuse across runs
- [ ] Parallelize safe phases (I/O, emission)

Benchmarks:
- [ ] Large libs (React, Material UI, Monaco) timing and memory budgets

Acceptance: Meets target run-time for selected libraries on CI hardware.

---

### M9 – Compatibility and Parity
- [ ] Curate golden set of libraries and expected outputs
- [ ] Optional: diff against legacy Scala-based outputs where meaningful
- [ ] Document known deviations and rationale
- [ ] Stdlib selection parity (tsconfig `lib` mapping and legacy `-stdlib` behavior)

Acceptance: Golden set stable across PRs; deviations intentional and documented.

---

### M10 – Documentation and Release
- [ ] User docs: quickstart, configuration, troubleshooting
- [ ] Contributor docs: architecture, testing, coding standards
- [ ] Versioning and changelog
- [ ] Release pipeline (npm publish for CLI, tags)

Acceptance: Public release usable with clear docs and basic support flow.

---

### Immediate Next TDD Tasks
- [ ] Implement `tsconfig` loading and `paths`/`baseUrl` resolution (tests)
- [ ] Extract declarations (interfaces/types/functions) into IR v1 (tests)
- [ ] Add CLI `analyze <entries>` to emit IR JSON (smoke tests)


