package com.gmail.filoghost.customjoinitems;

import com.gmail.filoghost.customjoinitems.CustomJoinItems;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class ErrorLogger {
    private static List<String> errors = new ArrayList<String>();

    public static void addError(String error) {
        if (error == null || error.length() == 0) {
            return;
        }
        errors.add(ChatColor.stripColor((String)error));
    }

    public static void printErrors() {
        if (errors.size() == 0) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(CustomJoinItems.plugin, new Runnable(){

            @Override
            public void run() {
                System.out.println("----------------------------------------------------------");
                for (String error : errors) {
                    CustomJoinItems.logger.info(error);
                }
                System.out.println("----------------------------------------------------------");
                errors = new ArrayList();
            }
        }, 1L);
    }
}

