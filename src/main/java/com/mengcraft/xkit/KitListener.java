package com.mengcraft.xkit;

import com.mengcraft.xkit.entity.Kit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 16-9-23.
 */
public class KitListener implements Listener {

    private final KitCommand command;
    private final Main main;

    public KitListener(Main main, KitCommand command) {
        this.main = main;
        this.command = command;
    }

    @EventHandler
    public void handle(InventoryCloseEvent event) {
        Inventory pak = event.getInventory();
        if (Main.isKitView(pak)) {
            kit(event.getPlayer(), pak);
        }
    }

    private void kit(HumanEntity p, Inventory inventory) {
        String title = inventory.getTitle();
        if (Main.eq(title, "礼物箱子")) {
            List<ItemStack> list = new ArrayList<>(Main.KIT_SIZE);
            inventory.forEach(item -> {
                if (!Main.nil(item) && item.getTypeId() > 0) list.add(item);
            });
            if (!list.isEmpty()) {
                Location location = p.getLocation();
                list.forEach(item -> location.getWorld().dropItem(location, item));
                p.sendMessage(ChatColor.RED + "未领取的物品已掉落脚下");
            }
            inventory.clear();
        } else {
            String name = title.substring(5, title.length());
            Kit kit = command.fetch(name, true);
            if (Main.nil(kit)) {
                throw new IllegalStateException("喵喵喵");
            } else {
                List<String> list = Main.collect(Arrays.asList(inventory.getContents()), item -> {
                    if (!Main.nil(item) && item.getTypeId() > 0) {
                        return Main.encode(item);
                    }
                    return null;
                });

                if (list.isEmpty()) {
                    kit.setItem(null);
                } else {
                    kit.setItem(JSONValue.toJSONString(list));
                }
                main.exec(kit, main::save);

                p.sendMessage(ChatColor.GREEN + "操作已完成");
            }
        }
    }

}
