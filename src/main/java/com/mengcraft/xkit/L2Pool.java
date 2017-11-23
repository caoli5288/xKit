package com.mengcraft.xkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mengcraft.xkit.entity.Kit;
import com.mengcraft.xkit.entity.KitOrder;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.TimeUnit;

public enum L2Pool {

    INST;

    private final Cache<String, Object> pool = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.HOURS)
            .build();

    public static void expire(String key) {
        INST.pool.invalidate(key);
    }

    public static void put(@NonNull String key, @NonNull Object input) {
        INST.pool.put(key, input);
    }

    public static void expire(Kit kit) {
        expire("kit:name:" + kit.getName());
    }

    @SneakyThrows
    public static Kit kitByName(String name) {
        return valid(INST.pool.get("kit:name:" + name, () -> {
            Kit kit = Main.getPool().find(Kit.class)
                    .where("name = :name")
                    .setParameter("name", name)
                    .findUnique();
            if (kit == null) {
                return INVALID;
            }
            return kit;
        }));
    }

    public static void expire(Player p) {
        Object[] array = INST.pool.asMap().keySet().toArray();
        String id = String.valueOf(p.getUniqueId());
        for (Object el : array) {
            String key = String.valueOf(el);
            if (key.startsWith(id)) expire(key);
        }
    }

    public static void put(KitOrder order) {
        put(order.getPlayer() + ":" + order.getKitId(), order);
    }

    @SneakyThrows
    public static KitOrder orderBy(Player p, Kit kit) {
        return valid(INST.pool.get(p.getUniqueId() + ":" + kit.getId(), () -> {
            List<?> order = Main.getPool().find(KitOrder.class)
                    .where("player = :player and kit_id = :kit_id")
                    .setParameter("player", p.getUniqueId())
                    .setParameter("kit_id", kit.getId())
                    .orderBy("time desc")
                    .setMaxRows(1)
                    .findList();
            return order.isEmpty() ? INVALID : order.iterator().next();
        }));
    }

    static <T> T valid(Object input) {
        return input == INVALID ? null : (T) input;
    }

    public static final Object INVALID = new Object();
}
