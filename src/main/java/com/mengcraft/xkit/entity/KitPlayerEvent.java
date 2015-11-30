package com.mengcraft.xkit.entity;

import com.avaje.ebean.annotation.CreatedTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

/**
 * Created on 15-11-30.
 */
@Entity
public class KitPlayerEvent {

    @Id
    private int id;

    @Column
    private String name;

    @Column
    private KitDefine kitDefine;

    @CreatedTimestamp
    private Timestamp time;

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

    public KitDefine getKitDefine() {
        return kitDefine;
    }

    public void setKitDefine(KitDefine kitDefine) {
        this.kitDefine = kitDefine;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

}
