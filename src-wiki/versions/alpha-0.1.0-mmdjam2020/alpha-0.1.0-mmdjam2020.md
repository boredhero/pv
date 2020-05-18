# Wiki for Project Vinum
## Version alpha-0.1.0 (MMD-Jam 2020)
[Back to Main](https://github.com/boredhero/pv/blob/wiki/src-wiki/Main.md)

Welcome! In future versions, this will have more pages, but for now, we will just show you how to get started, and explain a bit about what this very early alpha build of our mod does.

## Getting started

In this version of the mod, the amount of features added are very limited. For this reason, we will be going over how to get from zero to having everything this mod offers, and providing general information along the way.

### Finding Agave

To get agave, you need to find the nearest large sandy area. This can be a beach or a desert. There you will find Agave plants.

![agave-in-desert](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/agave_plant_in_world.png)

You can harvest these to get **Agave (Item)** and **Agave Seeds**. Agave Seeds will allow you to right click on any sand or farmland block to plant a new agave plant to grow. You can also craft the agave item into agave seeds should you ever need any.

![agave-seeds-crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/agave_seeds_crafting.png)

### Making Steel

For many of this mods crafting recipes, you will need a **Steel Ingot** and/or **Steel Nugget**. To make these, you will need to first craft a vanilla Minecraft Blast Furnace. Then, blast iron ingots and iron nuggets to get their respective steel items. Of course, you can always drop an ingot in a crafting table to get nine nuggets, and vice versa to turn nuggets into ingots. 

Additionally, both our ingots and nuggets are registered with Forge Tagging (the successor to Forge OreDictionary) so that you can use our nuggets and ingots with any recipes that support tagged items, and all of our recipes support tagged steel so all our recipes work with other mods tagged steel as well.

![steel-ingot-blasting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_ingot_blasting.png) ![steel-nugget-blasting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_nugget_blasting.png) ![steel-ingot-crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_ingot_crafting.png) ![steel-nugget-crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_nugget_crafting.png)

### Making Glassware

Obviously, booze needs to be stored. There are several glassware items that you will need to craft to use in crafting recipes and machine interfaces to make alcohol.

#### Fifth Bottle [Empty]

The first and most basic of these is the **Fifth Bottle [Empty]** which will be used to hold alcohol coming out of the distiller. Crafting is like this (Note that glass colors are unimportant and just there to show that any glass blcok can be used):

![fifth_bottle_empty_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/fifth_bottle_empty_crafting.png)

#### Pack Of Shot Glasses [25]

Secondly, you will need a **Pack of Shot Glasses [25]** to craft with so that you can drink your Tequila to get effects (more on those later...). The crafting recipe is simple, and supports all types of glass.

![pack_of_shot_glasses_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/pack_of_shot_glasses_crafting.png)

#### Margarita Glass & Required Ingredients

Thirdly, you will need to make a **Margarita Glass** for the one cocktail currently added by this mod. Unlike the other glass items, this is a little more involved. It will be replaced by a more elegant machine-based system in the future, but for now, there's 3 steps.

1. Craft a **Margarita Glass Mold** like so:

    ![margarita_glass_mold_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/margarita_glass_mold_crafting.png)

2. Make some **Molten Glass** by smelting any glass in a regular vanilla furnace like so:

    ![molten_glass_smelting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/molten_glass_smelting.png)

3. Make the **Margarita Glass** by crafting the **Molten Glass** with the **Margarita Glass Mold** like so:

    ![margarita_glass_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/margarita_glass_crafting.png)

### Making a Barrel

Barrels have all kinds of uses. We will be using them for fermentation and aging in this mod. Currently, this is handled by the **Barrel** item and crafting recipes, but in the future, it will be replaced by a block and GUI system. Here's what you'll need to craft a barrel (Note the variety of wood just shows that you can use any planks you have available):

![barrel_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/barrel_crafting.png)

### Making a Pot Still

The pot still has a venerable history serving humanity in all its alcohol-reducing needs. These can be made out of copper or steel. Ours is made from steel. However, to make the **Pot Still Multiblock** you will first need to craft several items. Here is what you'll need and how to make it:

#### Steel Brazier

The **Steel Brazier** block serves as the heat source to the **Pot Still Multiblock**. It can also be right clicked with Flint and Steel and it will light on fire for a very pleasing decorative block to spice up your builds and bases.

![steel_brazier_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_brazier_crafting.png)

![steel_brazier_unlit](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_brazier_unlit.png)

![steel_brazier_lit](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_brazier_lit.png)

#### Steel Pot

The **Steel Pot** block is used twice in our **Pot Still Multiblock**, so keep in mind you will need to craft two of these.

![steel_pot_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_pot_crafting.png)

#### Steel Coil

The **Steel Coil** block is used to collect the steam from one pot and condense the alcohol and drain it into the other pot.

![steel_coil_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_coil_crafting.png)

#### Assembling the Multiblock

To assemble the **Pot Still Multiblock**, place the **Steel Brazier**, **Steel Pots**, and **Steel Coil** in this fashion

![pot_still_assembly_guide](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/pot_still_assembly_guide.png)

It will then turn into this multiblock and you can access its GUI by right clicking any part of it:

![pot_still_assembled](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/pot_still_assembled.png)

***KNOWN BUG NOTE***: It is important that you are facing the same direction of the pot still and orienting the blocks all in the same direction when placing them to make the **Pot Still Multiblock**. If you do not do it this way, but instead place blocks against the ground or in different directions, the Multiblock will fail to assemble and the Pot Still GUI will not be accessible. This is not ideal and it is a small bug we are working to resolve. If you encounter this issue, simply break the blocks with a pickaxe and re-place them again. We apologize for any inconvenience this causes.

### It's Tequila Time!

We now have all the things gathered that we will need to make our first **Fifth [Silver Tequila]**. Here are the sub-steps:

#### Cooked Agave Pulp

Before we ferment it, we must take our **Agave** and cook it to make **Cooked Agave Pulp** This is edible as a food on its own and has the same nutritional value as a vanilla Dried Kelp item.

![cooked_agave_pulp_smelting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/cooked_agave_pulp_smelting.png)

#### Unfermented Agave Mash 

One of the steps of making most alchols is the creation of what is called a wort. This is usually a combination of some base fruit, vegetable, or grain and water with occasionally some other niche ingredients. In the case of tequila, the agave plant is harvested and cleaned, then cooked and blended, then mixed with water. This produces what is called **Unfermented Agave Wort**.

![unfermented_agave_wort_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/unfermented_agave_wort_crafting.png)

#### Fermented Agave Wort

Fermentation is the process by which all alcohol comes into the world. Yeasts and bacteria, often naturally occuring but sometimes added intentionally, break down sugar molecules and produce ethanol (read:alcohol) molecules. This process takes place inside wooden or stainless steel barrels. It is believed that the first alcohol was discovered by accident, as fermentation can occur in nature under certain conditions. In addition to producing ethanol, serveral other flavor and aromatic compounds can be produced depending on the exact compounds present in the wort and the specific species of microbe breaking them down. This is what makes all alcohol taste different. Here is the recipe for **Fermented Agave Wort**:

![fermented_agave_wort_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/fermented_agave_wort_crafting.png)

#### Using the Pot Still GUI to make a Fifth [Silver Tequila]

**Fermented Agave Wort** will have a very low alcohol concentration (~ a few %) and is full of nasty brown lumpy things that you really don't want to drink. This is where our lovely pot still comes in. A fire beneath the first pot which is filled with **Fermented Agave Wort** heats it up. Because ethanol and some aromatic/flavor compounds (these are called terpines) evaporate at a lower temperature than water and solids, they evaporate. This evaporate is then carried upwards and enters the coil. This coil is exposed to the cool air, cooling down the vapors and causing them to condense and slowly drip into the second pot where they can be bottled for consumption. This process is quite time consuming.

![fifth_silver_tequila_pot_still](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/fifth_silver_tequila_pot_still.png)

***KNOWN BUG NOTE***: As of this alpha build, the Pot Still has an issue where the GUI doesn't update to show the lit flame indicator or the progression indicator in the coil. We are aware of this bug and are actively working to fix it. It may seem like nothing is happening when you put all the ingredients in. This is not the case, but this action has a very high tick time to simulate reality a bit. Just let it sit and you'll find your alcohol there in less than a minute!

#### Fifth [Golden Tequila]

Just like many other forms of alcohol, aging post-distillation is used to add flavor to the alcohol. This happens with tequila too. The word "silver" is used to designate a clear non-aged tequila, and "golden" designates an aged one. Over time, the ethanol dissolves a little bit of the terpine's from the wood of the barrel, imparting varied flavors that can range from smoky (burned barrels), or woody (plain wood barrels). This also results in a color change, hence the color based designations. 

For now, there is no difference in drinking either in this mod, but the latter is necessary for our cocktail, and is an extra step you must perform if you want to make the cocktail. If you just want the base effects (more on these later) of drinking a shot, you may simply consume the silver.

![fifth_golden_tequila_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/fifth_golden_tequila_crafting.png)

## Consuming Alcoholic Beverages

In real life, all alcohol has an Alcohol By Volume (ABV) value. Depending on %, a different volumes of different drinks with different ABVs constitute what is called a "Standard Alcohol Unit". This Handy Chart that I'm sure you've seen if you've ever been a college freshman entering an American University way more times during the first week than you wish you had helps explain:

![standard-alcohol-units](https://upload.wikimedia.org/wikipedia/commons/thumb/b/b7/NIH_standard_drink_comparison.jpg/400px-NIH_standard_drink_comparison.jpg)

For the purposes of this mod, we are trying to be *mostly* realistic. For simplification sake, at this version, 1 shot = 1.0 FL OZ = 1 Standard Alcohol Unit. This is functional and *close enough* to reality. Keep this in mind when you are crafting and consuming cocktails that use multiple shots of alcohol, as you are then consuming more than one SAU per drink at that point. You'll see why this matters in a second.

Alcohol is a depressant that has "stimulant" effects at first, then drops off into pure depressant territory. In the pursuit of realism, this mod aims to simulate that. In the future, there will be a Blood Alcohol Content (BAC) bar above your hunger bar, but for now, this is missing. We apologize for the inconvenience. Here is another chart I'm sure you never wanted to see again:

![bac-chart](https://i1.wp.com/www.alcoholproblemsandsolutions.org/wp-content/uploads/2016/07/03-chart.png)

How many SAUs = what % BAC varies in real life based on many factors including gender, muscle mass weight, personal tolerance built up, and liver health. We simulate this graph by adding positive effects (you get more the drunker you are, and they last until the drink wears off). However, after 5 drinks, you start experiencing negative effects, up to and including death! Fortunately, just like in real life where the liver processes 1 SAU out of your body / hour after drinking, the mod will keep track of and reduce your SAU count by 1 every 5 real life minutes. Try to keep this in mind as you drink.

Here is a table of the effects you will get added to you at each level of drunkenness. Please keep in mind that these are stacked, so at 3 SAU, you get the effects of the first three rows, having the third row effect for 5 minutes, the second row effect for 10m, and the first row effect for 15, assuming you don't drink more to maintain them before it dips down.

[reg]: https://www.digminecraft.com/effects/images/regeneration_sm.png "Regeneration"
[str]: https://www.digminecraft.com/effects/images/strength_sm.png "Strength"
[jbt]: https://www.digminecraft.com/effects/images/jump_boost_sm.png "Jump Boost"
[abs]: https://www.digminecraft.com/effects/images/absorption_sm.png "Absorption"
[nau]: https://www.digminecraft.com/effects/images/nausea_sm.png "Nausea"
[wks]: https://www.digminecraft.com/effects/images/weakness_sm.png "Weakness"
[slo]: https://www.digminecraft.com/effects/images/slowness_sm.png "Slowness"
[bli]: https://www.digminecraft.com/effects/images/blindness_sm.png "Blindness"

| BAC Level | Potion Effect          |
| --------- | ---------------------- |
| 1         | ![][reg] Regeneration  |
| 2         | ![][str] Strength      |
| 3         | ![][jbt] Jump Boost    |
| 4         | ![][abs] Absorption    |
| 5         | No New Effect          |
| 6         | ![][nau] Nausea        |
| 7         | ![][wks] Weakness      |
| 8         | ![][slo] Slowness      |
| 9         | ![][bli] Blindness     |
| 10        | Instant Death          |

For this reason, you cannot directly drink a fifth of tequila (They are equivalent to 25 shots)

Cocktails are special, and will cause additional long lasting effects ON TOP of these effects that come default with drinking straight shots!

### Shots

To start drinking, take your **Fifth [Silver Tequila]** or **Fifth [Golden Tequila]** and craft it with a **Pack of Shot Glasses [25]** like so:

![shot_silver_tequila_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/shot_silver_tequila_crafting.png) ![shot_golden_tequila_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/shot_golden_tequila_crafting.png)

## Cocktails

Cocktails are a fun feature of this mod that will add extra potion effects and maybe even custom effects in the future. There will also eventually be custom crafting tables for these and lots of plants and liqours added to use as ingredients. For this alpha build, we have made a single cocktail recipe. Still, you'll need to go looking for things and crafting to make a **Spiced Apple Margarita**

### Bartending Tools

The tools of the great trade of bartending! These items aren't consumed when crafting with them!

#### Cocktail Shaker

*Vodka Martini, Shaken, not Stirred. -* ***James Bond***

The venerable **Cocktail Shaker** and strainer is a quintesential tool used in the creation of a vast variety of cocktails since it was formally invented in the mid 19th century. However, humans have been shaking drinks since at least [**7000 BCE**](https://en.wikipedia.org/wiki/Cocktail_shaker#History)! Ancient inhabitants of modern day Mexico and South America used gourd jars to shake drinks!

![cocktail_shaker_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/cocktail_shaker_crafting.png)

#### Ice Pick

The **Ice Pick** is a common bar tool used to shape large blocks of ultra-clear ice that is delivered to bar establishments regularly. The ice is shaped to fit the glass by a skilled bartender and their pick.

![ice_pick_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/ice_pick_crafting.png)

#### Juicer

Just a trusty **Juicer**. Although; we aren't on a health smoothie diet, we're getting trashed!

![juicer_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/juicer_crafting.png)

### Cocktail Ingredients

Cocktails entail more than just booze...that's why they're cocktails!

#### Ice Cubes

To make **Ice Cubes**, use the **Ice Pick** on any vanilla Ice block.

![ice_cubes_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/ice_cubes_crafting.png)

#### Apple Juice

This **Apple Juice** comes in glass, not a juice box!

![apple_juice_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/apple_juice_crafting.png)

#### Sea Salt

Be Careful around Slugs with this stuff! Still not as salty as you! **Sea Salt** is made by boiling or sun-evaporating sea water, and crumbling the resultant crystals or flakes.

![sea_salt_smelting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/sea_salt_smelting.png)

### Spiced Apple Margarita

The Spiced Apple Margarita was inspired by [this lovely recipe](https://cookieandkate.com/spiced-apple-margaritas/). Our recipe uses two **Shot [Golden Tequila]**, a **Cocktail Shaker** 
