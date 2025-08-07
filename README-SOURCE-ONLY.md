# ScalablyTyped Source-Only Generation for Mill

This document explains how to generate only Scala source files from TypeScript definitions without creating sbt project structures, specifically for use with Mill build tool.

## Overview

The `SourceOnlyMain` provides a Mill-compatible way to generate pure Scala source files from TypeScript definitions without any sbt project layout or compilation artifacts.

## Usage

### Basic Usage

From your project directory with `package.json` and `node_modules`:

```bash
# Using Mill task
mill mcli.generateSources

# Using Mill with specific output directory
mill mcli.generateSourcesTo generated-sources

# Using Mill with specific libraries
mill mcli.generateSourcesTo generated-sources react @types/react
```

### Manual Execution

You can also run the source generator directly:

```bash
# Generate sources for all dependencies in package.json
mill mcli.runMain org.scalablytyped.converter.cli.SourceOnlyMain

# Generate sources to specific directory
mill mcli.runMain org.scalablytyped.converter.cli.SourceOnlyMain -o ./my-sources

# Generate sources for specific libraries
mill mcli.runMain org.scalablytyped.converter.cli.SourceOnlyMain react @types/react lodash
```

### Command Line Options

- `-d, --directory <path>`: Directory containing package.json and node_modules (default: current directory)
- `-o, --output <path>`: Output directory for generated sources (default: ./generated-sources)
- `--includeDev`: Include dev dependencies from package.json
- `--includeProject`: Include current project TypeScript files
- `-f, --flavour <flavour>`: Output flavour (Normal, Slinky, SlinkyNative, ScalajsReact)
- `--outputPackage <package>`: Base package for generated code
- `--scala <version>`: Scala version (default: 2.13)
- `--scalajs <version>`: Scala.js version (default: 1.x)
- `--ignoredLibs <lib1,lib2>`: Libraries to ignore
- `-s, --stdlib <libs>`: TypeScript stdlib parts to include (e.g., es6,dom)

## Output Structure

The source-only generator produces a clean directory structure:

```
generated-sources/
├── react/
│   ├── mod.scala                    # React module definitions
│   ├── global.scala                 # Global React types
│   └── components/
│       └── ReactComponents.scala    # Component definitions
├── lodash/
│   ├── mod.scala                    # Lodash function definitions
│   └── collections/
│       └── Collections.scala        # Collection utilities
└── @types/
    └── node/
        ├── mod.scala                # Node.js type definitions
        └── fs/
            └── Fs.scala             # File system types
```

Each library gets its own subdirectory with:
- Pure `.scala` files (no `.sbt` files)
- Organized by TypeScript module structure
- Proper Scala package declarations
- Import statements for dependencies

## Integration with Mill

### Adding Generated Sources to Your Build

To use the generated sources in your Mill build:

```scala
// build.mill
import mill._, scalalib._

object myproject extends ScalaModule {
  def scalaVersion = "2.13.10"
  def moduleDeps = Seq(/* your other deps */)
  
  // Include generated sources
  def generatedSources = T.sources {
    os.walk(millSourcePath / "generated-sources")
      .filter(_.ext == "scala")
      .map(PathRef(_))
  }
  
  def sources = T.sources {
    super.sources() ++ generatedSources()
  }
}
```

### Automated Generation Task

You can create a Mill task that regenerates sources automatically:

```scala
// build.mill
object myproject extends ScalaModule {
  // ... other config ...
  
  def updateTypings() = T.command {
    // Clean existing generated sources
    if (os.exists(millSourcePath / "generated-sources")) {
      os.remove.all(millSourcePath / "generated-sources")
    }
    
    // Generate new sources
    mill.mcli.generateSourcesTo((millSourcePath / "generated-sources").toString)
  }
}
```

## Benefits

1. **Clean Output**: Only Scala source files, no build artifacts
2. **Mill Compatible**: Integrates naturally with Mill build processes
3. **Version Control Friendly**: Generated sources can be committed if desired
4. **Fast**: No compilation step, just source generation
5. **Flexible**: Can target specific libraries or include all dependencies

## Example Workflow

1. Add TypeScript dependencies to your `package.json`
2. Run `npm install` to populate `node_modules`
3. Generate Scala sources: `mill mcli.generateSources`
4. Include generated sources in your Mill build
5. Use the generated Scala.js bindings in your code

## Comparison with Full Converter

| Feature | Source-Only | Full Converter |
|---------|-------------|----------------|
| Output | `.scala` files only | Complete sbt projects with JARs |
| Build Tool | Mill, sbt, any | sbt only |
| Speed | Very fast | Slower (includes compilation) |
| Dependencies | Self-managed | Auto-managed via sbt/ivy |
| Flexibility | High | Medium |
| Complexity | Low | High |

The source-only approach is ideal when you want full control over your build process and prefer to manage dependencies through your existing build tool rather than through sbt project artifacts.
