# Proposal for MMD-Jam Mod Contest of Project Vinum Alpha Features

## Contributors:
* Noah Martino (A.K.A. Captain Bored) - personal.boredhero@gmail.com
* Yooks
* Rellit

### Alcohol Type: Tequila
Tequila is an agave based alcoholic beverage generally bottled at 40% alcohol by volume/proof. It is indigenous to the American Southwest deserted areas where the Agave plant is native. The agave plant is high in sugar, and thus an ideal fermentation ingredient for alcohol production. For centuries, the native peoples of the region have produced some form or another of agave-based alcohol. Tequila can be consumed right after distillation (Silver Tequila) OR it may be placed back into barrels for aging, which causes it to turn golden, and then it is referred to as Gold Tequila.

For the production of Tequila using this mod, the following steps must occur. Items, Blocks, and Emphasis in bold:
1. Craft **Glass Fifth Bottle [Empty]** from **Vanilla Glass Blocks**
2. Harvest **Agave Plant** to get **Agave**
3. Cook **Agave** in a **Vanilla Furnace** to get **Cooked Agave**
4. Craft **Cooked Agave** with **Vanilla Water Bucket** to get **Unfermented Agave Wort**
5. Ferment **Unfermented Agave Wort** in **Barrel** for **1-2 Days** to get **Fermented Agave Wort**
6. Cook **Glass Fifth Bottle [Empty]** (or **Canteen**), **Fermented Agave Wort**, and **Vanilla Fuel Item** in **Still**
7. Get a **Silver Tequila Fifth** or **Canteen (Silver Tequila)** and **Vanilla Empty Bucket** out

* Note: A whole bottle of tequila = 25 **Standard Alcohol Units** and will kill a player if consumed directly. To counteract this, the player may craft shot glasses and consume those individually, OR using the **Canteen**, a special item that supports taking multiple swigs (1 shot equiv. per swig). Lastly, they can craft a **Cocktail** using the shots of tequila which may then be consumed safely, and will also provide perks beyond the perks of standard alcohol consumption alone, similar to a Pam's "Top Tier Food" item.

### Machine: Barrel
A barrel is a wooden cask used for the fermentation and aging of alcohol. It can be crafted from most any wood. In our case, it should be just like a chest except that it has a different texture and is capable of keeping track of time passed and replacing the items in it with updated items.

The Barrel Must:
1. Accept **Unfermented Agave Wort** in its chest slots
2. Keep track of how long each **Unfermented Agave Wort** has been aging.
3. After **1-2 days of aging**, replace the **Unfermented Agave Wort** with **Fermented Agave Wort**
4. Also be able to do 1-3 for **Silver Tequila Fifth/Canteen (Silver Tequila)** --> **Gold Tequila Fifth/Canteen (Gold Tequila)**

### Machine: Still
A still is a machine universal to the production of all high-proof (> 15-20% ABV) spirits. It serves two purposes. The first is to concentrate the alcohol in a low concentration wort into something of usable strength. The second is that the pollutants, bacteria, and mash from the fermented liquid, professionally referred to as "wort", are filtered out automatically by this process. Most simple stills are essentially a pot that is heated with a metal coil out the top that catches evaporated alcohol and flavor compounds, and then they condensate in the hose and drip into the other tank, which is your drinking grade alcohol. We will be implementing this process as a furnace-like machine.

The Still must:
1. Accept **Vanilla Furnace Fuels**, **Empty Bottles/Canteens**, and **Fermented Agave Wort**
2. Consume **Vanilla Furnace Fuel**
3. Output **Vanilla Empty Bucket** AND **Tequila Fifth/Canteen**

### Special Feature: Cocktails
Cocktails are made with other ingredients than just liqour. They may have strange and mysterious yet powerful and beneficial potion effects at what is arguably a much cheaper cost than making potions themselves from rare ingredients from the nether and mob drops. These are TBD but will be very flexible.

### Special Feature: Blood Alcohol Content (BAC)/Alchol By Volume (ABV)
Blood alcohol content is the measure of how much alcohol is in a person's blood. A little, and the person may experience various uplifting and pleasant tonic-like effects. A little more, and these effects increase at a slight cost in visual effects. However, drink too much booze, and one will end up dying from alcohol poisoning. You can consume standard units of alcohol (1.5 fl oz shots of 40% ABV tequilla, for example), at whatever rate you want, but the human liver can only dissipate their effects at a rate of 1 standard unit of alcohol per hour. This must be minded.

This BAC System will be responsible for:
1. Tagging every consumable alcoholic item with a number of how many standard units of alcohol it contains.
2. Used as a reference for applying positive and negative effects to the player based on their drinking.
3. Updating a HUD row with 10 empty shotglasses, filling them up to show how many standard units the player has consumed.
  1. At 1-4 Shots, the player should experience mild positive effects
  2. At 5-7 Shots, the player should experience very positive effects with visual distortion
  3. At 8-9 Shots, the player should experience nausea, blindness, and poisoning effects
  4. At 10 Shots, the player will instantly be killed with a display message of "[Player] died of Acute Alcohol Poisoning"

### Special Feature: Mod API
An API that allows other mods to see and interface with the machines, items, and especially and most importantly, the BAC/ABV System.

The API should:
1. Provide full access to machines to add new recipes
2. Provide full access to querying a players BAC and updating it.
3. Provide full access to tag items in their own mods with the ABV Standard Units of Alcohol

### Full Registry of Blocks, Items, Machines, ETC

* Items: Agave Seeds, Agave, Cooked Agave, Unfermented Agave Wort, Fermented Agave Wort, Glass Fifth Bottle [Empty], Silver Tequila Fifth, Gold Tequila Fifth, Shot Glass [Empty], Shot Glass [Silver Tequila], Shot Glass[Gold Tequila], ??Canteen & Variations??

* Machines: Barrel, Still
