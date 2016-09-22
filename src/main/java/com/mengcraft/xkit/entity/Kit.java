package com.mengcraft.xkit.entity;

import com.comphenix.protocol.utility.StreamSerializer;
import com.google.common.collect.ImmutableList;
import com.mengcraft.xkit.Main;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

/**
 * Created on 16-9-23.
 */
@Entity
public class Kit {

    @Id
    private int id;

    @Column(unique = true)
    private String name;

    private String item;
    private String command;

    private String permission;
    private int period;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasItem() {
        return getItem() != null;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public boolean hasPermission() {
        return getPermission() != null;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean hasPeriod() {
        return getPeriod() > 0;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @SuppressWarnings("unchecked")
    public List<ItemStack> getItemList() {
        List<String> list = List.class.cast(JSONValue.parse(getItem()));
        return Main.collect(list, text -> {
            return Main.decode(String.valueOf(text));
        });
    }

}
