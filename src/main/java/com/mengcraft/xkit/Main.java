package com.mengcraft.xkit;

import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import com.mengcraft.xkit.lib.ItemUtil;
import com.mengcraft.xkit.lib.ItemUtilHandler;
import com.mengcraft.xkit.orm.EbeanHandler;

public class Main extends JavaPlugin {

    private final Holder holder = new Holder();

    private ItemUtil util;

    public Holder getHolder() {
        return holder;
    }

    public Inventory getInventory(String next) {
        return getServer().createInventory(holder, 54, next);
    }

    @Override
    public void onEnable() {
        EbeanHandler handler = new EbeanHandler(this);

        handler.define(Define.class);

        handler.setDriver("org.sqlite.JDBC");
        handler.setUrl("jdbc:sqlite:" + getDataFolder()
                + "/data.sqlite");

        handler.setUserName("mc");
        handler.setPassword("minmin");

        getDataFolder().mkdir();

        try {
            handler.initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        handler.install();
        handler.reflect();

        try {
            util = new ItemUtilHandler(this).handle();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        try {
            new Metrics(this).start();
        } catch (Exception e) {
            getLogger().warning(e.toString());
        }

        getCommand("xkit").setExecutor(new Executor(this));

        String[] strings = {
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(strings);
    }

    public ItemUtil getUtil() {
        return util;
    }

    public class Holder implements InventoryHolder {

        @Override
        public Inventory getInventory() {
            return null;
        }

    }
}
