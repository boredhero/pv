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

The wine prototype contributes all six grape varieties, seeds, juices, bottles,
glasses, seven decorative wine barrels, lead ore and metal, three falling sands,
and two translucent glass blocks. Its grape-growing and wine-fermentation systems
were unfinished in the original prototype and are not advertised as implemented
here. Wine content is accessible from the unified Project Vinum creative tab.

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
The asset validator checks JSON, references, recipes, models, and textures; when
Minecraft and NeoForge artifacts are cached it also resolves their resources.

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
