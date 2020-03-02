package com.mengcraft.xkit.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created on 16-9-23.
 */
@Entity
@Data
@EqualsAndHashCode(of = "id")
@Table(name = "kit")
public class Kit {

    @Id
    private int id;

    @Column(unique = true)
    private String name;

    private int period;
    private int next;
    private String permission;
    private String useToken;

    @Column(columnDefinition = "LONGTEXT")
    private String item;
    private String command;
}
