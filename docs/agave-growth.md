# Blue agave growth and harvest

Agave has eight stages (`age=0..7`). Each successful bonemeal use advances one
stage; natural growth requires light level 9 and succeeds on one in seven random
ticks. Sand, red sand, and cultivated farmland remain valid substrates.

Stages 0–3 fit in one block. Later stages expand into a bounded set of leaf cells;
the mature rosette spans a 3×3 footprint and three blocks of height. Its eighteen
occupied cells leave empty corners available rather than reserving an invisible
solid 3×3×3 cube. Plant centers should be at least three blocks apart, with room
above them. Wild colonies have a fully mature stage-7 center, partly grown plants
nearby, and young pups around their outer edges. Player-planted seeds still begin
at stage 0. Each plant's full footprint is checked before placement; obstructions
are never replaced and no partial rosettes are left behind.

Wild colonies have a 1-in-16 placement chance per eligible desert/badlands chunk.
Each colony attempts five plants: the mature center (stage 7), two intermediate
plants (stages 3–6) three blocks out, and two small outer pups (stages 0–2) five
blocks out. Positions are randomized around the center, with at least three
blocks between plant centers and clearance for their eventual mature forms.
Terrain and obstructions can reduce the final count.
The rarity is datapack-driven in `placed_feature/patch_agave.json`; existing
chunks are unchanged. Clustering is inspired by the vegetative offsets described
by the [Arizona-Sonora Desert Museum](https://desertmuseum.org/books/nhsd_agave.php).
The exact frequency and age distribution are gameplay choices rather than
botanical density estimates. Younger wild plants continue growing normally.

The heart and leaves have collision following their growth-stage geometry;
empty corners remain passable. Cutting a leaf immediately clears its collision,
while the exposed piña stays solid until harvested.

## Cut the leaves, then harvest the piña

Craft a **Jimador’s Coa** from three iron ingots and two sticks (iron at top-middle,
top-right and middle-right; sticks at middle-middle and bottom-left). The coa has
512 durability, accepts mining/durability enchantments, and is repaired with iron.
It is the only survival tool that can mine agave or clip its leaves. At stage 7,
mine surrounding leaf sections with the coa, or right-click them. Each right-click
costs one durability and respects block break-event cancellation. Cutting yields no piña or seeds. Any leaf sections disconnected from the heart by
that cut disappear immediately in the same interaction, at no extra durability
cost. Connectivity follows actual shared blade geometry, not just neighboring
block positions; attached leaves stay intact. The plant stays rooted,
and mature cut leaves do not regrow. The missing leaf blocks persist normally in
world saves, so partially harvesting a plant is safe across sessions.

The central heart cannot be mined normally while leaves remain. After the last
owned leaf section is cut, the root's persisted `stripped=true` state exposes its
cream-and-green diamond-scarred piña model. Break that heart with the coa to obtain the existing
`vinum:agave` ingredient and seeds. The produce loot requires `age=7`,
`stripped=true`, and a coa; force-removing an unstripped root cannot bypass the harvest step.
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
chunk is unavailable, so unloading never counts as cutting leaves. Disconnected
leaf cleanup likewise waits until the complete bounded footprint is loaded.

`scripts/generate_agave_models.py` produces the native cuboid rosette and tapered
heart models, blockstates, and `AgaveGeometry.java` from the same tapered radial
leaf geometry, including the shared-face connectivity graph. Generated sparse occupancy and selection bounds stay synchronized
with the models. `AgaveColors` supplies waxy blue-green leaf face colors. The piña
surface uses `textures/block/agave_pina.png`, matching the harvested item artwork.

Client review: grow a plant with seven bonemeal uses in a clear area, inspect the
splayed outer blades and upright center, then cut the leaves one section at a
time. Confirm that the root stays until the last cut exposes the piña. Harvest it
and follow the existing cooking and production chain. Server tests cover stages,
obstruction atomicity, sparse footprint, staged harvest, tool wear, no regrowth,
serialized exposed state, creative removal, and support cleanup; appearance still
requires in-game review.
