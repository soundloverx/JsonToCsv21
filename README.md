# JsonToCsv21

A desktop app (JavaFX 21) that lets you convert JSON to CSV interactively.

- Load a JSON file
- Inspect the inferred JSON schema as a tree
- Drag fields into a CSV definition table (supports nested arrays)
- Preview the resulting CSV rows live
- Save the CSV output and/or save/load your column definition presets (.j2csv)
- Optional custom columns via literals or formulas (CURRENT_TIMESTAMP, CONCAT, FIND, JSON)
- Preferences for dark mode, snake_case column names, preview limits, and how to render nulls


## Requirements
- Java 21 (JDK 21)
- JavaFX 21.0.8
- Maven 3.9+

This project uses the `javafx-maven-plugin` for running the app from Maven and the Maven Shade plugin to assemble an uber-jar.


## Quick start

### Run from Maven (recommended)
```
mvn clean javafx:run
```

### Build
- Package the application:
  ```
  mvn clean package
  ```
  This produces an uber JAR under `target/` (e.g., `JsonToCsv21-1.0.0-uber.jar`).

### Run the shaded JAR
Depending on your environment, JavaFX native libraries may be required. If launching the uber JAR fails on your OS, prefer `mvn javafx:run` or run from your IDE with the VM options below.
```
java -jar target/JsonToCsv21-1.0.0-uber.jar
```

### Run from your IDE
If you run the `org.overb.jsontocsv.App` class directly, and your IDE does not automatically attach JavaFX, add VM options like:
```
--module-path path/to/javafx-sdk-21.0.8/lib --add-modules javafx.controls,javafx.fxml
```
On Windows, the `path/to/javafx-sdk-21.0.8/lib` might look like `C:\Tools\javafx-sdk-21.0.8\lib`.


## Using the app
1. File → Load JSON file...
   - The app accepts several JSON formats:
     - An array of objects: `[{...}, {...}]`
     - Multiple objects separated by `},{` (will be wrapped into an array automatically)
     - Newline-delimited JSON (one JSON object per line)
     - A single JSON object
2. Inspect the JSON schema (left tree).
3. Define CSV columns (right table):
   - Drag primitive fields from the schema tree to add default columns.
   - For nested JSON with arrays, drag the array node to the “Root” field at the top, so the app knows which array to expand into rows.
   - Double-click a column to edit it (type, name, source path/formula).
4. Preview updates automatically as you change definitions. Use “Refresh” if needed.
5. File → Save CSV... to export.
6. You can also File → Save/Load CSV definitions... to persist your column setup to a `.j2csv` file.

### Column types
- Default: reads values from a JSON path.
- Literal: a constant string you provide.
- Formula: a computed value using simple functions (see below).

### Formulas
Formulas have the shape `FUNCTION(arg1, arg2, ...)`. Supported functions:
- `CURRENT_TIMESTAMP()` → Current datetime formatted as `yyyy-MM-dd HH:mm:ss`.
- `CONCAT(a, b, 'literal', c, ...)` → Concatenates values from previous columns and/or literals.
- `FIND(valueColumn, path.to.lookupField, returnField)` → Scans a JSON node or array at the given path, matches `lookupField` against the value from `valueColumn`, and returns `returnField` from the same object as `lookupField`.
- `JSON(path)` → Returns the JSON value at `path` relative to the current array element (when inside a group) or the root record; for non-scalars it returns a JSON string.

Notes:
- If a function has the wrong number of arguments or an unknown name, an error marker will be placed in the cell during preview.
- When editing a formula, single-quote string literals: `'literal text'`.

### Preferences
Open File → Preferences... to control:
- Auto-convert on load (tries to infer columns for simple arrays of flat objects)
- snake_case column names
- Preview row limit
- CSV null style: Empty vs literal `null`
- Dark mode

### Keyboard shortcuts
- Add column definition... → F4
- Refresh preview → F5


## Tips for nested JSON
- Set the Root to the array you want to export as rows (drag the array node onto the Root field).
- You can still include values from higher up in the JSON via their paths.
- When multiple arrays are involved, the app expands rows combinatorially across groups; use formulas to compute per-element values where needed.


## Building blocks (under the hood)
- Schema inference: `JsonSchemaService` builds a tree from your JSON.
- Row expansion: `CsvRowExpander` handles nested arrays and formulas to produce rows.
- JSON I/O: `JsonIo` accepts arrays, multi-object strings, and JSONL.
- Preferences: stored as JSON in a per-user config file.


## Troubleshooting
- If you see JavaFX errors when running from your IDE, ensure the JavaFX SDK path is correct in VM options or use `mvn javafx:run`.
- On Linux or macOS, make sure you’re using an OS build of JavaFX that matches your platform and JDK architecture.
- If the preview says “No rows”, verify the Root field points to the correct array when working with nested JSON.


## License
This project is provided as-is. See the repository’s license file if present.