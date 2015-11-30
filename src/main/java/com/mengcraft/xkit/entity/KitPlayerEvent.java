package com.mengcraft.xkit.entity;

import com.avaje.ebean.annotation.CreatedTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
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

    @OneToOne
    private KitDefine define;

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

    public KitDefine getDefine() {
        return define;
    }

    public void setDefine(KitDefine define) {
        this.define = define;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

}
