package com.mengcraft.xkit.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class KitDefine {

    @Id
    private int id;

    @Column
    private String name;

    @Column
    private String data;

    @Column
    private int intervalHour;

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getIntervalHour() {
        return intervalHour;
    }

    public void setIntervalHour(int intervalHour) {
        this.intervalHour = intervalHour;
    }

}
