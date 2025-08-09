import { readFile, stat } from 'node:fs/promises'

export async function readJsonFile<T>(absolutePath: string): Promise<T> {
  const content = await readFile(absolutePath, 'utf8')
  return JSON.parse(content) as T
}

export async function readTextFile(absolutePath: string): Promise<string> {
  return readFile(absolutePath, 'utf8')
}

export async function fileExists(absolutePath: string): Promise<boolean> {
  try {
    await stat(absolutePath)
    return true
  } catch {
    return false
  }
}


