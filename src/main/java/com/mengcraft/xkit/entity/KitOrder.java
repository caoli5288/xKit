package com.mengcraft.xkit.entity;

import com.mengcraft.xkit.Main;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import org.bukkit.entity.Player;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

/**
 * Created on 16-9-23.
 */
@Entity
@Data
@EqualsAndHashCode(of = "id")
public class KitOrder {

    @Id
    private int id;
    private UUID player;
    private int kitId;

    @Column
    private long time;

    public static KitOrder of(Player p, Kit kit) {
        val order = new KitOrder();
        order.player = p.getUniqueId();
        order.kitId = kit.getId();
        order.time = Main.now();
        return order;
    }
}
