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

##### Fifth Bottle [Empty]

The first and most basic of these is the **Fifth Bottle [Empty]** which will be used to hold alcohol coming out of the distiller. Crafting is like this (Note that glass colors are unimportant and just there to show that any glass blcok can be used):

![fifth_bottle_empty_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/fifth_bottle_empty_crafting.png)

##### Pack Of Shot Glasses [25]

Secondly, you will need a **Pack of Shot Glasses [25]** to craft with so that you can drink your Tequila to get effects (more on those later...). The crafting recipe is simple, and supports all types of glass.

![pack_of_shot_glasses_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/pack_of_shot_glasses_crafting.png)

##### Margarita Glass & Required Ingredients

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

##### Steel Brazier

The **Steel Brazier** block serves as the heat source to the **Pot Still Multiblock**. It can also be right clicked with Flint and Steel and it will light on fire for a very pleasing decorative block to spice up your builds and bases.

![steel_brazier_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_brazier_crafting.png)

![steel_brazier_unlit](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_brazier_unlit.png)

![steel_brazier_lit](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_brazier_lit.png)

##### Steel Pot

The **Steel Pot** block is used twice in our **Pot Still Multiblock**, so keep in mind you will need to craft two of these.

![steel_pot_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_pot_crafting.png)

##### Steel Coil

The **Steel Coil** block is used to collect the steam from one pot and condense the alcohol and drain it into the other pot.

![steel_coil_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/steel_coil_crafting.png)

##### Assembling the Multiblock

To assemble the **Pot Still Multiblock**, place the **Steel Brazier**, **Steel Pots**, and **Steel Coil** in this fashion

![pot_still_assembly_guide](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/pot_still_assembly_guide.png)

It will then turn into this multiblock and you can access its GUI by right clicking any part of it:

![pot_still_assembled](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/pot_still_assembled.png)

***KNOWN BUG NOTE***: It is important that you are facing the same direction of the pot still and orienting the blocks all in the same direction when placing them to make the **Pot Still Multiblock**. If you do not do it this way, but instead place blocks against the ground or in different directions, the Multiblock will fail to assemble and the Pot Still GUI will not be accessible. This is not ideal and it is a small bug we are working to resolve. If you encounter this issue, simply break the blocks with a pickaxe and re-place them again. We apologize for any inconvenience this causes.

### It's Tequila Time!

We now have all the things gathered that we will need to make our first **Fifth [Silver Tequila]**. Here are the sub-steps:

##### Cooked Agave Pulp

Before we ferment it, we must take our **Agave** and cook it to make **Cooked Agave Pulp** This is edible as a food on its own and has the same nutritional value as a vanilla Dried Kelp item.

![cooked_agave_pulp_smelting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/cooked_agave_pulp_smelting.png)

##### Unfermented Agave Mash 

One of the steps of making most alchols is the creation of what is called a wort. This is usually a combination of some base fruit, vegetable, or grain and water with occasionally some other niche ingredients. In the case of tequila, the agave plant is harvested and cleaned, then cooked and blended, then mixed with water. This produces what is called **Unfermented Agave Wort**.

![unfermented_agave_wort_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/unfermented_agave_wort_crafting.png)

##### Fermented Agave Wort

Fermentation is the process by which all alcohol comes into the world. Yeasts and bacteria, often naturally occuring but sometimes added intentionally, break down sugar molecules and produce ethanol (read:alcohol) molecules. This process takes place inside wooden or stainless steel barrels. It is believed that the first alcohol was discovered by accident, as fermentation can occur in nature under certain conditions. In addition to producing ethanol, serveral other flavor and aromatic compounds can be produced depending on the exact compounds present in the wort and the specific species of microbe breaking them down. This is what makes all alcohol taste different. Here is the recipe for **Fermented Agave Wort**:

![fermented_agave_wort_crafting](https://raw.githubusercontent.com/boredhero/pv/wiki/src-wiki/assets/fermented_agave_wort_crafting.png)

##### Using the Pot Still GUI

