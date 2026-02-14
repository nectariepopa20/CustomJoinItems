package com.gmail.filoghost.customjoinitems;

import com.gmail.filoghost.customjoinitems.CustomJoinItems;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

public class Utils {
    public static String color(String input) {
        if (input == null) {
            return null;
        }
        return ChatColor.translateAlternateColorCodes((char)'&', (String)input);
    }

    public static String addDefaultColor(String input) {
        if (input == null) {
            return null;
        }
        if (!input.startsWith("\u00a7")) {
            input = "\u00a7f" + input;
        }
        return input;
    }

    public static boolean connectToBungeeServer(Player player, String server) {
        block4: {
            try {
                Messenger messenger = Bukkit.getMessenger();
                if (!messenger.isOutgoingChannelRegistered(CustomJoinItems.plugin, "BungeeCord")) {
                    messenger.registerOutgoingPluginChannel(CustomJoinItems.plugin, "BungeeCord");
                }
                if (server.length() != 0) break block4;
                player.sendMessage("\u00a7cTarget server was \"\" (empty string) cannot connect to it.");
                return false;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                player.sendMessage("\u00a7cAn exception has occurred. Please notify OPs about this. (They should look at the console).");
                CustomJoinItems.logger.warning("Could not handle BungeeCord command from " + player.getName() + ": tried to connect to \"" + server + "\".");
                return false;
            }
        }
        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteArray);
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(CustomJoinItems.plugin, "BungeeCord", byteArray.toByteArray());
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            player.sendMessage("\u00a7cAn exception has occurred. Please notify OPs about this. (They should look at the console).");
            CustomJoinItems.logger.warning("Could not send BungeeCord connect message: " + ex.getMessage());
            return false;
        }
    }
}

