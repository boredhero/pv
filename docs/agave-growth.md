# Blue agave growth and harvest

Agave has eight stages (`age=0..7`). Each successful bonemeal use advances one
stage; natural growth requires light level 9 and succeeds on one in seven random
ticks. Sand, red sand, and cultivated farmland remain valid substrates.

Stages 0–3 fit in one block. Later stages expand into a bounded set of leaf cells;
the mature rosette spans a 3×3 footprint and three blocks of height. Its eighteen
occupied cells leave empty corners available rather than reserving an invisible
solid 3×3×3 cube. Plant centers should be at least three blocks apart, with room
above them. Wild plants begin at stage 3 and grow normally after generation.
The heart and leaves have collision following their growth-stage geometry;
empty corners remain passable. Cutting a leaf immediately clears its collision,
while the exposed piña stays solid until harvested.

## Cut the leaves, then harvest the piña

At stage 7, mine the surrounding leaf sections, or right-click each section with
an axe or shears. Right-click cutting costs one tool durability and respects block
break-event cancellation. Cutting yields no piña or seeds. The plant stays rooted,
and mature cut leaves do not regrow. The missing leaf blocks persist normally in
world saves, so partially harvesting a plant is safe across sessions.

The central heart cannot be mined normally while leaves remain. After the last
owned leaf section is cut, the root's persisted `stripped=true` state exposes its
cream-and-green diamond-scarred piña model. Break that heart to obtain the existing
`vinum:agave` ingredient and seeds. The produce loot requires both `age=7` and
`stripped=true`; force-removing an unstripped root cannot bypass the harvest step.
Creative removal of a leaf removes the whole plant without harvest drops.
Younger plants return seeds, and removing their root or a leaf clears the plant.

Cook the harvested piña into `cooked_agave_pulp`, then extract it in the Press:
one pulp yields 1,000 mB of agave mosto after eight crank turns or 2,000 FE.
Collect the liquid in a carboy, ferment it in a vessel, and distill the wash into tequila.
The harvest uses the existing processing ingredient, so the cooking, wort,
fermentation, and distillation recipes remain connected.

## Growth and implementation

The next stage is checked completely before any blocks change. An obstruction or
unloaded neighboring chunk pauses growth; bonemeal is not accepted when the next
stage cannot fit. Other plants, fluids, and player builds are never overwritten.
Every leaf cell stores its root offset and stage in its block state, requiring no
block entity, unbounded search, or forced chunk load. Leaf cells validate their
root on random ticks, clearing orphans after a previously unloaded neighboring
chunk becomes available. Piña exposure is deferred while a relevant neighboring
chunk is unavailable, so unloading never counts as cutting leaves.

`scripts/generate_agave_models.py` produces the native cuboid rosette and tapered
heart models, blockstates, and `AgaveGeometry.java` from the same tapered radial
leaf geometry. Generated sparse occupancy and selection bounds stay synchronized
with the models. `AgaveColors` supplies waxy blue-green leaf face colors. The piña
surface uses `textures/block/agave_pina.png`, matching the harvested item artwork.

Client review: grow a plant with seven bonemeal uses in a clear area, inspect the
splayed outer blades and upright center, then cut the leaves one section at a
time. Confirm that the root stays until the last cut exposes the piña. Harvest it
and follow the existing cooking and production chain. Server tests cover stages,
obstruction atomicity, sparse footprint, staged harvest, tool wear, no regrowth,
serialized exposed state, creative removal, and support cleanup; appearance still
requires in-game review.
