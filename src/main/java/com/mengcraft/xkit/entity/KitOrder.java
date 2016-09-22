package com.mengcraft.xkit.entity;

import com.mengcraft.xkit.Main;
import org.bukkit.entity.Player;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

/**
 * Created on 16-9-23.
 */
@Entity
public class KitOrder {

    @Id
    private int id;

    private UUID player;

    private int kitId;

    private int time;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public int getKitId() {
        return kitId;
    }

    public void setKitId(int kitId) {
        this.kitId = kitId;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public static KitOrder of(Player p, Kit kit) {
        KitOrder order = new KitOrder();
        order.player = p.getUniqueId();
        order.kitId = kit.getId();
        order.time = Main.unixTime();
        return order;
    }

}
