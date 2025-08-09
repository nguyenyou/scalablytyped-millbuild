import path from 'node:path'
import { fileExists, readJsonFile, readTextFile } from './io/fs.ts'
import type { PackageJson } from './core/resolution.ts'
import { listDependencies, resolveTypesEntry, parseTypesTextToAst, summarizeSourceFile } from './core/resolution.ts'

export async function readProjectPackageJson(projectCwd: string): Promise<PackageJson> {
  const projectPkgPath = path.resolve(projectCwd, 'package.json')
  return readJsonFile<PackageJson>(projectPkgPath)
}

export async function getDependencyPackageJson(
  projectCwd: string,
  depName: string,
): Promise<{ pkg: PackageJson; packageDir: string; packageJsonPath: string } | undefined> {
  const depPkgPath = path.resolve(projectCwd, 'node_modules', depName, 'package.json')
  const exists = await fileExists(depPkgPath)
  if (!exists) return undefined
  const depPkg = await readJsonFile<PackageJson>(depPkgPath)
  const depDir = path.dirname(depPkgPath)
  return { pkg: depPkg, packageDir: depDir, packageJsonPath: depPkgPath }
}

export type DependencyAnalysis = {
  dependencyName: string
  packageJsonPath?: string
  typesAbsolutePath?: string
  typesRelativePath?: string
  isEsmPreferred?: boolean
  parseSummary?: { statements: number; fileName: string }
}

export async function analyzeDependency(projectCwd: string, depName: string): Promise<DependencyAnalysis> {
  const base: DependencyAnalysis = { dependencyName: depName }
  const meta = await getDependencyPackageJson(projectCwd, depName)
  if (!meta) return base
  base.packageJsonPath = meta.packageJsonPath

  const types = resolveTypesEntry(meta.pkg, meta.packageDir)
  if (!types) return base
  base.typesAbsolutePath = types.absolutePath
  base.typesRelativePath = types.relativePath
  base.isEsmPreferred = types.isEsmPreferred

  if (!(await fileExists(types.absolutePath))) return base
  const text = await readTextFile(types.absolutePath)
  const sf = parseTypesTextToAst(text, types.absolutePath)
  base.parseSummary = summarizeSourceFile(sf)
  return base
}

export async function analyzeProject(projectCwd: string): Promise<DependencyAnalysis[]> {
  const pkg = await readProjectPackageJson(projectCwd)
  const deps = listDependencies(pkg)
  const analyses = await Promise.all(deps.map((d) => analyzeDependency(projectCwd, d)))
  return analyses
}


