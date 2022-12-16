/*
 This script defines the database schema for the DependencyAnalyzer tool.
 codegen/JooqCodeGenerator.java uses this script to generate Java source code
 modeling the schema.
 */

-- foreign keys are deactivated by default in SQLite, so we need to activate
PRAGMA FOREIGN_KEYS = ON;

/*
 Stores information about a bazel target.
 */
CREATE TABLE bazel_target (
    target_id INTEGER PRIMARY KEY AUTOINCREMENT,
    target_label TEXT NOT NULL UNIQUE
);

/*
 Stores a dependency edge between two Bazel targets where the target with id
 from_target_id depends on the target with id to_target_id.
 */
CREATE TABLE bazel_edge (
    from_target_id INTEGER,
    to_target_id INTEGER,
    FOREIGN KEY (from_target_id)
        REFERENCES bazel_target,
    FOREIGN KEY (to_target_id)
        REFERENCES bazel_target,
    PRIMARY KEY (from_target_id, to_target_id)
);

/*
 Stores an export edge between two Bazel targets where the target with id
 exporter_target_id exports the target with id exported_target_id.
 */
CREATE TABLE bazel_export_edge (
    exporter_target_id INTEGER,
    exported_target_id INTEGER,
    FOREIGN KEY (exporter_target_id)
       REFERENCES bazel_target,
    FOREIGN KEY (exported_target_id)
       REFERENCES bazel_target,
    PRIMARY KEY (exporter_target_id, exported_target_id)
);

/*
 Stores a Java source file path.
 */
CREATE TABLE java_file (
    file_id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_path TEXT NOT NULL UNIQUE
);

/*
 Stores a symbol with a fully qualified name (eg. com.stripe.foo.Bar.getVal)
 */
CREATE TABLE symbol (
    symbol_id INTEGER PRIMARY KEY AUTOINCREMENT,
    fully_qualified_name TEXT NOT NULL UNIQUE
);

/*
 Stores the many to many mapping from Java source files to Bazel targets.
 */
CREATE TABLE java_file_bazel_target (
    target_id INTEGER,
    file_id INTEGER,
    FOREIGN KEY (target_id)
        REFERENCES bazel_target,
    FOREIGN KEY (file_id)
        REFERENCES java_file,
    PRIMARY KEY (target_id, file_id)
);

/*
 Stores the many to many mapping between Java source files and exported symbols.
 */
CREATE TABLE java_file_exported_symbol (
    file_id INTEGER,
    symbol_id INTEGER,
    FOREIGN KEY (file_id)
        REFERENCES java_file,
    FOREIGN KEY (symbol_id)
        REFERENCES symbol,
    PRIMARY KEY (file_id, symbol_id)
);

/*
 Stores the many to many mapping between Java source files and imported symbols.
 */
CREATE TABLE java_file_imported_symbol (
    file_id INTEGER,
    symbol_id INTEGER,
    FOREIGN KEY (file_id)
        REFERENCES java_file,
    FOREIGN KEY (symbol_id)
        REFERENCES symbol,
    PRIMARY KEY (file_id, symbol_id)
);
