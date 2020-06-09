package com.mengcraft.xkit;

import com.mengcraft.xkit.entity.Kit;
import com.mengcraft.xkit.entity.KitOrder;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Level;

import static com.mengcraft.xkit.KitPlugin.nil;

public class KitPlaceholderHook extends PlaceholderExpansion {

    public KitPlaceholderHook(Plugin plugin) {
    }

    @Override
    public String getIdentifier() {
        return "xkit";
    }

    @Override
    public String getAuthor() {
        return "caoli5288";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    interface IFunc {

        String execute(Player p, Kit kit);
    }

    enum Label {

        OKAY(Label::okay),

        NEXT((p, kit) -> {
            KitOrder order = L2Pool.orderBy(p, kit);
            if (nil(order)) {
                return "-1";
            }

            long next = KitCommand.nextKit(kit, order);
            if (next < 1) {
                return "-1";
            }

            return String.valueOf(next);
        }),

        NEXTDATE((p, kit) -> {
            KitOrder order = L2Pool.orderBy(p, kit);
            if (nil(order)) {
                return "null";
            }

            long next = KitCommand.nextKit(kit, order);
            if (!(next > 0)) {
                return "null";
            }

            Instant instant = Instant.now().plusSeconds(next);
            LocalDateTime i = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            return i.getYear() + "年" + i.getMonthValue() + "月" + i.getDayOfMonth() + "日 " + i.getHour() + "点" + i.getMinute() + "分" + i.getSecond() + "秒";
        });

        static String okay(Player p, Kit kit) {
            return Objects.equals(Label.NEXT.func.execute(p, kit), "-1") ? "true" : "false";
        }

        private final IFunc func;

        Label(IFunc func) {
            this.func = func;
        }
    }

    @Override
    public String onPlaceholderRequest(Player p, String label) {
        Iterator<String> itr = Arrays.asList(label.split("_")).iterator();
        Kit kit = L2Pool.kitByName(itr.next());
        if (nil(kit)) {
            return "null";
        }
        try {
            return Label.valueOf(itr.next().toUpperCase()).func.execute(p, kit);
        } catch (Exception thr) {
            Bukkit.getLogger().log(Level.SEVERE, "", thr);
        }
        return "null";
    }

}
