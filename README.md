### Overview
Json2Csv is a desktop application built with JavaFX 21 that lets you interactively convert JSON to CSV.

- Load a JSON file (supports .json and .json.gz) or drag a file into the window
- Inspect the inferred JSON schema in a tree view, with instant search/filter
- Drag fields into a CSV definition table (supports nested arrays); drag to reorder columns
- Live CSV preview as you change definitions
- Export CSV and save/load your column definition presets (.j2csv); drag a .j2csv file onto the table to load
- Custom columns via literals and formulas (CURRENT_TIMESTAMP, CONCAT, FIND, JSON)
- Preferences: dark mode, snake_case column names, preview limits, and how to render nulls
- Optional update check from an endpoint configured in resources
- Recent files menu for quick access to JSON or .j2csv files
- Auto-detects a likely JSON root array for nested data and suggests it automatically


### Requirements
- Java 21 (JDK 21)
- JavaFX 21.0.8
- Maven 3.9+

This project uses:
- javafx-maven-plugin to run the app via Maven
- maven-jar-plugin for the main JAR and maven-dependency-plugin to copy runtime dependencies to target/lib


### Quick start

#### Run from Maven (recommended)
```
mvn clean javafx:run
```

#### Build
```
mvn clean package
```
Produces:
- target\json2csv.jar (the app)
- target\lib\* (all runtime dependencies, including JavaFX artifacts)

#### Run the built app without Maven
Because dependencies are copied to target\lib, run with the classpath including both the app JAR and the lib directory.

- Windows (PowerShell):
```
java -cp "target\json2csv.jar;target\lib\*" org.overb.jsontocsv.App
```
- Linux/macOS:
```
java -cp "target/json2csv.jar:target/lib/*" org.overb.jsontocsv.App
```
If you encounter JavaFX native library issues on your platform, prefer running via Maven (javafx:run) or use the packaged runtime/installer described below.

#### Run from your IDE
Run the main class:
- Main class: org.overb.jsontocsv.App
  Most IDEs will resolve JavaFX from Maven automatically. If not, ensure your run configuration uses the project’s Maven classpath.


### Using the app
1. File → Load JSON file...
    - Supported formats:
        - JSON array of objects: `[{...}, {...}]`
        - Multiple objects separated by `},{` (auto-wrapped into an array)
        - Newline-delimited JSON (JSONL; one object per line)
        - Single JSON object
        - Gzipped JSON files: .json.gz
2. Inspect the JSON schema (left tree).
3. Define CSV columns (right table):
    - Drag primitive fields from the schema tree to add default columns.
    - For nested JSON with arrays, drag the array node onto the “Root” field to tell the app which array expands into rows.
    - Double-click a column to edit it (type, name, source path/formula).
4. Preview updates automatically as you change definitions. Use “Refresh” if needed.
5. File → Save CSV... to export.
6. File → Save/Load CSV definitions... to persist your column setup to a .j2csv file.

#### Column types
- Default: reads values from a JSON path.
- Literal: a constant string you provide.
- Formula: a computed value using functions below.

#### Formulas
Formulas look like `FUNCTION(arg1, arg2, ...)`. Supported functions:
- CURRENT_TIMESTAMP() → Current datetime formatted as `yyyy-MM-dd HH:mm:ss`.
- CONCAT(a, b, 'literal', c, ...) → Concatenates values from previous columns and/or quoted literals.
- FIND(valueColumn, path.to.lookupField, returnField) → Scans a JSON node or array at the given path, matches lookupField against the value from valueColumn, and returns returnField from the matching object.
- JSON(path) → Returns the JSON value at path relative to the current array element (when inside a group) or the root record; non-scalars return JSON as text.

Notes:
- If a function name is unknown or the number of arguments is wrong, an error marker is shown in the preview cell.
- Use single quotes for string literals in formulas: `'some text'`.

#### Preferences
File → Preferences...
- Auto-convert on load (infer columns for simple arrays of flat objects)
- snake_case column names
- Preview row limit
- CSV null style: Empty vs literal `null`
- Dark mode

#### Keyboard shortcuts
- Add column definition... → F4
- Refresh preview → F5
- Delete selected columns → Del
- Undo last delete → Ctrl+Z
- Redo delete → Ctrl+Y


### Tips for nested JSON
- Set the Root to the array you want to export as rows (drag the array node onto the Root field).
- You can still include values from higher levels of the JSON via their paths.
- When multiple arrays are involved, the app performs a combinatorial expansion across groups; formulas can compute per-element values where needed.


### Building blocks (under the hood)
- Schema inference: JsonSchemaService builds a tree from your JSON.
- Row expansion: CsvRowExpander handles nested arrays and formulas to produce rows.
- JSON I/O: JsonIo accepts arrays, multi-object strings, JSONL, and gzipped JSON.
- Preferences: stored as JSON in a per-user config file.


### Packaging a Windows MSI installer
This repo includes msibuilder.cmd which uses jdeps, jlink, and jpackage to build a trimmed runtime image and an MSI installer.

Prerequisites:
- Windows host with JDK 21 installed (with jlink and jpackage available)
- JAVA_HOME set (e.g., `C:\Program Files\Java\jdk-21\`)
- JavaFX jmods installed and FXJMODS pointing to its jmods directory (e.g., `C:\javafx-jmods-21.0.8\`)
- Project built (`mvn clean package`) so that target\json2csv.jar and target\lib\* exist

Run:
```
msibuilder.cmd
```
What it does:
- Extracts project.version from pom.xml
- Uses jdeps to calculate required modules
- jlinks a minimal runtime to build\runtime
- Copies app JAR and libs to a staging folder
- Uses jpackage to create an MSI named Json2Csv-<version>.msi

The MSI installs a self-contained app with a bundled Java runtime, start menu entry, and desktop shortcut.


### Version and updates
- App version is sourced from src/main/resources/app.properties: `version=${project.version}` (resolved at build time from pom.xml, currently `1.0.3`).
- Update check URL is configurable in the same file: `update=<url>`.


### Troubleshooting
- JavaFX errors when running from an IDE or the standalone JAR: ensure the runtime can find JavaFX native libraries. Prefer `mvn javafx:run` or the MSI installer on Windows if you encounter issues.
- If the preview says “No rows”, ensure the Root field points to the correct array for nested JSON.
- CSV shows empty values: verify column JSON paths and formula arguments.


### License
This project is provided as-is. See the repository’s license file if present.