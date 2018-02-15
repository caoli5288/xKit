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
                return new UseTokenWrapper(null, new HashMap<>());
            }
            return new UseTokenWrapper(useToken, (Map<String, Integer>) JSONValue.parse(useToken.getUseToken()));
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
        UseTokenWrapper wrapper = load(p);

        Integer val = wrapper.all.get(useToken);
        if (nil(val) || val < 1) {
            return false;
        }

        wrapper.all.put(useToken, --val);

        save(p, wrapper);

        return true;
    }

    public static void save(Player p, UseTokenWrapper wrapper) {
        EbeanServer db = Main.getDataSource();
        if (nil(wrapper.token)) {
            db.createUpdate(KitUseToken.class, "insert into kit_use_token set id = :id, name = :name, use_token = :token;")
                    .setParameter("id", p.getUniqueId())
                    .setParameter("name", p.getName())
                    .setParameter("token", JSONObject.toJSONString(wrapper.all))
                    .execute();
        } else {
            db.createUpdate(KitUseToken.class, "update kit_use_token set use_token = :token where id = :id;")
                    .setParameter("id", p.getUniqueId())
                    .setParameter("token", JSONObject.toJSONString(wrapper.all))
                    .execute();
        }
    }

    @Data
    public static class UseTokenWrapper {

        private final KitUseToken token;
        private final Map<String, Integer> all;
    }
}
