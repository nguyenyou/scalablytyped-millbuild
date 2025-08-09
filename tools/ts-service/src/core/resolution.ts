import ts from 'typescript'
import path from 'node:path'

export type PackageJson = {
  name?: string
  version?: string
  dependencies?: Record<string, string>
  devDependencies?: Record<string, string>
  types?: string
  typings?: string
  exports?: unknown
}

export type TypesResolutionSource = 'exports.import' | 'exports.default' | 'exports.root' | 'types' | 'typings'

export type TypesResolutionResult = {
  absolutePath: string
  relativePath: string
  source: TypesResolutionSource
  isEsmPreferred: boolean
}

type ExportProbeResult = { relativePath: string; source: Extract<TypesResolutionSource, 'exports.import' | 'exports.default' | 'exports.root'> }

export function listDependencies(pkg: PackageJson): string[] {
  return Object.keys(pkg.dependencies ?? {})
}

export function resolveIfRelative(baseDir: string, maybeRelative: string): string {
  return path.isAbsolute(maybeRelative) ? maybeRelative : path.resolve(baseDir, maybeRelative)
}

export function tryExtractTypesFromExports(exportsField: unknown): ExportProbeResult | undefined {
  if (!exportsField || typeof exportsField !== 'object') return undefined
  const root = exportsField as Record<string, unknown>

  const probe = (node: unknown): ExportProbeResult | undefined => {
    if (!node || typeof node !== 'object') return undefined
    const obj = node as Record<string, unknown>
    if (obj.import && typeof obj.import === 'object') {
      const im = obj.import as Record<string, unknown>
      if (typeof im.types === 'string') return { relativePath: im.types, source: 'exports.import' }
      if (typeof im.typings === 'string') return { relativePath: im.typings, source: 'exports.import' }
    }
    if (typeof obj.types === 'string') return { relativePath: obj.types, source: 'exports.root' }
    if (typeof obj.typings === 'string') return { relativePath: obj.typings, source: 'exports.root' }
    if (obj.default && typeof obj.default === 'object') {
      const nested = obj.default as Record<string, unknown>
      if (typeof nested.types === 'string') return { relativePath: nested.types, source: 'exports.default' }
      if (typeof nested.typings === 'string') return { relativePath: nested.typings, source: 'exports.default' }
    }
    return undefined
  }

  const direct = probe(root)
  if (direct) return direct
  if (root['.']) {
    const dot = probe(root['.'])
    if (dot) return dot
  }
  return undefined
}

export function resolveTypesEntry(pkgJson: PackageJson, packageDir: string): TypesResolutionResult | undefined {
  const fromExports = tryExtractTypesFromExports(pkgJson.exports)
  if (fromExports) {
    const abs = resolveIfRelative(packageDir, fromExports.relativePath)
    return { absolutePath: abs, relativePath: fromExports.relativePath, source: fromExports.source, isEsmPreferred: fromExports.source === 'exports.import' }
  }
  const candidate = pkgJson.types || pkgJson.typings
  if (typeof candidate === 'string' && candidate.length > 0) {
    const abs = resolveIfRelative(packageDir, candidate)
    return { absolutePath: abs, relativePath: candidate, source: (pkgJson.types ? 'types' : 'typings') as TypesResolutionSource, isEsmPreferred: false }
  }
  return undefined
}

export function parseTypesTextToAst(sourceText: string, fileName: string): ts.SourceFile {
  return ts.createSourceFile(fileName, sourceText, ts.ScriptTarget.Latest, true)
}

export function summarizeSourceFile(sourceFile: ts.SourceFile): { statements: number; fileName: string } {
  return { statements: sourceFile.statements.length, fileName: sourceFile.fileName }
}


