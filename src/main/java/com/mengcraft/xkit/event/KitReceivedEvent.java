package com.mengcraft.xkit.event;

import com.mengcraft.xkit.entity.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Created on 16-10-29.
 */
public class KitReceivedEvent extends PlayerEvent {

    private final static HandlerList HANDLER_LIST = new HandlerList();
    private final Kit kit;

    public KitReceivedEvent(Player who, Kit kit) {
        super(who);
        this.kit = kit;
    }

    public Kit getKit() {
        return kit;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public static KitReceivedEvent call(Player p, Kit kit) {
        KitReceivedEvent event = new KitReceivedEvent(p, kit);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

}
