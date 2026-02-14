package com.gmail.filoghost.customjoinitems;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VersionUtils {
    private static boolean setup;
    private static Method oldGetOnlinePlayersMethod;
    private static boolean useReflection;

    public static Collection<? extends Player> getOnlinePlayers() {
        try {
            if (!setup) {
                oldGetOnlinePlayersMethod = Bukkit.class.getDeclaredMethod("getOnlinePlayers", new Class[0]);
                if (oldGetOnlinePlayersMethod.getReturnType() == Player[].class) {
                    useReflection = true;
                }
                setup = true;
            }
            if (!useReflection) {
                return Bukkit.getOnlinePlayers();
            }
            Object[] playersArray = (Player[])oldGetOnlinePlayersMethod.invoke(null, new Object[0]);
            return Arrays.asList((Player[])playersArray);
        }
        catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}

