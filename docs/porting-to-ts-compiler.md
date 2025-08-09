## Porting to the TypeScript Compiler

### Why
- **Precision**: Leverage the official TypeScript Program/TypeChecker for module resolution, symbol binding, `export *`/namespace merges, `typeof` queries, const-enum inference, etc.
- **Simplicity**: Replace the custom Scala parser and many Phase 1 transforms with TS-verified semantics.
- **Maintainability**: Track TS language evolution by upgrading the `typescript` npm dependency.

### Scope and Non-Goals
- **In-scope**: Replace Phase 1 parsing/semantics with a TS-compiler–backed service. Keep Scala-side code generation initially.
- **Out-of-scope (initially)**: Rewriting Scala codegen in Node. We may consider it later once parity is achieved.

## Current Architecture (Summary)
- Phase 1 (TypeScript side, Scala):
  - Custom lexer/parser and transformations over `TsParsedFile` (e.g., `NormalizeFunctions`, `QualifyReferences`, `ReplaceExports`, `FlattenTrees`, etc.).
  - Entry point: `Phase1ReadTypescript` with a pluggable `parser: InFile => Either[String, TsParsedFile]`.
- Phase 2 (Scala.js IR → Scala code):
  - Converts TS IR into Scala packages and applies Scala-targeted transforms (overload erasure, inheritance cleanup, React flavours).
  - Entry point: `Phase2ToScalaJs` → `Printer` emits `.scala`.
- Phase 3 (Packaging/compile):
  - Produces build layout and compiles (Bloop/SBT integration).

Key TS IR type: `TsParsedFile` (and `Ts*` tree family) in `mcore/src/org/scalablytyped/converter/internal/ts/trees.scala`.

## Target Architecture (TS-centric)
- **Node TS Service (daemon/CLI)**: Build one TypeScript `Program` per library import, keep it warm, query `TypeChecker` to produce a normalized IR:
  - Resolved exports/re-exports, fully qualified names, namespace merges, type queries, enum values, ambient/declare handling, etc.
  - Emit compact JSON matching our current `TsParsedFile` (or a strict superset).
- **Scala Bridge**: Replace the `parser` used by `Phase1ReadTypescript` with a new adapter that calls the Node service and converts JSON → `TsParsedFile`.
- **Downstream**: Keep Phase 2/3 as-is initially. Later, retire redundant Phase 1 transforms once TS outputs already-resolved semantics.

## Strategy (Incremental, Parity-First)
### 1) Baseline & Guardrails
- Snapshot current generated outputs (tests under `tests/*`) to serve as goldens.
- Add a feature flag `--useTsCompiler` to switch between Scala parser and TS service.

### 2) IR Contract
- Start with a JSON schema mirroring `TsParsedFile` and the core `Ts*` nodes needed by Phase 2.
- Include resolved info that lets us eventually drop Scala transforms (e.g., qualified names, resolved exports, const enum values).

Example (illustrative only):
```json
{
  "comments": [],
  "directives": [{"kind": "NoStdLib"}],
  "members": [
    {
      "kind": "DeclInterface",
      "name": "Foo",
      "tparams": [],
      "members": [
        {"kind": "MemberProperty", "name": "bar", "type": {"kind": "TypeString"}}
      ],
      "jsLocation": {"kind": "Global"}
    }
  ],
  "codePath": "NoPath"
}
```

### 3) Node TS Program Service
- Inputs: list of files, `tsconfig`/compiler options (mirror our `CompilerOptions`/`TsConfig`), working dir.
- Build a `Program` once per library; use `TypeChecker` to:
  - Resolve module paths, `export *` graphs, default/named exports, symbol FQNs.
  - Resolve `typeof`/type queries, constant values for enums, literal inferences.
  - Surface namespace/ambient merges as unified declarations.
- Output: compact JSON IR (batched per library or per file).
- Interface: JSON-RPC over stdio (daemon) or a single-shot CLI. Prefer a long-lived process for performance.

### 4) Scala Bridge (`TSCompilerParser`)
- New class implementing the same signature as the current parser: `InFile => Either[String, TsParsedFile]`.
- Talks to the Node service (keep it warm), deserializes JSON → `TsParsedFile` using small adapters.
- Integrate with existing `PersistingParser` cache (store IR JSON hashed per file content).

### 5) Parity Run (Transforms ON)
- Use the TS service to produce `TsParsedFile`, then run the full Phase 1 pipeline unchanged.
- Compare outputs against goldens; enrich the Node IR as needed to close diffs.

### 6) Retire Redundant Scala Transforms (Gated)
- Disable (behind flags), validate parity, then remove:
  - `modules.HandleCommonJsModules`, `modules.RewriteExportStarAs`
  - `T.QualifyReferences`, `T.ResolveTypeQueries`
  - Large parts of `modules.ReplaceExports`, selected `FlattenTrees` responsibilities
  - Inference steps made redundant by checker: `T.InferEnumTypes`, `T.InferTypeFromExpr`
