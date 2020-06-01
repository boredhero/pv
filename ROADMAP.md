![pv-logo](https://raw.githubusercontent.com/boredhero/pv/master/src/main/resources/vinum_logo.png)

# Project Vinum Official Roadmap / Ideas Brainstorming

#### Wines
* See [Wiki - Grape Varietals](https://en.wikipedia.org/wiki/List_of_grape_varieties)
* Chardonnay, Sauvignon Blanc, Cabernet Sauvignon, Merlot, Pinot Noir, Zinfandel
  * Have a fully featured seed/plant/fruit for each of the above wine types
  * Cork Tree/Bark
  * Wine Yeast (need to figure out a natural way to get this stuff...)
* Grape Juice
  * A juice bucket for every variety of wine.
* Misc Blocks and Items
  * Sand: Refined Sand, Leaded Sand, Ferrous Sand
  * Glass: Dark Wine Glass, Crystal Wine Glass
  * Grape Press (Currently an Item, but maybe make a machine?)
  * Lead Powder & Ferrous Powder
  * Lead Ore (needs OreDict registry)
  * Wine Barrels (Currently a block, should re-implement as a sort of aging chest)
  * Need to have grapes naturally occur in the world, and add to dungeon loot tables
    * Pick biomes for specific wine varieties?
* Add support for blended wines via a new type of barrel, the "blending barrel"
* Fruit Wines [Wiki Ref](https://en.wikipedia.org/wiki/Fruit_wine)
  * Will need to add fruits for flavorings anyways, this is just another thing to do with them.

#### Beer [Brewing Wiki Ref](https://en.wikipedia.org/wiki/Brewing)
* Add barley, malted barley, corn, sorghum, rye, and hops as plants
* Yeast
* Universal Casks
  * These can be used in the bottler
* Add mash item (combine main grain + supplements to get beer specific mash)
* Add a copper brew kettle
  * Outputs a liquid brew into buckets
    * This is then strained via a sieve item in a crafting table
* Universal Fermentation vat
  * Should have time awareness
  * Takes in strained wort from brew kettle
  * Takes in an empty metal beer cask
  * Exports a full fermented beer cask
* Bottling Machine
  * Takes in casks and empty beer bottles
  * Outputs a fixed amount of full beer bottles and a now empty cask.
* Need to have beer styles
  * At least Pale Lager, IPA, Double IPA, Dark Lager/Amber Ale, Porter, Stout, and Imperial Stout
    * [Beer Style Wiki Ref](https://en.wikipedia.org/wiki/Beer_style#Appearance)

#### Glassware
* **Beer Bottle**
  * Should be brown, should have empty + all possible beer variants
  * Needs a metadata tag for AV and should be around 5-7% AV
* **Wine Bottle**
  * Should be green, should have empty + all possible wine/fruit wine variants
  * Needs a metadata tag for AV and should be around 15% AV
  * Should have a cork involved (see cork under plants)
* **Liquor Fifth**
  * Should be clear, shave have empty + all possible liquor variants
  * Needs a metadata tag for AV and should be 40%, or 100% ABV depending on what goes in it
    * Most liquors will be around 40%
    * Everclear is 100% ABV


#### Universal Storage Cask
* Metal cask that is used throughout the alcohol production process leading up to Bottling

#### Wooden Barrel
* Used to store/age wine, rum, whisky, etc.

#### UAC - Universal Aging Cellar
* Should be wood based
* Should be used to age wines, rums, and whiskey/vodka.
* Should probably work more like a machine than a chest.
* Needs time awareness.
* Accepts casks and Barrels
* Outputs aged casks and barrels to be bottled.

#### UFV (Universal Fermentation Vat)
* Must be used for all beer and spirit fermentation tasks
  * This explicitly excludes wine, which ferments in barrels/casks
* Must have a setting for beer, everclear/vodka, rum, whisky, etc
* Should take in whatever ingredients the specific alcohol requires
* Should take in a universal metal cask
* Should output a cask filled with a middle product that needs filtering/distilling *usually*

#### Universal Strainer (Item)
* It'll turn you cask of fermented mash into a liquid that can be distilled and bottled!
* Just stick your liquid to be filtered and the filter in

#### UD (Universal Distiller)
* Similar in concept to the UFV, but for distilling things.
* Needs a mode for each alcohol type
* Accepts casks of middle product from the UFV
* Outputs finalized casks to be bottled or flavored.

#### UBP (Universal Bottling Plant)
* Similiar in concept to the UFV and UD, but for bottling things
* Should take in casks of finished alcohol products
* Should take in empty wine bottles, fifths, beer bottles/cans, etc.
* Should output empty casks and full containers of alcohol with ABV metadata in label

#### Universal Steeper
* Should take casks and flavoring additives (fruit, spices) and steep them to make things like flavored vodka and spiced rum, etc.

#### Bottle Factory
* Lets blow glass with a machine with different settings
  * Should allow you to select bottle type (wine, beer, fifth, handle)
* Takes different ingredients and acts as a custom furnace that outputs bottles

#### AV
* At this point, all of the various alcohols should have a metadata tag "AV"
* This will range from 0% (Water) to 100% (Everclear)

#### ABV
* There should be a bar on top of the food bar in the player UI
  * This bar should be just like the food bar, but it should be wine glasses or shot glasses
  * It should start out unhighlighted, and then light up as players consume alcohol.
* As players start drinking, we want to initially increase stamina, luck, and the like.
* As players hit a drunk level, we want to start debuffing them
* As players become overly inebriated, we want to add visual field and maybe sound effects
  * Blackening at edges of screen
  * Blurry vision
  * Slow walking speed
* If players continue drinking, they will die of alcohol poisoning
* ABV should decrease by one "glass" per minecraft hour of not drinking.
  * Sleeping in a bed will reset ABV to 0
* AV of consumed liquids will affect how much each drink loads the bar
* If hunger is low/empty, the alcohol consumed should increase ABV 1.5x faster

#### Vodka [Vodka Wiki Ref](https://en.wikipedia.org/wiki/Vodka)
* Vodka is essentially diluted Everclear, so we will start by adding Everclear as an Item
* We will then use a crafting recipe to dilute Everclear into 40% and 50% ABV Vodka
* Need to add bottle sizes Fifth and Handle made of clear Glass
  * These should be accepted by our bottler.
* Vodka should use grains such as sorghum, corn, rye, or wheat
  * However, potatoes, molasses, soybeans, grapes, rice, and sugar beets also work, or even sugar
* Universal Fermentation Vat
  * Vodka mode should accept grains such as sorghum, corn, rye, wheat, potato, etc.
  * Should accept an empty universal cask
  * Should return a cask of vodka mash which must be strained with a universal strainer item in a crafting table.
* Universal Distiller (Vodka Mode)
  * This should take any cask/keg of alcohol and activated charcoal
  * It should output casks of pure Everclear
    * This must then be diluted with water to make vodka via crafting with water.
* Steeping (Optional)
  * Steeper should accept vodka and fruits/spices to produced flavored vodka casks
* Universal bottler
  * Bottles in fifths or handles.

#### Rum [Rum Wiki Ref](https://en.wikipedia.org/wiki/Rum)
* Need to redo sugar
  * [Wiki Ref - Sugar Production from Cane](https://en.wikipedia.org/wiki/Sugarcane_mill)
  * Disable vanilla production of white sugar from crafting sugar cane.
  * Sugar Cane Press
    * Accepts sugar Cane
    * Consumes Fuel
    * Returns cane juice
  * Sugar Refinery
    * Accepts cane juice
      * Outputs White Sugar and molasses
      * White sugar and molassas can be crafted into brown sugar.
* UFV Fermenter (Rum Setting)
  * Accepts sugar, water, molassas
  * Accepts a universal cask
  * Outputs a universal cask
* Must be distilled in a Universal Distiller
  * Rum mode should accept universal casks and wooden Barrels
  * Should output rum into wooden barrels
* If a wooden barrel is bottled directly, you will have clear/white/silver rum
  * If it is aged, you will have a golden/aged rum
  * If it is put into the Universal Steeper with things like vanilla and cinammon, etc, we should produce a spiced rums
    * Fruit flavored rums too

#### Whisky [Whisky Wiki Ref](https://en.wikipedia.org/wiki/Whisky)

* This will piss a lot of people off, but whisky is basically aged vodka made from a few specific types of grain.
  * Barley, Corn, Rye, Wheat
    * Fermented
* Must be aged in wooden barrels

#### Brandy [Brandy Wiki Ref](https://en.wikipedia.org/wiki/Brandy)
* Distilled Wine
  * Aged in wooden Barrels

#### Cocktails!
  * Probably want a bartending crafting table of some sort.
  * Will require more advanced glassware.
  * Will require ice cubes, etc.
  * Will require the addition of liquors such as bitters, amaretto, etc.
  * Will require additional fruits
  * Let your imagination run wild.
    * Pina Coladas in Minecraft!
  * Probably should have some extra special buffs for the player for all the trouble they'll be to make.
  * Bars in villages + Bartender Villager type.
#### Vineyard village
  * Custom villagers
    * Vine Tender, Grape Harvester, Wine Maker, Cellar Manager
  * Fields of grape vines being managed/tended to by Vine Tenders and Grape Harvesters
  * A building where grapes are processed into juice and barrelled by wine makers
  * A cellar where finished and bottled wines are stored.
    * Managed by Cellar Manager, who should be able to sell you a bottle or a glass

#### Cordials
  * Various speciality flavored liqours that can be used in cocktails.
    * Kahlua, Baileys, Triple Sec, Cointrau, ETC ETC

#### Pam's HarvestCraft Interoperability
  * Make the mod compatible with Pam's Food/Items
  * Could be done as a seperate addon jar, or just by checking to see if Pam's is present and adjusting things in the main jar accordingly.
