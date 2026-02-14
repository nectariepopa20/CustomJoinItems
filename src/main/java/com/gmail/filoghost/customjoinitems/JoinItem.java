package com.gmail.filoghost.customjoinitems;

import com.gmail.filoghost.customjoinitems.CustomJoinItems;
import com.gmail.filoghost.customjoinitems.ItemCommand;
import com.gmail.filoghost.customjoinitems.Utils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class JoinItem {
    private Material material;
    private ItemCommand[] commands;
    private Short dataValue = null;
    private String customName = null;
    private String permission = null;
    private Integer slot = null;
    private boolean onlyOnFirstJoin = false;
    private boolean blockMovement = false;
    private boolean droppable = false;
    private boolean giveOnRespawn = true;
    private List<String> lore;
    private int cooldownSeconds = 0;
    private boolean useCooldown = false;
    private List<String> playersInCooldown = new ArrayList<String>();
    private boolean giveOnWorldChange;
    private List<String> removeInWorlds = new ArrayList<String>();
    /** Unique ID used for CustomModelData so we can match items reliably on Paper/Leaf (remapping). */
    private int matchId = -1;

    public JoinItem(Material mat) {
        this.material = mat;
    }

    public String getCustomName() {
        return this.customName;
    }

    public void setCustomName(String customName) {
        if (customName == null || customName.length() == 0) {
            this.customName = null;
            return;
        }
        this.customName = Utils.addDefaultColor(Utils.color(customName));
    }

    public void setLore(List<String> lore) {
        if (lore == null || lore.size() == 0) {
            this.lore = null;
            return;
        }
        this.lore = new ArrayList<String>();
        for (String s : lore) {
            s = Utils.color(s);
            s = Utils.addDefaultColor(s);
            this.lore.add(s);
        }
    }

    public void setSlot(Integer slot) {
        if (slot == null || slot == 0) {
            this.slot = null;
            return;
        }
        if (slot < 1) {
            slot = 1;
        }
        if (slot > 9) {
            slot = 9;
        }
        this.slot = slot = Integer.valueOf(slot - 1);
    }

    public void setPerm(String permission) {
        this.permission = permission == null || permission.length() == 0 ? null : permission;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public boolean isSimilar(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && this.matchId >= 0 && meta.hasCustomModelData() && meta.getCustomModelData() == this.matchId) {
            return true;
        }
        // Fallback: compare by material (by key on remapped servers) and display name
        Material itemType = item.getType();
        if (itemType != this.material) {
            try {
                if (!itemType.getKey().getKey().equals(this.material.getKey().getKey())) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        if (meta == null) {
            return this.customName == null && this.lore == null;
        }
        if (this.customName == null) {
            if (meta.hasDisplayName()) {
                return false;
            }
        } else {
            if (!meta.hasDisplayName()) {
                return false;
            }
            String itemName = meta.getDisplayName();
            if (!this.customName.equals(itemName)) {
                String plainOurs = ChatColor.stripColor(this.customName);
                String plainTheirs = ChatColor.stripColor(itemName);
                if (plainOurs == null || plainTheirs == null || !plainOurs.equals(plainTheirs)) {
                    return false;
                }
            }
        }
        return this.dataValue == null || item.getDurability() == this.dataValue.shortValue();
    }

    public void removeFrom(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        int i = 0;
        while (i < contents.length) {
            if (this.isSimilar(contents[i])) {
                inv.setItem(i, new ItemStack(Material.AIR));
            }
            ++i;
        }
        if (this.isSimilar(inv.getHelmet())) {
            inv.setHelmet(new ItemStack(Material.AIR));
        }
        if (this.isSimilar(inv.getChestplate())) {
            inv.setChestplate(new ItemStack(Material.AIR));
        }
        if (this.isSimilar(inv.getLeggings())) {
            inv.setLeggings(new ItemStack(Material.AIR));
        }
        if (this.isSimilar(inv.getBoots())) {
            inv.setBoots(new ItemStack(Material.AIR));
        }
    }

    public void giveTo(Player player, boolean notifyFailure) {
        ItemStack itemStack;
        PlayerInventory inv = player.getInventory();
        if (inv.firstEmpty() == -1) {
            if (notifyFailure) {
                player.sendMessage("\u00a7cYour inventory is full.");
            }
            return;
        }
        ItemStack[] itemStackArray = inv.getContents();
        int n = itemStackArray.length;
        int n2 = 0;
        while (n2 < n) {
            itemStack = itemStackArray[n2];
            if (this.isSimilar(itemStack)) {
                if (notifyFailure) {
                    player.sendMessage("\u00a7cYour already own this item.");
                }
                return;
            }
            ++n2;
        }
        itemStackArray = inv.getArmorContents();
        n = itemStackArray.length;
        n2 = 0;
        while (n2 < n) {
            itemStack = itemStackArray[n2];
            if (this.isSimilar(itemStack)) {
                if (notifyFailure) {
                    player.sendMessage("\u00a7cYour already own this item.");
                }
                return;
            }
            ++n2;
        }
        ItemStack item = new ItemStack(this.material);
        if (this.dataValue != null) {
            item.setDurability(this.dataValue.shortValue());
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (this.matchId >= 0) {
                meta.setCustomModelData(this.matchId);
            }
            if (this.customName != null) {
                meta.setDisplayName(this.customName);
            }
            if (this.lore != null) {
                meta.setLore(this.lore);
            }
            item.setItemMeta(meta);
        }
        if (this.slot != null) {
            ItemStack previous = inv.getItem(this.slot.intValue());
            inv.setItem(this.slot.intValue(), item);
            if (previous != null) {
                inv.addItem(new ItemStack[]{previous});
            }
        } else {
            inv.addItem(new ItemStack[]{item});
        }
    }

    public void executeCommands(Player player) {
        if (this.commands != null && this.commands.length > 0) {
            if (this.useCooldown) {
                if (this.playersInCooldown.contains(player.getName().toLowerCase())) {
                    player.sendMessage("\u00a7cPlease wait before clicking again.");
                    return;
                }
                this.addCooldown(player);
            }
            ItemCommand[] itemCommandArray = this.commands;
            int n = this.commands.length;
            int n2 = 0;
            while (n2 < n) {
                ItemCommand itemCommand = itemCommandArray[n2];
                itemCommand.execute(player);
                ++n2;
            }
        }
    }

    public boolean hasPerm(Player player) {
        if (this.permission == null) {
            return true;
        }
        return player.hasPermission(this.permission);
    }

    public boolean isOnlyOnFirstJoin() {
        return this.onlyOnFirstJoin;
    }

    public void setOnlyOnFirstJoin(boolean onlyOnFirstJoin) {
        this.onlyOnFirstJoin = onlyOnFirstJoin;
        if (onlyOnFirstJoin && this.giveOnRespawn) {
            this.giveOnRespawn = false;
        }
    }

    public void setDataValue(Short dataValue) {
        if (dataValue == null || dataValue == 0) {
            this.dataValue = null;
            return;
        }
        this.dataValue = dataValue;
    }

    public boolean isDroppable() {
        return this.droppable;
    }

    public void setDroppable(boolean droppable) {
        this.droppable = droppable;
    }

    public boolean isGiveOnRespawn() {
        return this.giveOnRespawn;
    }

    public void setGiveOnRespawn(boolean giveOnRespawn) {
        if (this.onlyOnFirstJoin) {
            return;
        }
        this.giveOnRespawn = giveOnRespawn;
        this.isGiveOnRespawn();
    }

    public void setCommands(ItemCommand[] commands) {
        this.commands = commands;
    }

    public void setBlockMovement(boolean block) {
        this.blockMovement = block;
    }

    public boolean isMovementBlocked() {
        return this.blockMovement;
    }

    public void setUseCooldown(boolean use) {
        this.useCooldown = use;
    }

    public void setCooldownSeconds(int seconds) {
        this.cooldownSeconds = seconds;
    }

    public boolean usesCooldown() {
        return this.useCooldown;
    }

    public void setGiveOnWorldChange(boolean b) {
        this.giveOnWorldChange = b;
    }

    public boolean isGiveOnWorldChange() {
        return this.giveOnWorldChange;
    }

    public boolean isAllowedInWorld(String s) {
        return !this.removeInWorlds.contains(s);
    }

    public void setDisabledWorlds(List<String> list) {
        if (list != null) {
            this.removeInWorlds = list;
        }
    }

    public void addCooldown(Player player) {
        final String name = player.getName().toLowerCase();
        if (this.playersInCooldown.contains(name)) {
            return;
        }
        this.playersInCooldown.add(name);
        Bukkit.getScheduler().scheduleSyncDelayedTask(CustomJoinItems.plugin, new Runnable(){

            @Override
            public void run() {
                JoinItem.this.playersInCooldown.remove(name);
            }
        }, (long)(this.cooldownSeconds * 20));
    }
}

