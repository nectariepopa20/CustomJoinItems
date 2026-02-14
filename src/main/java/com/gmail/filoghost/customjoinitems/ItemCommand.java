package com.gmail.filoghost.customjoinitems;

import com.gmail.filoghost.customjoinitems.AsciiSymbols;
import com.gmail.filoghost.customjoinitems.CustomJoinItems;
import com.gmail.filoghost.customjoinitems.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ItemCommand {
    private String command;
    private Type type;

    private ItemCommand(String command, Type type) {
        this.command = command;
        this.type = type;
    }

    public String getCommand() {
        return this.command;
    }

    public static ItemCommand[] arrayFromString(String input) {
        if (input == null || input.length() == 0) {
            return new ItemCommand[]{new ItemCommand("", Type.DEFAULT)};
        }
        String[] commandStrings = input.split(";");
        ItemCommand[] commands = new ItemCommand[commandStrings.length];
        int i = 0;
        while (i < commandStrings.length) {
            commands[i] = ItemCommand.fromString(commandStrings[i].trim());
            ++i;
        }
        return commands;
    }

    public static ItemCommand fromString(String input) {
        if (input == null || input.length() == 0) {
            return new ItemCommand("", Type.DEFAULT);
        }
        input = input.trim();
        Type type = Type.DEFAULT;
        if (input.startsWith("console:")) {
            input = input.substring(8);
            type = Type.CONSOLE;
        }
        if (input.startsWith("op:")) {
            input = input.substring(3);
            type = Type.OP;
        }
        if (input.startsWith("server:")) {
            input = input.substring(7);
            type = Type.SERVER;
        }
        if (input.startsWith("tell:")) {
            input = input.substring(5);
            type = Type.TELL;
        }
        input = input.trim();
        input = Utils.color(input);
        input = AsciiSymbols.placeholdersToSymbols(input);
        return new ItemCommand(input, type);
    }

    public void execute(Player player) {
        if (this.command == null || this.command.length() == 0) {
            return;
        }
        if (this.command.contains("%player%")) {
            this.command = this.command.replace("%player%", player.getName());
        }
        if (this.command.contains("%world%")) {
            this.command = this.command.replace("%world%", player.getWorld().getName());
        }
        switch (this.type) {
            case CONSOLE: {
                Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)this.command);
                break;
            }
            case OP: {
                boolean isOp = player.isOp();
                player.setOp(true);
                try {
                    player.chat("/" + this.command);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    player.setOp(isOp);
                }
                catch (Exception danger) {
                    danger.printStackTrace();
                    player.setOp(false);
                    CustomJoinItems.logger.severe("An exception has occurred while removing " + player.getName() + " from OPs, while executing a command. OP or not, he was removed from OPs!");
                }
                break;
            }
            case SERVER: {
                Utils.connectToBungeeServer(player, this.command);
                break;
            }
            case TELL: {
                player.sendMessage(this.command);
                break;
            }
            default: {
                player.chat("/" + this.command);
            }
        }
    }

    public static enum Type {
        DEFAULT,
        CONSOLE,
        OP,
        SERVER,
        TELL;

    }
}