- Keep transforms motivated by Scala constraints (e.g., `NormalizeFunctions`, `SplitMethods`, `VarToNamespace`, intersection tweaks) until later.

### 7) Codegen Path Decision
- Option A (recommended initially): Keep Phase 2 and `Printer` in Scala. Lowest risk and effort.
- Option B (later): Reimplement Phase 2 transforms and `Printer` in Node/TS for a single-runtime toolchain.

## Detailed Task Breakdown
### A. Flags and Wiring
- Add `--useTsCompiler` flag to `mcli` and CI.
- In `mcli/Main.scala` and `mimporter/.../Ci.scala`, choose between `PersistingParser(...)` (legacy) and `TSCompilerParser(...)` (new).

### B. Node Service
- Create a small package under `website/` or a new `tools/ts-service/` folder with:
  - `program-loader.ts`: builds Program and caches by project root.
  - `ir-emitter.ts`: walks AST + TypeChecker, emits normalized IR JSON.
  - `server.ts`: stdio JSON-RPC daemon; supports commands: `init`, `analyze`, `shutdown`.
- Pin `typescript` in `package.json` and expose the version in logs.

### C. IR Adapters
- TS → JSON: normalize constructs to our IR names (e.g., `DeclModule`, `DeclNamespace`, `Export*`, `TypeRef`, etc.).
- JSON → Scala: small decoders to `TsParsedFile` and friends; include an IR version field for migration.

### D. Caching & Performance
- One Program per library; batch files.
- Cache IR per file hash; reuse across runs via existing parse cache location.
- Keep the Node daemon warm during a run; shutdown cleanly at the end.

### E. Testing & CI
- Run the existing `tests/*` in dual mode (legacy vs TS) and diff outputs.
- Add an `--emitIr` debug flag to dump per-file IR for diagnosis.
- CI: initially non-blocking for TS mode, then make it the default once green.

## What Gets Removed vs Kept (Guidance)
Likely removable (after TS service parity):
- Module/exports handling: `HandleCommonJsModules`, `RewriteExportStarAs`, large parts of `ReplaceExports`.
- Resolution steps: `QualifyReferences`, `ResolveTypeQueries`.
- Inference steps where TypeChecker provides the answer: `InferEnumTypes`, `InferTypeFromExpr`.

Likely kept (Scala-target constraints):
- `NormalizeFunctions`, `SplitMethods`, `VarToNamespace`, `DefaultedTypeArguments`, intersection and inheritance simplifications.
- Phase 2 transforms: erasure/overload management, inheritance completion, flavour-specific React handling.

## Risks and Mitigations
- **IPC/size overhead**: Use a daemon and batch requests, compress JSON, and cache by content-hash.
- **IR impedance mismatch**: Start by mirroring `TsParsedFile` to maximize reuse of Phase 2.
- **Semantic edge cases**: Drive with existing `tests/*/in/*`; add reduced repros where needed.
- **Version drift**: Pin `typescript` and log its version; include in conversion metadata.

## Milestones (Indicative)
- M0 (1–2 days): Add flags, freeze baselines, scaffold Node service/Scala bridge.
- M1 (1–2 weeks): Program build + minimal IR + Scala bridge; parse-only parity for a subset.
- M2 (1–2 weeks): Full test-suite parity with Phase 1 transforms still enabled.
- M3 (1–2 weeks): Retire redundant Phase 1 transforms while staying green.
- M4 (2–6 weeks, optional): Port Phase 2/Printer to Node if desired.
- M5 (1 week): Default to TS mode in CI; keep legacy parser behind an opt-in flag; deprecate later.

## Immediate Next Steps
- Add `--useTsCompiler` flag and wire a stub `TSCompilerParser`.
- Define IR JSON (v1) mirroring `TsParsedFile` and implement basic encoders/decoders.
- Implement Node `Program` and emit minimal declarations/types; run diffs on a handful of libraries.

## Key Files to Touch
- Parser wiring: `mcli/src/org/scalablytyped/converter/cli/Main.scala`, `mimporter/src/org/scalablytyped/converter/internal/importer/Ci.scala`.
- Phase 1: `mimporter-portable/src/org/scalablytyped/converter/internal/importer/Phase1ReadTypescript.scala` (leave pipeline intact initially).
- New bridge: `mts/src/org/scalablytyped/converter/internal/ts/parser/TSCompilerParser.scala` (new).
- IR types: `mcore/src/org/scalablytyped/converter/internal/ts/trees.scala` (leave), add JSON codecs if needed.
- Node service: new folder (e.g., `tools/ts-service/`), referenced by CLI.

## Operational Notes
- The codebase currently enforces that `node_modules/typescript/lib` exists; keep that requirement and surface helpful errors.
- Log the `typescript` version and the effective `tsconfig` used for reproducibility.


