package com.gmail.filoghost.customjoinitems;

import com.gmail.filoghost.customjoinitems.Configuration;
import com.gmail.filoghost.customjoinitems.ErrorLogger;
import com.gmail.filoghost.customjoinitems.ItemCommand;
import com.gmail.filoghost.customjoinitems.JoinItem;
import com.gmail.filoghost.customjoinitems.MetricsLite;
import com.gmail.filoghost.customjoinitems.PlayerListener;
import com.gmail.filoghost.customjoinitems.Updater;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomJoinItems
extends JavaPlugin {
    public static Plugin plugin;
    public static List<JoinItem> items;
    public static Logger logger;

    static {
        items = new ArrayList<JoinItem>();
    }

    public void onEnable() {
        plugin = this;
        logger = this.getLogger();
        Bukkit.getPluginManager().registerEvents((Listener)new PlayerListener(), (Plugin)this);
        CustomJoinItems.load();
        try {
            MetricsLite metrics = new MetricsLite((Plugin)this);
            metrics.start();
        }
        catch (Exception metrics) {
            // empty catch block
        }
        Updater.UpdaterHandler.setup(plugin, 64989, "\u00a76[\u00a7eCJItems\u00a76] ", super.getFile(), ChatColor.YELLOW, "/cji update", "custom-join-items");
        if (Configuration.updaterEnable) {
            Thread updaterThread = new Thread(new Runnable(){

                @Override
                public void run() {
                    Updater.UpdaterHandler.startupUpdateCheck();
                }
            });
            updaterThread.start();
        }
    }

    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("\u00a7c[Custom Join Items]");
            sender.sendMessage("\u00a7f/cji reload\u00a77 - Reloads the plugin");
            sender.sendMessage("\u00a7f/cji update\u00a77 - Updates the plugin");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            CustomJoinItems.load();
            sender.sendMessage("\u00a7aConfiguration reloaded!");
            return true;
        }
        if (args[0].equalsIgnoreCase("update")) {
            Thread updaterThread = new Thread(new Runnable(){

                @Override
                public void run() {
                    Updater.UpdaterHandler.manuallyCheckUpdates(sender);
                }
            });
            updaterThread.start();
            return true;
        }
        sender.sendMessage("\u00a7cUnknown command. Type /cji for help.");
        return true;
    }

    public static void load() {
        Configuration.load();
        items = new ArrayList<JoinItem>();
        plugin.saveResource("tutorial.txt", true);
        FileConfiguration itemsConfig = CustomJoinItems.loadFile("items.yml");
        Set<String> keys = itemsConfig.getKeys(false);
        for (String internalName : keys) {
            String disabledWorlds;
            ConfigurationSection itemNode = itemsConfig.getConfigurationSection(internalName);
            if (!itemNode.isSet("name")) {
                ErrorLogger.addError("The item \"" + internalName + "\" has no name!");
                continue;
            }
            if (!itemNode.isSet("id")) {
                ErrorLogger.addError("The item \"" + internalName + "\" has no ID!");
                continue;
            }
            if (itemNode.getInt("id") == 0 || Material.getMaterial(String.valueOf(itemNode.getInt("id"))) == null) {
                ErrorLogger.addError("The item \"" + internalName + "\" has an invalid item ID: " + itemNode.getInt("id") + ".");
                continue;
            }
            Material material = Material.getMaterial(String.valueOf(itemNode.getInt("id")));
            String command = itemNode.getString("command");
            String name = itemNode.getString("name");
            String permission = itemNode.getString("permission");
            Integer slot = itemNode.getInt("slot");
            Short dataValue = (short)itemNode.getInt("data-value");
            JoinItem item = new JoinItem(material);
            item.setCommands(ItemCommand.arrayFromString(command));
            item.setPerm(permission);
            item.setSlot(slot);
            item.setCustomName(name);
            item.setDataValue(dataValue);
            if (itemNode.isSet("lore") && itemNode.isList("lore")) {
                item.setLore(itemNode.getStringList("lore"));
            }
            item.setGiveOnWorldChange(itemNode.getBoolean("give-on-world-change", false));
            item.setDroppable(itemNode.getBoolean("allow-drop", false));
            item.setGiveOnRespawn(itemNode.getBoolean("give-at-respawn", true));
            item.setOnlyOnFirstJoin(itemNode.getBoolean("first-join-only", false));
            item.setBlockMovement(itemNode.getBoolean("block-movement", false));
            if (itemNode.getInt("cooldown-seconds") > 0) {
                item.setUseCooldown(true);
                item.setCooldownSeconds(itemNode.getInt("cooldown-seconds"));
            }
            if ((disabledWorlds = itemNode.getString("disabled-worlds")) != null && disabledWorlds.length() > 0) {
                List<String> disabledWorldsList = Arrays.asList(disabledWorlds.replace(" ", "").split(","));
                item.setDisabledWorlds(disabledWorldsList);
            }
            items.add(item);
        }
        ErrorLogger.printErrors();
    }

    public static FileConfiguration loadFile(String path) {
        File file;
        if (!path.endsWith(".yml")) {
            path = String.valueOf(path) + ".yml";
        }
        if (!(file = new File(plugin.getDataFolder(), path)).exists()) {
            try {
                plugin.saveResource(path, false);
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.warning("Cannot save " + path + " to disk!");
                return null;
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)file);
        return config;
    }
}

