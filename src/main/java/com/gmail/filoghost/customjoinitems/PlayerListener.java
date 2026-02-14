package com.gmail.filoghost.customjoinitems;

import com.gmail.filoghost.customjoinitems.Configuration;
import com.gmail.filoghost.customjoinitems.CustomJoinItems;
import com.gmail.filoghost.customjoinitems.JoinItem;
import com.gmail.filoghost.customjoinitems.Updater;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener
implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (Configuration.clearInventoryOnJoin) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
        for (JoinItem item : CustomJoinItems.items) {
            if (!item.hasPerm(player) || !item.isAllowedInWorld(world) || item.isOnlyOnFirstJoin() && player.hasPlayedBefore()) continue;
            item.giveTo(player, false);
        }
        Updater.UpdaterHandler.notifyIfUpdateWasFound(player, "customjoinitems.admin");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Iterator iter = event.getDrops().iterator();
        while (iter.hasNext()) {
            ItemStack next = (ItemStack)iter.next();
            for (JoinItem item : CustomJoinItems.items) {
                if (!item.isSimilar(next)) continue;
                iter.remove();
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(CustomJoinItems.plugin, new Runnable(){

            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                String world = player.getWorld().getName();
                for (JoinItem item : CustomJoinItems.items) {
                    if (!item.hasPerm(player) || !item.isGiveOnRespawn() || !item.isAllowedInWorld(world)) continue;
                    item.giveTo(player, false);
                }
            }
        }, 1L);
    }

    @EventHandler(ignoreCancelled=true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack drop = event.getItemDrop().getItemStack();
        for (JoinItem item : CustomJoinItems.items) {
            if (!item.isSimilar(drop) || item.isDroppable()) continue;
            event.setCancelled(true);
            final Player player = event.getPlayer();
            Bukkit.getScheduler().scheduleSyncDelayedTask(CustomJoinItems.plugin, new Runnable(){

                @Override
                public void run() {
                    player.updateInventory();
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack itemInHand = event.getItem();
            for (JoinItem item : CustomJoinItems.items) {
                if (!item.isSimilar(itemInHand)) continue;
                event.setCancelled(true);
                item.executeCommands(event.getPlayer());
                event.getPlayer().updateInventory();
            }
        }
    }

    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent event) {
        final Player whoSwitched = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(CustomJoinItems.plugin, new Runnable(){

            @Override
            public void run() {
                if (!whoSwitched.isOnline()) {
                    return;
                }
                String world = whoSwitched.getWorld().getName();
                for (JoinItem item : CustomJoinItems.items) {
                    if (!item.isAllowedInWorld(world)) {
                        item.removeFrom(whoSwitched);
                        continue;
                    }
                    if (!item.isGiveOnWorldChange() || !item.hasPerm(whoSwitched)) continue;
                    item.giveTo(whoSwitched, false);
                }
            }
        }, 1L);
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }
        for (JoinItem joinItem : CustomJoinItems.items) {
            if (!joinItem.isSimilar(item) || !joinItem.isMovementBlocked()) continue;
            event.setCancelled(true);
        }
    }
}

