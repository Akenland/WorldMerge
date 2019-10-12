package com.kylenanakdewa.worldmerge;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.awt.image.BufferedImage;
import java.awt.Color;
import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * WorldMerge Plugin
 *
 * @author Kyle Nanakdewa
 */
public class WorldMergePlugin extends JavaPlugin {

    private static WorldMergePlugin plugin;


    /** The target world, to merge into. */
    private World targetWorld;

    /** The image file to use. */
    private BufferedImage image;

    /** The X offset of the image, relative to the game world. */
    private int xOffset;
    /** The Z offset of the image, relative to the game world. */
    private int zOffset;

    /** The color mappings to use for merging. */
    Map<Color, World> colorMappings;


    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        getCommand("worldmerge").setExecutor(new WorldMergeCommands());

        prepareMerge();
    }

    /**
     * Gets the plugin instance.
     */
    public static WorldMergePlugin getPlugin() {
        return plugin;
    }

    /**
     * Loads the plugin configuration, to prepare to start merging.
     */
    void prepareMerge() {
        reloadConfig();

        loadTargetWorld();
        loadImageFile();
        loadImageOffset();
        loadColorMappings();
    }

    /** Load the target world. */
    private void loadTargetWorld() {
        String worldName = getConfig().getString("target-world");
        targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null)
            getLogger().severe("Target world " + worldName + " was not found!");
    }

    /**
     * Load the image file.
     */
    private void loadImageFile() {
        String fileName = getConfig().getString("map-image-file", "map.png");

        File file = new File(getDataFolder(), fileName);
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            getLogger().severe("Unable to load map image file: " + e.getLocalizedMessage());
        }
    }

    /**
     * Load the image offset.
     */
    private void loadImageOffset() {
        xOffset = getConfig().getInt("offset.x", 0);
        zOffset = getConfig().getInt("offset.z", 0);
    }

    /**
     * Load the world-color mappings.
     */
    private void loadColorMappings() {
        colorMappings = new HashMap<Color, World>();

        for (Entry<String, Object> mapping : getConfig().getConfigurationSection("color-mappings").getValues(false)
                .entrySet()) {
            Color color = Color.decode(mapping.getKey());

            String worldName = mapping.getValue().toString();
            World world = Bukkit.getWorld(worldName);
            if (world == null)
                getLogger().warning("Mapped world " + worldName + " for color " + color.getRGB() + " was not found!");

            colorMappings.put(color, world);
        }
    }

    /**
     * Gets the color of a specific pixel on the image. If the requested pixel is
     * out-of-bounds, returns black.
     */
    private Color getPixelColor(int x, int y) {
        if (x > image.getWidth() || y > image.getHeight()) {
            return Color.BLACK;
        }

        return new Color(image.getRGB(x, y));
    }

    /**
     * Gets the color of a specified pixel on the image, using game world X/Z
     * values. This will use the offset.
     */
    @SuppressWarnings("unused")
    private Color getPixelColorFromGame(int x, int z) {
        x -= xOffset;
        z -= zOffset;

        return getPixelColor(x, z);
    }

    /**
     * Gets the equivalent game world co-ord for an image x/y pixel, using the
     * offset.
     */
    private int[] getGameLocFromPixel(int x, int y) {
        x += xOffset;
        int z = y + zOffset;

        return new int[] { x, z };
    }

    /**
     * Gets the source world represented by the specified color. May return null.
     */
    private World getSourceWorldForColor(Color color) {
        return colorMappings.get(color);
    }

    /**
     * Captures all block data in a single column.
     */
    private BlockData[] getBlockDataColumn(World world, int x, int z) {
        BlockData[] column = new BlockData[world.getMaxHeight()];

        for (int y = 0; y < world.getMaxHeight(); y++) {
            BlockData blockData = world.getBlockAt(x, y, z).getState().getBlockData();
            column[y] = blockData;
        }

        return column;
    }

    private enum MergeResult {
        /** Merge completed successfully for this point. */
        SUCCESS,
        /** Merge failed, source world not found. */
        NO_WORLD,
        /** Merge not attempted, no color on image map. */
        NO_COLOR,
        /** Unknown error during merge. */
        ERROR;
    }

    /**
     * For a pixel in the image map, perform a merge of this column.
     */
    private MergeResult mergeFromImagePixel(int x, int y) {
        // Get color from image
        Color color = getPixelColor(x, y);
        if (color.equals(Color.BLACK))
            return MergeResult.NO_COLOR;
        // Get source world
        World sourceWorld = getSourceWorldForColor(color);
        if (sourceWorld == null)
            return MergeResult.NO_WORLD;

        // Get game co-ords
        int[] gameLoc = getGameLocFromPixel(x, y);
        int gameX = gameLoc[0];
        int gameZ = gameLoc[1];

        // Get source blocks and biome
        BlockData[] sourceBlocks = getBlockDataColumn(sourceWorld, gameX, gameZ);
        Biome sourceBiome = sourceWorld.getBiome(gameX, gameZ);

        // Set blocks in target world
        for (int gameY = 0; gameY < sourceWorld.getMaxHeight(); gameY++) {
            BlockData newData = sourceBlocks[gameY];

            Block block = targetWorld.getBlockAt(gameX, gameY, gameZ);
            block.setBlockData(newData, false);
        }

        // Set biome in target world
        targetWorld.setBiome(gameX, gameZ, sourceBiome);

        return MergeResult.SUCCESS;
    }

    /**
     * Merges all columns that are coloured in the image.
     */
    public void mergeAll(boolean verbose) {
        getLogger().info("Starting merge. Target world: "+targetWorld.getName()+" - Image file name: "+getConfig().getString("map-image-file", "map.png"));

        int mergeCount = 0;
        int mergeSkippedCount = 0;
        int mergeFailedCount = 0;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                switch (mergeFromImagePixel(x, y)) {
                case SUCCESS:
                    if (verbose)
                        getLogger().info("Merge successful at " + x + " " + y);
                    mergeCount++;
                    break;
                case NO_WORLD:
                    getLogger().warning("Merge failed at " + x + " " + y + ", source world not found!");
                    mergeFailedCount++;
                    break;
                case NO_COLOR:
                    if (verbose)
                        getLogger().info("Merge not needed at " + x + " " + y);
                    mergeSkippedCount++;
                    break;
                case ERROR:
                    getLogger().warning("Merge failed at " + x + " " + y + ", unknown error!");
                    mergeFailedCount++;
                    break;
                }
            }

            // Save at end of each row
            targetWorld.save();
        }

        getLogger().info("Merge complete. "+mergeCount+" columns merged, "+mergeSkippedCount+" columns skipped, "+mergeFailedCount+" columns failed.");
    }
}