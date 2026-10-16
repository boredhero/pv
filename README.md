# Project Vinum — Minecraft 1.21.1

A unified NeoForge port of the released tequila mod from `boredhero/pv`
(`1.15.2-0.1.0-HOTFIX`) and the wine prototype from `boredhero/project-vinum`
(`401c3ee`). The mod ID and all content namespaces are now `vinum`.

## Requirements and installation

- Minecraft **1.21.1**
- NeoForge **21.1.262** (the version used for development and verification)
- Java **21**

Put the non-sources `vinum-0.3.0.jar` from `build/libs/` in the client's
and server's `mods` directories. CocoLib is no longer required. Use a new world
for this port; migration of saves from the old Minecraft releases is not provided.

## Included content

The tequila production chain includes wild agave, plantable seeds, cooked agave,
wort fermentation, the four-part steel pot still, silver and golden tequila,
shots, and the spiced apple margarita. Reusable tools and barrels survive crafting.
Drinks affect blood alcohol content (BAC), with the original progression of
benefits and penalties, a client HUD, and gradual recovery.

The wine content includes six grape varieties, seeds, juices, bottles, drinkable
glasses, lead ore and metal, three falling sands, and two translucent glass blocks.
Universal oak barrels now store, age, and dispense wine and spirits. Grape-growing
and grape-to-wine fermentation remain unfinished; bottled wines are currently
available from the creative tab for testing the maturation system.

## Universal oak barrels

Craft an **Oak Barrel** using the existing barrel recipe. Place barrels facing the
same direction: complete solid cubes automatically combine, from **1×1×1 through
9×9×9**, including even sizes. Partial additions remain separate until a larger
compatible cube is complete. Different fluids or facings cannot share a reservoir.
Each block contributes **225 L** by default: a 3×3×3 barrel holds 6,075 L and a
9×9×9 barrel holds 164,025 L. Larger connected builds never create a reservoir
larger than 9×9×9.

All sizes use curved oak staves, metal hoops, plank ends, crescent-shaped wooden rests, and a visible
tap. There are no content names painted on the barrel. Right-click with an empty
hand to inspect the fluid, volume, capacity, age, and dimensions.

![Oak barrels from single-block to 9×9×9 in Minecraft 1.21.1](docs/images/barrels-overview.png)

![Plank end, tap, and integrated wooden rests](docs/images/barrel-saddle-closeup.png)

- Pour a filled spirit or wine bottle into a barrel by right-clicking it.
- Tap with an empty fifth or wine bottle to draw **750 mL**, or with an empty wine
  glass to draw **125 mL**. A full serving must fit; partial transfers do not consume
  the container.
- Water/lava buckets and NeoForge fluid pipes also work. A reservoir stores one
  compatible fluid at a time; unknown fluids work without beverage-specific code.
- Breaking a block preserves that block's local share in the dropped barrel.
  Remaining blocks separate safely. Filled dropped barrels do not stack, and
  replacing one restores its liquid without claiming the former structure.

### Aging and maturation

`fluidContentAge` records elapsed **world game ticks**, independent of the day/night
clock. Aging continues while a barrel's chunk is unloaded if the server world is
running. It pauses while the world is closed and while contents are in a bottle
or a carried barrel item. Emptying a barrel resets its batch age. Combining batches
of the same fluid uses volume-weighted age; adding fresh liquid dilutes that age.

| Beverage | Default maturation stages |
| --- | --- |
| Tequila | Blanco; reposado at 60 game days; añejo at 365; extra añejo at 1,095 |
| All six wines | Young; mature at 180 game days; reserve at 730 |

Tapping tequila selects the appropriate bottle. Wine retains its variety and shows
its age and maturation stage in the bottle/glass tooltip. Aging does not increase
the drink's alcohol dose. The original golden-tequila recipe remains a separate
legacy pathway, distinct from this age-based system.

