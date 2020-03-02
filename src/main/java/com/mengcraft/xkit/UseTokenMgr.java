package com.mengcraft.xkit;

import com.mengcraft.xkit.entity.KitUseToken;
import io.ebean.EbeanServer;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.NumberConversions;

import javax.persistence.OptimisticLockException;

@RequiredArgsConstructor
public class UseTokenMgr {

    private static final String META_KEY = "_kit_use_token";
    private final KitPlugin plugin;

    public KitUseToken load(Player player) {
        if (player.hasMetadata(META_KEY)) {
            return (KitUseToken) player.getMetadata(META_KEY).iterator().next().value();
        }
        KitUseToken useToken = plugin.getDataSource().find(KitUseToken.class, player.getUniqueId());
        if (useToken == null) {
            useToken = plugin.getDataSource().createEntityBean(KitUseToken.class);
            useToken.setId(player.getUniqueId());
            useToken.setName(player.getName());
        }
        useToken.flip();
        player.setMetadata(META_KEY, new FixedMetadataValue(plugin, useToken));
        return useToken;
    }

    public void supply(Player p, String token, int amount) {
        KitUseToken load = load(p);
        load.getUseTokenWrapper().compute(token, (__, old) -> NumberConversions.toLong(old) + amount);
        save(load);
    }

    public void consume(Player p, String useToken, int amount) throws IllegalStateException {
        KitUseToken token = load(p);
        token.getUseTokenWrapper().compute(useToken, (__, old) -> {
            long value = NumberConversions.toLong(old);
            if (value < amount) {
                throw new IllegalStateException("consume " + amount + " " + token);
            }
            return value - amount;
        });
        save(token);
    }

    public void save(KitUseToken useToken) {
        EbeanServer source = plugin.getDataSource();
        useToken.flip();
        try {
            source.save(useToken);
        } catch (OptimisticLockException ignored) {
            int result = source.createSqlUpdate("update kit_use_token set use_token = ? where id = ?")
                    .setParameter(1, useToken.getUseToken())
                    .setParameter(2, useToken.getId())
                    .execute();
            if (result < 1) {
                throw new IllegalStateException("update kit use token " + useToken);
            }
        }
    }

}
