# Cinnamon texture references

These assets represent true/Ceylon cinnamon, **Cinnamomum verum** (synonym *C. zeylanicum*), rather than cassia or camphor. Research and image inspection: 2026-11-02.

## Photographs actually inspected

- Sri Lanka Department of Cinnamon Development, [post-harvest processing](https://cinnamon.gov.lk/about-cinnamon/post-harvest-technology/): [cut stems / knot removal photograph](https://cinnamon.gov.lk/wp-content/uploads/2024/07/knotting-1024x826.jpg) shows gray-brown outer bark and a pale exposed cut end on a small harvested stem. [Scraping photograph](https://cinnamon.gov.lk/wp-content/uploads/2024/07/scraping-1024x828.jpg) shows the light yellow-tan surface exposed by removing outer bark. These are harvested branches, **not a verified mature trunk cross-section**. Cinnamon quills are processed inner bark, not spirals in the wood.
- Flora of Sri Lanka, [C. verum species account](https://www.floraofsrilanka.com/species/1035): [leaf and branch photograph by Jacob de Vlas](https://www.floraofsrilanka.com/uploads/species/resized/species_1729078332_Cinnamomum%20verum%C2%A0%20%281%29.jpg) shows pointed, broad elliptic leaves with three strong longitudinal veins, a leathery green surface, and gray-brown bark.
- Richter & Dallwitz, [Commercial Timbers: Cinnamomum spp.](https://www.delta-intkey.com/wood/en/www/laucinsp.htm): the [Medang wood plate](https://www.delta-intkey.com/wood/images/medang.jpg) shows an actual magnified transverse section beside natural-scale wood grain. It supports a fine, diffuse-porous texture. **This reference is genus-level, not authenticated C. verum**, so it is used cautiously for general wood structure, not an exact species color match.

No exact-species, mature full-trunk crosscut photograph was sufficiently verified. The end-grain tiles are therefore an interpretation combining the species-specific cut-stem colors with genus-level anatomy, not a claim of photographic reproduction.

## Supporting botanical descriptions

[Kew's species account](https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A463752-1/general-information) describes glossy leathery leaves, conspicuous basal veins, and reddish young foliage. [Singapore NParks](https://www.nparks.gov.sg/florafaunaweb/flora/2/8/2807) specifies dark green leaves with three distinct yellow veins. These support the pointed leaf forms, pale veins and small reddish new shoots in the foliage and sapling.

[PROSEA's Cinnamomum timber treatment](https://prosea.prota4u.org/view.aspx?id=4066), whose studied species include C. verum, describes pale sapwood and fine, even texture with inconspicuous growth rings. The assets consequently use understated pale tan wood and faint grain, rather than orange cinnamon-colored wood or conspicuous oak rings.

## Asset production

Seven original images were generated with the **built-in imagegen tool**, one call per asset. Source photographs were used for visual research; they are not redistributed as game textures. Pillow only packaged the generated images by nearest-neighbor downscaling to 32×32 pixels. The final assets were visually inspected. Leaves and sapling retain generated alpha; all five wood tiles are fully opaque.

Final directory: `src/main/resources/assets/vinum/textures/block/`.

### Final prompt set

Shared request: one square game-ready Minecraft texture, 32×32 pixel-grid appearance, hard pixels, restrained vanilla-style palette, no text, perspective or rendered cube. Opaque tiles fill the image edge to edge.

| Asset | Specific prompt |
|---|---|
| `cinnamon_log.png` | True cinnamon gray/light-brown bark, finely broken vertical streaks and small lenticels; #81715d and #a19379 with restrained #655743 fissures; seamless; no deep oak furrows, moss or quill scrolls. |
| `cinnamon_log_top.png` | Pale straw-cream tan end grain #d7bd89; thin gray-brown bark frame, tiny muted pores, three very faint irregular squared growth rings and off-center pith; tropical diffuse-porous wood, no bold oak rings. |
| `stripped_cinnamon_log.png` | Pale tan #d7bd89 with fine #b69b6c vertical grain and tiny pores; no bark, panels, border, large knots or orange quill imagery. |
| `stripped_cinnamon_log_top.png` | Same pale tan palette, tiny pores and three faint squared growth rings with pith; no bark frame or circular-log background. |
| `cinnamon_planks.png` | Four horizontal pale tan planks with staggered short end joints; #d7bd89 base, #b69b6c grain, #927747 seams; fine even grain and sparse pores; seamless, no giant knots. |
| `cinnamon_leaves.png` | Dense overlapping broad pointed elliptic leaves at varied diagonals; three prominent pale longitudinal veins per leaf; two tiny copper-red new leaves; #294a29/#466c34/#91a15e; foliage reaches edges with transparent gaps and no exterior margin; genuinely transparent background. |
| `cinnamon_sapling.png` | Thin brown stem rooted near bottom center, three opposite pointed leaf pairs with three pale veins, copper-red top pair, matching leaf palette, transparent margin, no pot, soil or shadow; genuinely transparent background. |

The native door and trapdoor models reuse the plank material rather than introducing unrelated palettes.
