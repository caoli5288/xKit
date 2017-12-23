package com.mengcraft.xkit;

import com.mengcraft.xkit.entity.Kit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;

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

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        L2Pool.expire(event.getPlayer());
    }

    private void kit(HumanEntity p, Inventory inventory) {
        String title = inventory.getTitle();
        if (Main.eq(title, "礼物箱子")) {
            ItemStack[] all = StreamSupport.stream(inventory.spliterator(), false)
                    .filter(item -> !(item == null) && !(item.getType() == Material.AIR))
                    .toArray(ItemStack[]::new);
            if (all.length >= 1) {
                Collection<ItemStack> overflow = p.getInventory().addItem(all).values();
                if (!overflow.isEmpty()) {
                    Location location = p.getLocation();
                    overflow.forEach(item -> location.getWorld().dropItem(location, item));
                    Main.getMessenger().send(p, "item_overflow", ChatColor.RED + "未领取的物品已掉落脚下");
                }
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
