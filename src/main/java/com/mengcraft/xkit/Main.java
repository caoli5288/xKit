package com.mengcraft.xkit;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Query;
import com.comphenix.protocol.utility.StreamSerializer;
import com.mengcraft.simpleorm.DatabaseException;
import com.mengcraft.simpleorm.EbeanHandler;
import com.mengcraft.simpleorm.EbeanManager;
import com.mengcraft.xkit.entity.Kit;
import com.mengcraft.xkit.entity.KitOrder;
import com.mengcraft.xkit.entity.KitUseToken;
import com.mengcraft.xkit.util.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.runAsync;

public class Main extends JavaPlugin implements InventoryHolder {


    private static EbeanServer dataSource;
    private static Messenger messenger;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EbeanHandler db = EbeanManager.DEFAULT.getHandler(this);
        if (db.isNotInitialized()) {
            db.define(Kit.class);
            db.define(KitOrder.class);
            db.define(KitUseToken.class);
            try {
                db.initialize();
            } catch (DatabaseException e) {
                throw new RuntimeException("db");
            }
        }
        db.install(true);
//        db.reflect();
        dataSource = db.getServer();
        messenger = new Messenger(this);

        exec(() -> new Metrics(this).start());

        KitCommand command = new KitCommand(this);
        getCommand("xkit").setExecutor(command);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            KitPlaceholderHook hook = new KitPlaceholderHook(this);
            hook.hook();
        }

        getServer().getPluginManager().registerEvents(new KitListener(this, command), this);
    }

    public static Messenger getMessenger() {
        return messenger;
    }

    public static boolean isKitView(Inventory inventory) {
        return inventory.getHolder() instanceof Main;
    }

    public Inventory getInventory(String name) {
        if (nil(name)) {
            return getServer().createInventory(this, KIT_SIZE, "礼物箱子");
        }
        return getServer().createInventory(this, KIT_SIZE, "管理模式|" + name);
    }

    public static EbeanServer getDataSource() {
        return dataSource;
    }

    @Override
    public Inventory getInventory() {
        return getInventory(null);
    }

    public void dispatch(String command) {
        getServer().dispatchCommand(getServer().getConsoleSender(), command);
    }

    public <T> void consume(Callable<T> callable, Consumer<T> consumer) {
        exec(() -> {
            try {
                T result = callable.call();
                run(() -> consumer.accept(result));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void run(Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }

    public static void exec(Runnable runnable) {
        runAsync(runnable);
    }

    public <T> void exec(T in, Consumer<T> consumer) {
        exec(() -> consumer.accept(in));
    }

    public <T> Query<T> find(Class<T> type) {
        return dataSource.find(type);
    }

    public void save(Object object) {
        dataSource.save(object);
    }

    public static long now() {
        return Instant.now().getEpochSecond();
    }

    public static boolean nil(Object any) {
        return any == null;
    }

    public static boolean eq(Object i, Object j) {
        return i == j || (i != null && i.equals(j));
    }

    public static <T, E> List<T> collect(List<E> in, Function<E, T> func) {
        List<T> out = new ArrayList<>(in.size());
        for (E i : in) {
            T ref = func.apply(i);
            if (!nil(ref)) {// Extra not null check
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
        return !(nil(kit.getItem()) && nil(kit.getCommand()));
    }

    public static ItemStack[] itemListFrom(Kit kit) {
        List<String> list = List.class.cast(JSONValue.parse(kit.getItem()));
        List<ItemStack> i = Main.collect(list, text -> Main.decode(text));
        return i.toArray(new ItemStack[Main.KIT_SIZE]);
    }

    public static final StreamSerializer SERIALIZER = new StreamSerializer();
    public static final int KIT_SIZE = 54;
}
