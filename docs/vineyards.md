# Vineyard vines

Cultivated grapes use two-block-tall wooden trellises. Place supports in a row,
then use a variety of grape seeds on a support to plant it. The vine keeps its
variety throughout growth, harvesting, and regrowth. Wild grape bushes remain a
source of fruit and seeds; cultivated seeds require a trellis.

Craft two trellises with sticks in the four corners and any planks across the
middle row (`S S / PPP / S S`). Place them on dirt-family blocks or farmland,
with both occupied cells clear. A planted support can be dismantled from either
half; Survival returns one trellis, the variety's seeds, and any remaining ripe
fruit. Picking a ripe cultivated bunch yields one or two grape items.

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
