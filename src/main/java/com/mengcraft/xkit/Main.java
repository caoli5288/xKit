package com.mengcraft.xkit;

import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import com.mengcraft.xkit.entity.KitDefine;
import com.mengcraft.xkit.entity.KitPlayerEvent;
import com.mengcraft.xkit.lib.ItemUtil;
import com.mengcraft.xkit.lib.ItemUtilHandler;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private final InventoryHolder holder = (() -> null);

    @Override
    public void onEnable() {
        EbeanHandler handler = EbeanManager.DEFAULT.getHandler(this);
        if (!handler.isInitialized()) {
            handler.define(KitDefine.class);
            handler.define(KitPlayerEvent.class);
            try {
                handler.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        handler.install();
        handler.reflect();

        try {
            new Executor(this, new ItemUtilHandler(this).handle()).bind();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            new Metrics(this).start();
        } catch (Exception e) {
            getLogger().warning(e.toString());
        }

        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(strings);
    }

    public InventoryHolder getHolder() {
        return holder;
    }

    public Inventory getInventory(String next) {
        return getServer().createInventory(holder, 54, next);
    }

    public void execute(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

}
