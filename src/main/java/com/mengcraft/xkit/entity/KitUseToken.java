package com.mengcraft.xkit.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(of = "id")
@Table(name = "kit_token")
public class KitUseToken {

    @Id
    private UUID id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String useToken;

    private transient Map<String, Long> useTokenWrapper = new HashMap<>();

    public void flip() {
        if (getUseToken() == null) {
            setUseToken(JSONObject.toJSONString(useTokenWrapper));
            return;
        }

        if (useTokenWrapper.isEmpty()) {
            useTokenWrapper.putAll((Map<String, Long>) JSONValue.parse(useToken));
            return;
        }

        setUseToken(JSONObject.toJSONString(useTokenWrapper));
    }
}
