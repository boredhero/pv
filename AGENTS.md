# Vinum development

Minecraft 1.21.1, NeoForge 21.1.262, Java 21; Gradle wrapper and ModDevGradle.
Work on `port/1.21.1-neoforge`; keep version 0.3.0 unless Noah requests a change.
Preserve unrelated and partially completed working-tree changes.

## Relevant context

- `README.md`: player workflows, aging defaults, installation, CI, and extension points. Read the relevant section when needed.
- `src/main/java/io/vinum/`: gameplay code. `barrel/` defines beverages and aging; `tileentity/` owns persisted storage/machines; `multiblock/` owns assembly; `client/` owns rendering.
- `src/main/resources/`: models, translations, recipes, loot, and world generation. Use Minecraft 1.21.1 singular data directory names.
- `src/gametest/`: server integration tests, excluded from the released JAR.

## Architecture

- Prefer small cohesive classes and composition. Keep process rules separate from vessel storage, assembly, rendering, and player interaction. Introduce abstractions for concrete extension needs, not speculative frameworks.
- Keep beverage/process definitions extensible; avoid drink-name switches in generic vessels. Document units, persistence contracts, and non-obvious invariants in code. Document new extension points with an example.
- Preserve fluid quantity, ingredient proportions, alcohol, and metadata through transfer, save/load, split/merge, and item conversion. Simulation must not mutate storage. Fluid components must describe per-unit properties; extensive batch totals must scale with transferred volume.
- Keep fermentation progress separate from maturation age. Preserve registry IDs and legacy saves where supported; make compatibility paths explicit.
- Bound topology searches, avoid forced chunk loads, and keep server mutations separate from client rendering. Coordinate shared API changes before parallel implementation.

## Verification and handoff

- Noah performs interactive in-game testing. Supply the built JAR path and a short checklist with setup, actions, and expected results. Do not claim visual/gameplay verification based only on compilation or server tests.
- Java changes: compile first; run affected behavioral tests and the full server GameTest suite before handing off a substantial gameplay change.
- Resource changes: run `python scripts/check_resources.py` and relevant behavioral checks.
- Package: `./gradlew build`. Server integration: `./gradlew runGameTestServer --console=plain`. `build` alone does not run the GameTests.
- Keep full logs under ignored `build/`; return the result, failing test names, and relevant error excerpts. Avoid repeated successful builds and full log dumps.
- Do not modify classes/resources underneath Noah's running client. Ask him to close/restart it before builds when necessary.
- Before a requested push, check the staged diff and whitespace; report the commit, branch, checks, and manual testing still needed. Do not stage unrelated work.
