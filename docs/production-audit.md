# Production and architecture audit — 2026-11-02

Scope: Survival acquisition, recipe reachability, ingredient and container
remainders, fluid metadata, processing, aging, and serving for the current six
grapes and tequila. Compilation alone is not evidence that a production path is
playable; the regression suite exercises world interactions and machine outputs.

## Current production paths

| Product | Reachable path | Important conditions |
| --- | --- | --- |
| Wine from juice | Wild seeds → trellis → ripe grapes → powered press → juice → carboy/vat fermentation → wine | Press requires a crank or powered motor. Optional yeast accelerates fermentation. |
| Wine from must | Grapes + juicer + bowl → must → vat fermentation → separate strainer → wine | Skins remain until clarification. Bulk containers can carry the cloudy batch; serving glasses reject solids. |
| Wine blend | Finished wines → shared container or barrel → volume-weighted blend | Mixing preserves proportions and alcohol; incompatible extra metadata prevents mixing. |
| Aged wine | Finished wine/blend → assembled oak barrel → reusable bottle/glass | Fermentation progress and maturation age are distinct. Bottling preserves age and composition. |
| Tequila | Mature agave → strip leaves → piña → furnace cooking → cooked pulp → powered press → mosto → fermentation → wash → still → spirit | 3 L of 10% wash produces 750 mL at 40% ABV in 1,200 heated ticks. |
| Aged tequila | Spirit → oak barrel → reposado/añejo/extra añejo → fifth bottle or shot glass | Age controls the presentation and maturation stage. It is not a second fluid identity. |

The core machine, support, tool, yeast, steel, barrel, bottle, and glass recipes
exist. All six grapes have pressing, must preparation, juice fermentation, and
must fermentation recipes. A batch bucket is created by drawing fluid into an
ordinary bucket; it does not need its own crafting recipe.

## Confirmed defects addressed

- **Canonical wine aging display:** newly produced universal wine and blends were
  absent from the beverage aging catalog. Bottles therefore omitted age/stage
  tooltips even though the barrel stored the age. Canonical wine now has an aging
  definition, including blend support and boundary tests.
- **Wine names in fermenters:** vessel status used the generic fluid name instead
  of the batch's variety or blend name. It now uses the shared wine naming logic.
- **Unusable servings:** drinking glasses accepted skin-bearing must/wine but
  silently refused drinking. Serving acceptance now rejects solids; bulk
  containers retain the ability to transport them for straining.
- **Still intake:** the UI only understood old wort items and batch buckets.
  Reusable fluid containers now load wash and retain undrained remainders.
- **Unsafe distillation definitions:** an oversized or unsuitable output item
  could receive a hidden fluid component and lose usable output. Recipes now
  validate the actual container, fluid acceptance, and capacity before work.
  The selected recipe determines the required empty container.
  Changing the wash or container cancels an incompatible saved plan and its
  progress, so the machine can select a valid replacement rather than getting stuck.
- **Legacy distillation bypass:** the obsolete fermented-wort item offered a
  faster path unrelated to the real wash recipe. That path and its misleading
  Creative entry are removed; tests use actual fermented wash.
- **Partial still structures:** a loaded controller could keep processing while
  another structure chunk was unavailable. Processing now pauses without loading
  those chunks.
- **Duplicated grape defaults:** prepared-input chemistry repeated six names
  separately from `vinum/wines.json`. Adapters now derive ingredient identity
  from the variety catalog, with tests for sugar, solids, and provenance.
- **Consumed containers/tools:** cinnamon crafting and salt smelting consumed
  shears and iron buckets respectively. Cinnamon now uses a reusable tool recipe;
  salt uses a heated steel pot that returns the bucket at filling. Apple juice
  returns its glass bottle as a crafting remainder.
- **Unreachable materials:** lead processing, powder preparation, sand washing
  and mixtures, and the two special glasses now have crafting/cooking paths.
  These are intentionally compact game abstractions, not complete metallurgy.

## Remaining design gaps

These are still incomplete features, not implied to be solved by this audit.

1. **Closure depth:** wine bottling now consumes a cork on the first successful
   fill, with prepared bottles for automation and no extra charge for top-ups.
   Explicit uncorking, reusable closures, and seal-dependent storage remain
   unimplemented.
2. **Pre-fermentation blending:** finished wines blend; different juice fluids
   cannot currently co-ferment in one tank. A universal must/juice representation
   would need recipe selection based on batch composition.
3. **Grain mashing:** `MashingRecipe` and `MashingEngine` are tested starch-conversion
   infrastructure, with no machine caller or shipped grain recipes. Agave
   cooking uses a furnace; extraction now uses the shared Press. The old
   pulp-and-water crafting shortcut has been removed.
4. **Distillation depth:** no cuts, multiple passes, cooling-water requirement, or
   recoverable stillage. The existing recipe conserves output alcohol but
   discards the remainder of the consumed wash.
5. **Maturation depth:** time changes names, labels, and maturity stages. It does
   not yet model wood extraction, flavor, oxidation, quality, or evaporation.
6. **Variety registration:** colors and prepared-input defaults share a catalog,
   but a new grape still needs item/block/fluid registrations and production
   recipes. `wines.json` is a packaged startup catalog, not a reloadable registry.
   A colors-only entry cannot create an entire new crop and production chain.
7. **Brine/source detail:** salt evaporation retains the existing water input
   abstraction. Salinity, seawater collection, and brine concentration are not
   simulated.

## Extension boundaries

Keep composition and quantities on `FluidStack`/`BatchProfile`; keep processing
clocks on machines, aging on stored batches, and bottle artwork in the appearance
catalog. New spirits can supply fermentation/distillation recipes and presentation
definitions, but still require registered fluids and any desired aging definition.
Distillation outputs must be supported reusable containers and fit their capacity.
Custom recipes must preserve real tool/container remainders and reject incomplete
inputs before consuming anything.

Validation after the unified carboy, airlock, cork bottling, and cellar integration:
all 151 server GameTests passed. The resource checker validated 688 JSON resources
and 86 recipes. An isolated client reached the welcome screen without Vinum model
loading errors after removing an unregistered placement helper item. In-game
visual review remains for fuller vines, flowering clusters, shelves, and airlocks.
Logs are under the ignored `build/logs/` directory.

Future work should prioritize universal pre-fermentation batches
before adding many more drink-specific items. That addresses the current
expansion pressure without replacing stable storage and transaction code.
