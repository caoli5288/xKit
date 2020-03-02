package com.mengcraft.xkit;

import com.comphenix.protocol.utility.StreamSerializer;
import com.github.skystardust.ultracore.core.PluginInstance;
import com.github.skystardust.ultracore.core.database.newgen.DatabaseManager;
import com.github.skystardust.ultracore.core.exceptions.ConfigurationException;
import com.github.skystardust.ultracore.core.exceptions.DatabaseInitException;
import com.mengcraft.xkit.entity.Kit;
import com.mengcraft.xkit.entity.KitOrder;
import com.mengcraft.xkit.entity.KitUseToken;
import com.mengcraft.xkit.util.Messenger;
import io.ebean.EbeanServer;
import io.ebean.Query;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.concurrent.CompletableFuture.runAsync;

public class KitPlugin extends JavaPlugin implements InventoryHolder, PluginInstance {


    public static final StreamSerializer SERIALIZER = new StreamSerializer();
    public static final int KIT_SIZE = 54;
    private static Messenger messenger;
    private static EbeanServer dataSource;
    private UseTokenMgr useTokenMgr;
    private DatabaseManager databaseManager;

    public static Messenger getMessenger() {
        return messenger;
    }

    public static boolean isKitView(Inventory inventory) {
        return inventory.getHolder() instanceof KitPlugin;
    }

    public static long now() {
        return Instant.now().getEpochSecond();
    }

    public static boolean eq(Object one, Object other) {
        return Objects.equals(one, other);
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

    public static boolean nil(Object any) {
        return any == null;
    }

    public static ItemStack[] itemListFrom(Kit kit) {
        List<String> list = List.class.cast(JSONValue.parse(kit.getItem()));
        List<ItemStack> i = KitPlugin.collect(list, text -> KitPlugin.decode(text));
        return i.toArray(new ItemStack[KitPlugin.KIT_SIZE]);
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

    public static EbeanServer getDataSource() {
        return dataSource;
    }

    public static void exec(Runnable runnable) {
        runAsync(runnable);
    }

    public UseTokenMgr getUseTokenMgr() {
        return useTokenMgr;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            databaseManager = DatabaseManager.newBuilder()
                    .withName("xkit")
                    .withSqlConfiguration(DatabaseManager.setupDatabase(this))
                    .withOwnerPlugin(this)
                    .withModelClass(Arrays.asList(Kit.class, KitOrder.class, KitUseToken.class))
                    .build()
                    .openConnection();
        } catch (DatabaseInitException e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + " Failed to init database.");
            getServer().getConsoleSender().sendMessage(ChatColor.RED + e.getLocalizedMessage());
            return;
        } catch (ConfigurationException e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + " Failed to init configurations.");
            getServer().getConsoleSender().sendMessage(ChatColor.RED + e.getLocalizedMessage());
            return;
        }

        dataSource = databaseManager.getEbeanServer();
        messenger = new Messenger(this);

        useTokenMgr = new UseTokenMgr(this);

        KitCommand command = new KitCommand(this);
        getCommand("xkit").setExecutor(command);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            KitPlaceholderHook hook = new KitPlaceholderHook(this);
            Formatter.setReplacePlaceholder(true);
            hook.hook();
        }

        getServer().getPluginManager().registerEvents(new KitListener(this, command), this);
    }

    @Override
    public Inventory getInventory() {
        return getInventory(null);
    }

    public Inventory getInventory(String name) {
        if (nil(name)) {
            return getServer().createInventory(this, KIT_SIZE, "礼物箱子");
        }
        return getServer().createInventory(this, KIT_SIZE, "管理模式|" + name);
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

    public <T> void exec(T in, Consumer<T> consumer) {
        exec(() -> consumer.accept(in));
    }

    public <T> Query<T> find(Class<T> type) {
        return dataSource.find(type);
    }

    public void save(Object object) {
        dataSource.save(object);
    }

    @Override
    public Logger getPluginLogger() {
        return getLogger();
    }
}
