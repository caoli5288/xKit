package com.mengcraft.xkit;

import com.mengcraft.xkit.entity.Kit;
import com.mengcraft.xkit.entity.KitOrder;
import com.mengcraft.xkit.util.Cache;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.json.simple.JSONValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created on 16-9-23.
 */
public class KitCommand implements CommandExecutor {

    private final Map<String, Cache<Kit>> cache = new HashMap<>();
    private final Main main;

    public KitCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command i, String label, String[] j) {
        Iterator<String> it = Arrays.asList(j).iterator();
        if (it.hasNext()) {
            return process(sender, it.next(), it);
        } else {
            sendInfo(sender);
        }
        return false;
    }

    private void sendInfo(CommandSender p) {
        p.sendMessage(ChatColor.RED + "/xkit kit <kit_name>");
        if (p.hasPermission("xkit.admin")) {
            p.sendMessage(ChatColor.RED + "/xkit all");
            if (p instanceof Player) {
                p.sendMessage(ChatColor.RED + "/xkit set <kit_name>");
            }
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> permission [permission]");
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> period [period_time]");
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> command [command]...");
        }
    }

    private boolean process(CommandSender sender, String next, Iterator<String> it) {
        if (Main.eq(next, "kit")) {
            if (it.hasNext() && sender instanceof Player) {
                main.execute(() -> {
                    kit(Player.class.cast(sender), it.next());
                });
                return true;
            } else {
                sendInfo(sender);
            }
        } else if (Main.eq(next, "set")) {
            if (it.hasNext() && sender.hasPermission("xkit.admin")) {
                return set(sender, it.next(), it);
            } else {
                sendInfo(sender);
            }
        } else if (Main.eq(next, "all")) {
            if (sender.hasPermission("xkit.admin")) {
                main.process(() -> main.find(Kit.class).findList(), list -> {
                    sender.sendMessage(ChatColor.GOLD + ">>> Kit List");
                    list.forEach(kit -> {
                        sender.sendMessage(ChatColor.GOLD + "- " + kit.getName());
                        sender.sendMessage(ChatColor.GOLD + "  - permission " + kit.getPermission());
                        sender.sendMessage(ChatColor.GOLD + "  - period " + kit.getPeriod());
                        sender.sendMessage(ChatColor.GOLD + "  - command " + kit.getCommand());
                    });
                });
                return true;
            } else {
                sendInfo(sender);
            }
        }
        return false;
    }

    private boolean set(CommandSender sender, String name, Iterator<String> it) {
        // TODO
        return false;
    }

    private void kit(Player p, String name) {
        Kit kit = fetch(name, true);
        if (Main.eq(kit, null)) {
            p.sendMessage(ChatColor.RED + "礼包" + name + "不存在");
        } else {
            kit(p, kit);
        }
    }

    private void kit(Player p, Kit kit) {
        if (!kit.hasPermission() || p.hasPermission(kit.getPermission())) {
            if (!kit.hasPeriod() || period(p, kit)) {
                main.process(() -> kit1(p, kit));
            }
        }
    }

    private void kit1(Player p, Kit kit) {
        if (kit.hasItem()) {
            kitItem(p, kit);
        }
        kitDispatch(p, kit.getCommand());
    }

    private void kitItem(Player p, Kit kit) {
        Inventory pak = main.getInventory();
        kit.getItemList().forEach(it -> {
            pak.addItem(it);
        });
        p.openInventory(pak);
    }

    private boolean period(Player p, Kit kit) {
        KitOrder order = main.find(KitOrder.class)
                .where()
                .eq("player", p.getUniqueId())
                .eq("kitId", kit.getId())
                .gt("time", Main.unixTime() - kit.getPeriod())
                .findUnique();
        boolean result = Main.eq(order, null);
        if (result) {
            main.getDatabase().save(KitOrder.of(p, kit));// Store only if have period
        } else {
            int time = order.getTime() + kit.getPeriod() - Main.unixTime();
            p.sendMessage(ChatColor.RED + "冷却时间剩余" + time + "秒");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void kitDispatch(Player p, String command) {
        if (!Main.eq(command, null)) {
            List<String> list = List.class.cast(JSONValue.parse(command));
            for (String line : list) {
                main.dispatch(line.replace("%player%", p.getName()));
            }
        }
    }

    private Kit fetch(String name, boolean update) {
        Cache<Kit> cached = cache.get(name);
        if (cached == null) {
            Kit kit = main.find(Kit.class)
                    .where()
                    .eq("name", name)
                    .findUnique();
            if (kit != null) {
                cache.put(name, new Cache<>(() -> update(kit), 300000));
            }
            return kit;
        }
        return cached.get(update);
    }

    private Kit update(Kit kit) {
        main.getDatabase().refresh(kit);
        return kit;
    }

}
