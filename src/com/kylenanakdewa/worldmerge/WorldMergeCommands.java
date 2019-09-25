package com.kylenanakdewa.worldmerge;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * WorldMerge Commands
 *
 * @author Kyle Nanakdewa
 */
public final class WorldMergeCommands implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Version command
        if (args.length == 0 || args[0].equalsIgnoreCase("version")) {
            sender.sendMessage(ChatColor.RED + "WorldMerge " + WorldMergePlugin.getPlugin().getDescription().getVersion() + " by Kyle Nanakdewa");
            sender.sendMessage(ChatColor.GRAY + "- Merges worlds together, using an image as a template");
            sender.sendMessage(ChatColor.GRAY + "- Website: http://Akenland.com/plugins");
            return true;
        }

        // Start command
        if (args.length >= 1 && args[0].equalsIgnoreCase("start")) {
            boolean verbose = args.length == 2 && args[1].equalsIgnoreCase("verbose");
            WorldMergePlugin.getPlugin().mergeAll(verbose);
            sender.sendMessage(ChatColor.RED+ "World merging has started. Check the console for current status.");
            return true;
        }

        // Reload command
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            WorldMergePlugin.getPlugin().prepareMerge();
            sender.sendMessage(ChatColor.RED + "WorldMerge reloaded. Check console for errors, then start the merge.");
            return true;
        }

        // Invalid command
        sender.sendMessage(ChatColor.RED + "Invalid arguments.");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Main command - return each sub-command
        if (args.length <= 1)
            return Arrays.asList("start", "reload", "version");
        // Otherwise return nothing
        return Arrays.asList("");
    }

}