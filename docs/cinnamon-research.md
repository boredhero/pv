# Cinnamon tree: ecological basis and world-generation choices

Research checked 2026-11-02. Vinum's tree represents **true/Ceylon cinnamon,
*Cinnamomum verum***, rather than treating every tree sold as cinnamon as one
species. These are botanical references and game-design decisions, not claims
that Minecraft reproduces Sri Lanka geographically.

## Species and habitat evidence

Kew accepts *C. verum* as native to Sri Lanka and primarily a wet-tropical tree.
Its widespread occurrence elsewhere includes introduced populations. In
contrast, Kew's accepted *C. aromaticum* (Chinese cassia) is native to southeast
China through Vietnam and is primarily subtropical. Consequently, evidence for
cassia, camphor trees, or unspecified “wild cinnamon” should not silently define
the climate or appearance of this tree. Sources: [Kew: true cinnamon](https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A463752-1/general-information),
[Kew: Chinese cassia](https://powo.science.kew.org/taxon/urn%3Alsid%3Aipni.org%3Anames%3A463288-1).

Sri Lanka's Department of Cinnamon Development describes an evergreen tropical
hardwood, naturally much taller than the repeatedly harvested, multistemmed
commercial bush. Its cultivation guidance gives warm temperatures (25–35 °C),
high humidity, and substantial rainfall; waterlogged or saline ground is
unsuitable. Its commercial elevation recommendation is 700 m. These are
**cultivation guidelines**, not proof that every wild tree stops at that height.
Source: [Department of Cinnamon Development: environmental factors](https://cinnamon.gov.lk/about-cinnamon/environmental-factors/).

The IUCN assessment published in 2024 (assessment date 2019) reports the species
as common in Sri Lanka while explicitly stating that wild population size and
trend are not known. It lists moist lowland and moist montane forest habitat and
classifies it Vulnerable because of limited documented distribution and habitat
decline. There is an internal elevation discrepancy: its justification says
700 m, whereas the habitat account and upper-elevation field say 1,700 m. Neither
is a defensible exact Minecraft height limit. This assessment is not a census of
stems per hectare or a measurement of cinnamon's proportion of the forest.
Source: [IUCN assessment, de Kok 2024](https://doi.org/10.2305/IUCN.UK.2024-1.RLTS.T15441323A15441339.en).

Sri Lanka's forest-genetic-resources country report also distinguishes highly
domesticated, extensively cultivated *C. verum* from its wild relatives in
natural forests. Plantation coverage should therefore not be converted into
natural spawn density. Source: [Sri Lanka country report to FAO](https://www.fao.org/4/i3825e/i3825e63.pdf).

## Translation into Minecraft world generation

The following numbers are **gameplay tuning**, not measured ecological density.
The aim is an occasional recognizable tree mixed into an appropriate forest,
with saplings providing a renewable supply after discovery.

| Minecraft biome | Proposed rule | Reasoning |
| --- | --- | --- |
| `minecraft:jungle` | One natural placement attempt per 8 chunks on average | Best vanilla approximation of warm, wet evergreen forest |
| `minecraft:sparse_jungle` | One attempt per 12 chunks on average | A secondary approximation for open tropical woodland and forest margins |
| `minecraft:bamboo_jungle` | Omit by default | A deliberate biome-identity choice, not evidence that cinnamon cannot grow near bamboo |
| Swamp and mangrove swamp | No natural generation | Persistent saturated ground is a poor match for the drainage requirement |
| Savanna, desert, badlands | No natural generation | The baseline tree represents wet-forest habitat, not irrigated cultivation |
| Temperate/cold forests, snowy and alpine biomes | No natural generation | Poor climatic analogues for this species |

An attempt can fail because of unsuitable ground or insufficient space. It is
not a guarantee of one tree every eight or twelve chunks, and chunks do not have
an exact real-world ecological area correspondence. Keep these parameters
configurable and report placement attempts separately from successful trees in
generation tests.

Use ordinary soil-supported placement, with air and trunk/canopy clearance;
reject underwater trunks and avoid forcing neighboring chunks to load. Let the
warm biome and available vertical space determine placement rather than
converting metres of real elevation into an arbitrary absolute Minecraft Y
cutoff. A moderate single trunk with a compact branching evergreen crown is a
better wild-tree reference than a commercial stool pruned to harvesting height.
Player-grown saplings can follow normal Minecraft growth rules outside the
natural-generation biome whitelist; that is a cultivation convenience.

## Foliage and harvesting interpretation

Singapore's NParks describes opposite, dark-green, ovate-to-elliptic leaves with
three conspicuous veins running from the base toward the tip. Young foliage is
reddish before turning green. Its natural-height description is 8–17 m. For the
leaf texture, these imply distinct elongated leaf shapes, subtle pale/yellowish
three-vein structure, and restrained reddish new-growth accents rather than
maple-like lobes or conifer needles. Source: [NParks species account](https://www.nparks.gov.sg/florafaunaweb/flora/2/8/2807).

Kew's cultivation account explains that the spice comes from processed inner
bark of harvested shoots. A game interaction that changes a bark-covered log
to a stripped log while yielding cinnamon is an abstraction of peeling bark;
dropping cinnamon from leaves, crafting arbitrary oak with shears, or generating
endless spice from an already stripped log would not reflect that origin.
Source: [Kew cinnamon profile](https://www.kew.org/plants/cinnamon).

Wood end-grain and bark artwork should be informed by independently identified
photographs of cut cinnamon stems, not by the familiar rolled spice quill or an
unidentified “cinnamon-colored” furniture wood. The art/reference work is tracked
separately from this ecological spawn recommendation.
