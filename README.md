# WorldMerge
Merge Minecraft worlds together, using an image as a template.

This is a Bukkit plugin, for Minecraft 1.14+.

# What this does
This plugin allows you to merge any number of Minecraft worlds together, based on an image template. This has a variety of uses, including simply drawing on a map, copying structures or terrain between worlds, or creating custom worlds. You can even use it to replace biomes in a vanilla world!

# How it works
First, you need source worlds. These are the worlds that blocks will be copied from. Load all of the worlds into the server using a world manager plugin.

Then, you need an image. Each pixel on the image represents one vertical column of blocks (bedrock to sky limit). The image should contain solid colours, each colour represents a different source world.

For example, the colour red could refer to one world, while the colour blue could refer to another world. Both worlds will be merged into the target world, in the columns that coloured accordingly on the image.

The actual merge process, performed by the plugin, is as follows:
1) Get color from image
2) Get the source world (determined by color)
3) Get the game co-ords (if image is offset from game world)
4) Get a snapshot of the source blocks and biome
5) Set the blocks in the target world
6) Set the biome in the target world
