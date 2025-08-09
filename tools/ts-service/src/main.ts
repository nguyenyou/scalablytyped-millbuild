import { readFile, stat } from 'node:fs/promises'
import path from 'node:path'
import ts from 'typescript'

type PackageJson = {
  name?: string
  version?: string
  dependencies?: Record<string, string>
  devDependencies?: Record<string, string>
  types?: string
  typings?: string
  exports?: unknown
}

async function readJsonFile<T>(absolutePath: string): Promise<T> {
  const content = await readFile(absolutePath, 'utf8')
  return JSON.parse(content) as T
}

async function fileExists(absolutePath: string): Promise<boolean> {
  try {
    await stat(absolutePath)
    return true
  } catch {
    return false
  }
}

function resolveIfRelative(baseDir: string, maybeRelative: string): string {
  return path.isAbsolute(maybeRelative)
    ? maybeRelative
    : path.resolve(baseDir, maybeRelative)
}

function tryExtractTypesFromExports(exportsField: unknown): string | undefined {
  // Best-effort probe for common shapes:
  // - "exports": { ".": { "types": "./index.d.ts" } }
  // - "exports": { "types": "./index.d.ts" }
  // - "exports": { ".": { "default": { "types": "./index.d.ts" } } }
  if (!exportsField || typeof exportsField !== 'object') return undefined

  const root = exportsField as Record<string, unknown>

  const probe = (node: unknown): string | undefined => {
    if (!node || typeof node !== 'object') return undefined
    const obj = node as Record<string, unknown>
    // Prefer ESM condition first if present at this node
    if (obj.import && typeof obj.import === 'object') {
      const im = obj.import as Record<string, unknown>
      if (typeof im.types === 'string') return im.types
      if (typeof im.typings === 'string') return im.typings
    }
    if (typeof obj.types === 'string') return obj.types
    if (typeof obj.typings === 'string') return obj.typings
    // Some packages nest under "default"
    if (obj.default && typeof obj.default === 'object') {
      const nested = obj.default as Record<string, unknown>
      if (typeof nested.types === 'string') return nested.types
      if (typeof nested.typings === 'string') return nested.typings
    }
    return undefined
  }

  // Direct types at top-level exports
  const direct = probe(root)
  if (direct) return direct

  // Common case: under "."
  if (root['.']) {
    const dot = probe(root['.'])
    if (dot) return dot
  }

  return undefined
}

function getTypesEntryAbsolutePath(pkgJson: PackageJson, packageDir: string): string | undefined {
  // Prefer exports mapping when present, and within it prefer ESM ("import")
  const fromExports = tryExtractTypesFromExports(pkgJson.exports)
  if (fromExports) {
    return resolveIfRelative(packageDir, fromExports)
  }

  // Fallback to package-level types/typings
  const candidate = pkgJson.types || pkgJson.typings
  if (typeof candidate === 'string' && candidate.length > 0) {
    return resolveIfRelative(packageDir, candidate)
  }

  return undefined
}

async function parseTypesFile(absolutePath: string): Promise<{ statements: number; fileName: string } | undefined> {
  const exists = await fileExists(absolutePath)
  if (!exists) return undefined
  const sourceText = await readFile(absolutePath, 'utf8')
  const sourceFile = ts.createSourceFile(absolutePath, sourceText, ts.ScriptTarget.Latest, true)
  return { statements: sourceFile.statements.length, fileName: sourceFile.fileName }
}

async function main() {
  const cwd = process.cwd()
  const projectPkgPath = path.resolve(cwd, 'package.json')
  const projectPkg = await readJsonFile<PackageJson>(projectPkgPath)

  const dependencies = Object.keys(projectPkg.dependencies ?? {})
  if (dependencies.length === 0) {
    console.log('No dependencies found in package.json')
    return
  }

  for (const depName of dependencies) {
    const depPkgPath = path.resolve(cwd, 'node_modules', depName, 'package.json')
    const exists = await fileExists(depPkgPath)
    if (!exists) {
      console.warn(`[skip] Missing package.json for ${depName}`)
      continue
    }

    const depPkg = await readJsonFile<PackageJson>(depPkgPath)
    const depDir = path.dirname(depPkgPath)
    const typesAbs = getTypesEntryAbsolutePath(depPkg, depDir)

    if (!typesAbs) {
      console.warn(`[skip] ${depName} has no types/typings entry`)
      continue
    }

    const parsed = await parseTypesFile(typesAbs)
    if (!parsed) {
      console.warn(`[skip] ${depName} types file not found: ${path.relative(cwd, typesAbs)}`)
      continue
    }

    console.log(`${depName}: parsed ${path.relative(cwd, parsed.fileName)} with ${parsed.statements} top-level statements`)
  }
}

main().catch((err) => {
  console.error(err)
  process.exitCode = 1
})