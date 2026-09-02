# Second-Order SQLi Field Companion

Flags a SQL sink reading a `@Entity` field that's ALSO confirmed,
elsewhere in the project, to be written from unsanitized HTTP input.

## Why it exists

CWE-89, the second-order form: the tainted value passes through
persistent storage (a real database) between the write and the read.
Documented explicitly as the real blind spot of simple SAST tools --
"SAST tools excel at direct input-to-query flows but struggle when
malicious data is stored and later used in a different execution
context... most simple pattern-matching SAST tools fail because they
don't model the database as a taint propagation intermediary."
Checkmarx and Fortify (real, confirmed enterprise SAST) model this
exact pattern -- neither is free, an inline IDE plugin, or
Community-Edition-compatible. No dedicated Marketplace plugin found.

## Why built this way

- **Field-sensitive, not variable/call-graph-sensitive** -- every
  other mechanism in this catalog correlates by variable name, call
  graph, or grammar structure. Here, the write site and the read site
  can live in completely unrelated classes with NO call-graph
  relationship at all -- the field NAME is the only real link, since
  the actual "connection" between them is a database, invisible to
  PSI.
- **Two-phase whole-project analysis**: catalogs every `@Entity`
  class's `String` fields, then separately finds WRITE sites
  (`entity.setName(taintedParam)`/`entity.name = taintedParam` inside
  an HTTP endpoint method) and READ sites (`entity.getName()`/
  `entity.name` concatenated into a SQL sink) -- a field only gets
  flagged when BOTH a real write and a real read are confirmed for the
  exact same `(entityClass, fieldName)` pair.
- **Reuses this catalog's existing `sql-concatenation-companion`
  detection shape** for the sink side (`executeQuery`/
  `createNativeQuery`/etc. with `+` concatenation), applied to a new,
  deeper correlation this catalog's existing SQL plugin never attempts.

## v0.1 scope — stated honestly, not exhaustively

- The field name must be textually IDENTICAL between write and read --
  never resolves a field alias.
- Only `String`-typed fields of a class textually annotated `@Entity`.
- A write site is only recognized within an HTTP endpoint method's own
  body, tainted by that SAME method's own parameter (bare reference or
  one-hop concatenation) -- a value forwarded through an intermediate
  service method before reaching the setter is out of scope.
- A project with more than 2,000 `.java` files skips analysis
  entirely.

## Usage

Open a Java file with a SQL sink concatenating a `@Entity` getter,
where some OTHER file in the project sets that same field from an HTTP
parameter -- the sink call shows a warning.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
