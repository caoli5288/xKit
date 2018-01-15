package com.mengcraft.xkit.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(of = "id")
public class KitUseToken {

    @Id
    private UUID id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String useToken;
}
