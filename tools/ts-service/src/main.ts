async function main() {
  const { analyzeProject } = await import('./analyzer.ts')
  const cwd = process.cwd()
  const results = await analyzeProject(cwd)
  if (results.length === 0) {
    console.log('No dependencies found in package.json')
    return
  }
  for (const r of results) {
    if (!r.parseSummary || !r.typesAbsolutePath) {
      console.warn(`[skip] ${r.dependencyName} has no resolvable types or file not found`)
      continue
    }
    const rel = r.parseSummary.fileName.replace(cwd + '/', '')
    console.log(`${r.dependencyName}: parsed ${rel} with ${r.parseSummary.statements} top-level statements`)
  }
}

main().catch((err) => {
  console.error(err)
  process.exitCode = 1
})