Capacity, maturation-day duration, and thresholds are configurable in each world's
`serverconfig/vinum-server.toml`. One maturation day defaults to 24,000 ticks
(20 minutes at 20 TPS). Tequila timing is inspired by the
[CRT's maturation categories](https://www.crt.org.mx/en/our-tequila/), with two months
approximated as 60 days. Wine milestones are gameplay defaults, not universal wine
quality rules. Vessel-size regulations, evaporation, oxidation, and fermentation
chemistry are not simulated yet. Third-party containers must preserve fluid
components to retain age; Vinum bottles/glasses and its vanilla-bucket interactions do.

### Extending beverages

Storage and assembly never branch on individual wine or liquor names:

- `BarrelBlockEntity` owns local fluid and age; its `BarrelContents` snapshot exposes
  fluid type, amount, capacity, age, and availability. Every part retains ownership
  of its saved liquid, so a controller is not a single point of data loss.
- `BarrelMultiblock` manages bounded, queued topology updates. Fluid transfers
  reject unavailable chunks rather than loading them or modifying a partial tank.
- `BeverageDefinition` supplies a fluid, an `AgingProfile`, and `Serving` definitions
  for empty containers, volumes, and filled variants. `BarrelBottling` handles all
  profiles through the same exact-volume transaction path.

To add another wine or spirit, register its fluid/items, then register one
`BeverageDefinition` with `BeverageCatalog.register(...)` during common setup
(`enqueueWork` when called from parallel mod loading). A profile can keep the same
item through all stages, like wine, or select different items, like tequila.
No barrel, multiblock, or renderer changes are needed. Fluid and item stacks carry
the shared `vinum:fluid_content_age` data component through transfers.

The seven legacy barrel registry IDs still load existing blocks, but now use the
same universal storage and unlabelled appearance. Only the canonical oak barrel is
offered in the creative tab.

## Tequila quick start

1. Find mature agave in deserts or badlands. Harvest it and replant seeds on sand
   or farmland; bone meal also works. Smelt agave into cooked agave pulp.
2. Blast iron ingots or nuggets into steel. Craft a steel brazier, two steel pots,
   and a steel coil. Place them in a two-by-two vertical arrangement, facing the
   same direction: pot above brazier on the left, coil above pot on the right.
   The still assembles automatically when the last component is placed.
3. Craft cooked agave pulp with a water bucket to make unfermented wort. Craft
   that wort with the reusable barrel item to ferment it.
4. Open the still and insert fermented wort, an empty fifth bottle, and furnace
   fuel. A bottle of silver tequila takes **240 active ticks** (12 seconds).
5. Craft silver tequila with a reusable barrel to make golden tequila. Combine a
   fifth with a pack of shot glasses for drinkable shots. Craft the margarita with
   two golden tequila shots, apple juice, salt, cinnamon, honey, ice, a margarita
   glass, and a reusable shaker.

The still supports hoppers and NeoForge item handlers: inputs from above, fuel
from the sides, and finished bottles/spent fuel containers from below. Breaking
any part dismantles the structure and drops its inventory once.

The [original tutorial](https://github.com/boredhero/pv/blob/tutorials/src-tutorial/versions/alpha-0.1.0-mmdjam2020/alpha-0.1.0-mmdjam2020.md)
illustrates the recipes and explains BAC. The port fixes several bugs in that
release, including fuel-free progress, invalid output replacement, inconsistent
multiblock orientation, shared item properties, and lost drinking containers.

## Development and checks

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runGameTestServer
python scripts/check_resources.py
```

The wrapper pins Gradle 9.2.1 with a distribution checksum; ModDevGradle 2.0.148
uses Java 21. Runtime data lives in ignored `run/` and `run-gametest/` directories.
GameTests live separately in `src/gametest/` and are excluded from the released jar.
The asset validator checks JSON, references, recipes, models, and textures. Pass
`--minecraft-jar` and `--neoforge-jar` paths to also validate external resources.

Keep changes on a development branch and submit them through a pull request.
Do not commit directly to the upstream `master` branch.

## Continuous integration and downloads

GitHub Actions runs on every branch push and can also be started manually:

| Branch | Checks | Downloadable artifacts |
| --- | --- | --- |
| Any branch except `develop` and `master` | Build, resource validation, Gradle checks, and all server GameTests | Test reports and logs (14 days), including failed runs |
| `develop` or `master` | Build and resource validation | Mod and sources JARs (90 days), after a successful build |

Open **Actions → Build and test → a successful develop/master run → Artifacts**
to download the JAR bundle. Install the JAR without the `-sources` suffix.
Artifact names include the branch and commit SHA to identify each build.

The workflow uses Java 21 and the Gradle wrapper, with GitHub's cache backing
Gradle distributions, dependencies, build outputs, and NeoForm's Minecraft data.
Each branch can save its cache for subsequent pushes. JARs are GitHub Actions
artifacts; the workflow does not create GitHub Releases.

## Credits and license

Project Vinum: Noah Martino (boredhero), Rellit, and Yooks.
Wine prototype: Noah Martino, with credits to Cadiboo's modding tutorial.
The tequila code retains [GPLv3](LICENSE.md); the imported wine prototype retains
its [original license notice](LICENSE-WINE-PROTOTYPE.md). Both notices are included
in the distributed jar.

The new `assets/vinum/textures/block/barrel_oak.png` was generated with the built-in
imagegen tool using the prompt "seamless Minecraft pixel-art oak, vertical grain,
limited warm brown palette, no seams, metal, labels, or lighting," then finalized
at 64×64 using nearest-neighbor scaling. All barrel geometry is implemented in code.
