package com.mengcraft.xkit;

import com.avaje.ebean.EbeanServer;
import com.mengcraft.xkit.entity.KitUseToken;
import lombok.Data;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.HashMap;
import java.util.Map;

import static com.mengcraft.xkit.Main.nil;

public class UseTokenMgr {

    public static UseTokenWrapper load(Player p) {
        return L2Pool.load(String.valueOf(p.getUniqueId()) + ":use_token", () -> {
            EbeanServer db = Main.getDataSource();
            KitUseToken useToken = db.find(KitUseToken.class, p.getUniqueId());
            if (useToken == null || useToken.getUseToken() == null) {
                return new UseTokenWrapper(new HashMap<>());
            }
            return new UseTokenWrapper((Map<String, Integer>) JSONValue.parse(useToken.getUseToken()));
        });
    }

    public static void supply(Player p, String token, int amount) {
        UseTokenWrapper load = load(p);
        Integer val = load.all.get(token);
        if (nil(val)) {
            load.all.put(token, amount);
        } else {
            load.all.put(token, amount + val);
        }
        save(p, load);
    }

    public static boolean consume(Player p, String useToken) {
        UseTokenWrapper load = load(p);

        Integer val = load.all.get(useToken);
        if (nil(val)) {
            return false;
        }

        if (val-- < 1) {
            return false;
        }

        load.all.put(useToken, val);
        save(p, load);

        return true;
    }

    public static void save(Player p, UseTokenWrapper wrapper) {
        EbeanServer db = Main.getDataSource();
        int result = db.createUpdate(KitUseToken.class, "update kit_use_token set use_token = :token where id = :id;")
                .setParameter("token", JSONObject.toJSONString(wrapper.all))
                .setParameter("id", p.getUniqueId())
                .execute();
        if (result < 1) {
            db.createUpdate(KitUseToken.class, "insert into kit_use_token set id = :id, name = :name, use_token = :token;")
                    .setParameter("id", p.getUniqueId())
                    .setParameter("name", p.getName())
                    .setParameter("token", JSONObject.toJSONString(wrapper.all))
                    .execute();
        }
    }

    @Data
    public static class UseTokenWrapper {

        private final Map<String, Integer> all;
    }
}
