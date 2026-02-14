package com.gmail.filoghost.customjoinitems;

import com.gmail.filoghost.customjoinitems.CustomJoinItems;
import org.bukkit.configuration.file.FileConfiguration;

public class Configuration {
    public static boolean updaterEnable;
    public static boolean clearInventoryOnJoin;

    public static void load() {
        CustomJoinItems.plugin.saveDefaultConfig();
        CustomJoinItems.plugin.reloadConfig();
        FileConfiguration config = CustomJoinItems.plugin.getConfig();
        Nodes[] nodesArray = Nodes.values();
        int n = nodesArray.length;
        int n2 = 0;
        while (n2 < n) {
            Nodes n3 = nodesArray[n2];
            if (!config.isSet(n3.path)) {
                config.set(n3.path, n3.value);
            }
            ++n2;
        }
        CustomJoinItems.plugin.saveConfig();
        updaterEnable = config.getBoolean(Nodes.UPDATE_NOTIFICATIONS.path);
        clearInventoryOnJoin = config.getBoolean(Nodes.CLEAR_INVENTORY_ON_JOIN.path);
    }

    public static enum Nodes {
        UPDATE_NOTIFICATIONS("update-notifications", true),
        CLEAR_INVENTORY_ON_JOIN("clear-inventory-on-join", false);

        public String path;
        public Object value;

        private Nodes(String path, Object value) {
            this.path = path;
            this.value = value;
        }
    }
}

