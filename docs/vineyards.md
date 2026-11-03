# Vineyard vines

Cultivated grapes use two-block-tall wooden trellises. Place supports in a row,
then use a variety of grape seeds on a support to plant it. The vine keeps its
variety throughout growth, harvesting, and regrowth. Wild grape bushes remain a
source of fruit and seeds; cultivated seeds require a trellis.

Craft two trellises with sticks in the four corners and any planks across the
middle row (`S S / PPP / S S`). Place them on dirt-family blocks or farmland,
with both occupied cells clear. A planted support can be dismantled from either
half; the planted root returns its seed, while spread branches do not create
extra seeds. Uprooting restores connected branch supports. Picking a ripe cultivated bunch yields one or two grape items.

The permanent trunk develops horizontal arms along the support. Growth advances
through a young shoot, training, foliage, flowering, small green bunches, enlarged
green bunches, coloring fruit, and ripe fruit. Right-click a ripe bunch to pick it
without destroying the vine. The other bunch stays in place until picked. Once
both are harvested, the established vine starts another fruiting cycle rather
than returning to a seedling.

The ripe berry palette is defined separately from juice, must, and wine colors in
`vinum/wines.json`, using `grape_color`. Those colors match the existing grape item
art: pale yellow Chardonnay, yellow-green Sauvignon Blanc, blue Cabernet
Sauvignon, gray-purple Merlot, dark Pinot Noir, and rose Zinfandel. Immature fruit
starts green and transitions toward its variety's ripe color. Shared geometry
provides the berry clusters; adding a palette does not require repainting every
growth-stage model.

## Natural spread and artwork

One planted root can occupy up to five same-facing trellises in a continuous,
level row. Only natural random ticks attempt expansion; bone meal advances
existing growth/fruit and never creates a new section. Set
`maxTrellisesPerVine` from 1–16 in `vinum-vineyard-server.toml` to change the
maximum total sections per seedling, including its original support.

Branches retain their original root identity across saves. Cutting a middle arm
prunes its disconnected outer growth; uprooting the parent restores the empty
supports. Chunk boundaries never force loads, and temporarily unavailable roots
are not treated as destroyed. Lowering the configured cap stops further spread
without removing existing growth.

The canopy uses one shared 32×32 leaf cutout with veins and serrated lobes,
layered at varied angles across a broader, deeper canopy. Lower leaves project
outward from attached petioles. Stage 3 shows branching cream-green flower
clusters; stage 4 replaces them with small green grapes. Each bunch uses the shared 64×64 neutral berry material
and the variety's color. Spread sections carry upper canopy without another
rooted vine trunk. Prompts and final asset paths are in `vine-art-prompts.json`.

## Research and adaptation

The design adapts a bilateral cordon: a permanent trunk with horizontal woody
arms supported along a trellis. Real vineyards commonly use posts and tensioned
wire. The game's wooden frame and thin cross wires make that support readable
at Minecraft scale, with space between rows for harvesting.

- [University of Minnesota: growing grapes in the home garden](https://extension.umn.edu/garden-and-home/yard-and-garden/gardening-in-minnesota/growing-grapes-in-the-home-garden)
  explains permanent supports and the importance of sunlight for fruiting.
- [University of Missouri: grape training systems](https://extension.missouri.edu/publications/g6090)
  describes bilateral cordons and their accessibility for picking and pruning.
- [University of Maryland: growing grapes](https://www.extension.umd.edu/resource/growing-grapes-home-garden)
  describes cutting ripe clusters from the plant while preserving the vine.

Growth and repeat fruiting use Minecraft's random-tick and bone-meal mechanics;
they do not simulate seasons or real-world years of establishment.

## Leaf color correction

Healthy mature foliage now uses a muted green blade and softer sage veins instead
of the previous strongly yellow-lime palette. The 32×32 alpha silhouette and UV
layout are unchanged. Three modest upper-face tints vary the canopy; the reverse
uses the unshaded palette and is slightly paler, with no added polygons or textures.
This is an artistic palette choice, not a claim that every cultivar shares one
measured RGB color.

[NC State Extension](https://plants.ces.ncsu.edu/plants/vitis-vinifera/) describes
green, toothed, lobed leaves and potentially slightly fuzzy undersides.
[Singapore NParks](https://www.nparks.gov.sg/florafaunaweb/flora/5/8/5822) describes
paler undersides. The treatment is deliberately subtle because
[Oregon State Extension](https://extension.oregonstate.edu/crop-production/wine-grapes/grape-variety-identification)
notes that heavily downy backs common in some American grapes are not typical
of European wine grapes. Fruit, stems, and flower palettes are unchanged.
