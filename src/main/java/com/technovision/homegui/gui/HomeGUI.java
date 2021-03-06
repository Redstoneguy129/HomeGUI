package com.technovision.homegui.gui;

import com.technovision.homegui.Homegui;
import com.technovision.homegui.playerdata.EssentialsReader;
import com.technovision.homegui.playerdata.Home;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class HomeGUI implements InventoryHolder, Listener {

    public static List<String> activeGui = new ArrayList<String>();
    public static Map<String, List<Home>> allHomes = new HashMap<>();

    private Inventory inv;
    private List<Home> homes;

    public HomeGUI(UUID playerUUID) {
        Bukkit.getServer().getPluginManager().registerEvents(this, Homegui.PLUGIN);
        EssentialsReader reader = new EssentialsReader(playerUUID.toString());
        homes = reader.getHomes();
        String title = Homegui.PLUGIN.getConfig().getString("gui-main-header").replace('&', '§');
        title = title.replace("§8", "");
        inv = Bukkit.createInventory(this, calculateSize(), title);
        allHomes.put(playerUUID.toString(), homes);
        Homegui.dataReader.create(playerUUID.toString());
        initItems(playerUUID.toString());
    }

    private int calculateSize() {
        int size = homes.size();
        if (size == 0) {return 9;}
        if (size >= 54) { return 54; }
        if (size % 9 == 0) { return size; }
        int count = 0;
        for (int i = size%9; i>0; i--) {
            count++;
        }
        return (size - count) + 9;
    }

    private void initItems(String playerID) {
        String name;
        for (Home home : homes) {
            name = home.getName().substring(0, 1).toUpperCase() + home.getName().substring(1);
            Material icon = Homegui.dataReader.getIcon(playerID, home.getName());
            String nameColor = Homegui.PLUGIN.getConfig().getString("home-color").replace("&", "§");
            name = nameColor + name;
            List<String> lore = Homegui.PLUGIN.getConfig().getStringList("home-lore");
            String location = home.getX() + "x§7,§f " + home.getY() + "y§7,§f " + home.getZ() + "z";
            for (int i = 0; i < lore.size(); i++) {
                String newLine = lore.get(i).replace("{location}", location);
                newLine = newLine.replace("{world}", home.getWorld());
                if (newLine.contains("&")) {
                    newLine = newLine.replace("&", "§");
                } else {
                    newLine = "§f" + newLine;
                }
                lore.set(i, newLine);
            }
            inv.addItem(createGuiItem(icon, name, lore));
        }
    }

    private ItemStack createGuiItem(final Material material, final String name, final List<String> lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
        activeGui.add(player.getName());
    }

    @EventHandler
    public void onGuiActivation(InventoryClickEvent event){
        if (event.getClickedInventory() == null) { return; }
        Player player = (Player) event.getWhoClicked();
        if (activeGui.contains(player.getName()) && event.getCurrentItem() != null) {
            event.setCancelled(true);
            if (event.getClickedInventory().getType() == InventoryType.PLAYER) { return; }
            String playerID = player.getUniqueId().toString();
            int slotNum = event.getSlot();
            String name = allHomes.get(playerID).get(slotNum).getName();
            //Left Click
            if (event.isLeftClick()) {
                player.performCommand("essentials:home " + name);
                player.closeInventory();
                //Middle Click
            } else if (event.getAction() == InventoryAction.CLONE_STACK) {
                player.performCommand("essentials:delhome " + name);
                Homegui.dataReader.removeIcon(playerID, name);
                player.closeInventory();
                //Right Click
            } else if (event.isRightClick()) {
                player.closeInventory();
                ChangeIconGUI iconGUI = new ChangeIconGUI();
                iconGUI.openInventory(player, allHomes.get(playerID).get(slotNum));
            }
        }
    }

    @EventHandler
    public void onGuiClosing(InventoryCloseEvent event){
        Player player = (Player) event.getPlayer();
        if (activeGui.contains(player.getName())) {
            activeGui.remove(player.getName());
        }
    }


    @Override
    public Inventory getInventory() {
        return inv;
    }
}
