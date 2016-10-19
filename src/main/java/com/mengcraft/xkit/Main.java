package com.mengcraft.xkit;

import com.avaje.ebean.Query;
import com.comphenix.protocol.utility.StreamSerializer;
import com.mengcraft.simpleorm.DatabaseException;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import com.mengcraft.xkit.entity.Kit;
import com.mengcraft.xkit.entity.KitOrder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class Main extends JavaPlugin implements InventoryHolder {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EbeanHandler db = EbeanManager.DEFAULT.getHandler(this);
        if (db.isNotInitialized()) {
            db.define(Kit.class);
            db.define(KitOrder.class);
            try {
                db.initialize();
            } catch (DatabaseException e) {
                throw new RuntimeException("db");
            }
        }
        db.install();
        db.reflect();

        execute(() -> new Metrics(this).start());

        KitCommand command = new KitCommand(this);
        getCommand("xkit").setExecutor(command);

        getServer().getPluginManager().registerEvents(new KitListener(this, command), this);
    }

    public static boolean isKitView(Inventory inventory) {
        return inventory.getHolder() instanceof Main;
    }

    public Inventory getInventory(String name) {
        if (eq(name, null)) {
            return getServer().createInventory(this, KIT_SIZE, "礼物箱子");
        }
        return getServer().createInventory(this, KIT_SIZE, "管理模式|" + name);
    }

    @Override
    public Inventory getInventory() {
        return getInventory(null);
    }

    public void dispatch(String command) {
        getServer().dispatchCommand(getServer().getConsoleSender(), command);
    }

    public <T> void process(Callable<T> callable, Consumer<T> consumer) {
        execute(() -> {
            try {
                T result = callable.call();
                process(() -> consumer.accept(result));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void process(Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }

    public void execute(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    public <T> void execute(T in, Consumer<T> consumer) {
        execute(() -> consumer.accept(in));
    }

    public <T> Query<T> find(Class<T> type) {
        return getDatabase().find(type);
    }

    public void save(Object object) {
        getDatabase().save(object);
    }

    public static int unixTime() {
        return Math.toIntExact(System.currentTimeMillis() / 1000);
    }

    public static boolean eq(Object i, Object j) {
        return i == j || (i != null && i.equals(j));
    }

    public static <T, E> List<T> collect(List<E> in, Function<E, T> func) {
        List<T> out = new ArrayList<>(in.size());
        for (E i : in) {
            T ref = func.apply(i);
            if (!eq(ref, null)) {
                out.add(ref);
            }
        }
        return out;
    }

    public static ItemStack decode(String in) {
        try {
            return SERIALIZER.deserializeItemStack(in);
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }
        return null;
    }

    public static String encode(ItemStack item) {
        try {
            return SERIALIZER.serializeItemStack(item);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean valid(Kit kit) {
        return kit.hasItem() || kit.hasCommand();
    }

    public static ItemStack[] getItemList(Kit kit) {
        List<String> list = List.class.cast(JSONValue.parse(kit.getItem()));
        List<ItemStack> i = Main.collect(list, text -> {
            return Main.decode(text);
        });
        return i.toArray(new ItemStack[Main.KIT_SIZE]);
    }

    public static final StreamSerializer SERIALIZER = new StreamSerializer();
    public static final int KIT_SIZE = 54;

}
