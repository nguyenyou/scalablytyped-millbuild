s## Scala Converter – End-to-End Flow

This diagram shows the current Scala-based pipeline from reading your `package.json` (e.g., with `class-variance-authority`) to writing generated Scala sources under `my-sources/`.

```mermaid
graph TD
  U["User runs CLI (SourceOnly)\n(e.g. mcli generateSources\n -sourceOutputDir my-sources)"] --> CLI["mcli SourceOnlyMain"]
  CLI --> ReadPkg["Read package.json & node_modules"]
  ReadPkg --> Bootstrap["Bootstrap\n- locate TS stdlib (node_modules/typescript/lib)\n- scan node_modules for type sources"]
  Bootstrap --> Resolver["LibraryResolver\n- local path vs npm packages\n- moduleNameFor"]
  Resolver --> Pipe["SourceOnlyPipeline"]

  subgraph Phase1["Phase 1: Read TypeScript"]
    Parser["PersistingParser + TsLexer/TsParser\nparse .d.ts/.ts"] --> ResolveRefs["ResolveExternalReferences\n- imports/exports ⇒ modules"]
    ResolveRefs --> P1Xforms["Transforms (high-level):\nHandleCommonJsModules, RewriteExportStarAs, QualifyReferences,\nAugmentModules, ResolveTypeQueries, ReplaceExports,\nModuleAsGlobalNamespace, MoveGlobals, FlattenTrees,\nDefaultedTypeArguments, RejiggerIntersections, ExpandTypeParams,\nUnionTypesFromKeyOf, DropProperties, InferReturnTypes,\nRewriteTypeThis, InlineConstEnum, InlineTrivial,\nExtractInterfaces, ExtractClasses, ExpandCallables,\nSplitMethods, RemoveDifficultInheritance, VarToNamespace"]
  end

  Pipe --> Phase1
  Phase1 --> LibTs["LibTs (parsed, deps, version)"]

  subgraph Phase2["Phase 2: To Scala.js"]
    ImportStage["ImportTree / ImportType / ImportName"] --> ScalaXforms["Scala transforms:\nCleanupTrivial, ModulesCombine, CastConversion,\nRemoveDuplicateInheritance, Deduplicator, FakeLiterals,\nUnionToInheritance, LimitUnionLength, RemoveMultipleInheritance,\nCombineOverloads, FilterMemberOverrides, InferMemberOverrides, CompleteClass"]
  end

  LibTs --> Phase2
  Phase2 --> LibScalaJs["LibScalaJs (PackageTree)"]

  Flavour["PhaseFlavour (Normal / Slinky / Japgolly)"]
  LibScalaJs --> Flavour --> Printer["Printer (emit .scala)"]

  Printer --> Write["Write files → my-sources/<libName>/..."]
  Write --> Done["Done"]
```

### Notes
- The stdlib is sourced from `node_modules/typescript/lib` (ensure `typescript` is installed).
- For your root `package.json` including `"class-variance-authority": "0.7.1"`, the pipeline resolves that package and its type definitions before emission.


