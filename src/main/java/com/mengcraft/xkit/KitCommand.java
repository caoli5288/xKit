package com.mengcraft.xkit;

import com.mengcraft.xkit.entity.Kit;
import com.mengcraft.xkit.entity.KitOrder;
import com.mengcraft.xkit.event.KitReceivedEvent;
import com.mengcraft.xkit.util.Cache;
import com.mengcraft.xkit.util.Messenger;
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
    private final Messenger messenger;

    public KitCommand(Main main) {
        this.main = main;
        messenger = new Messenger(main);
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

    private boolean process(CommandSender sender, String next, Iterator<String> it) {
        if (Main.eq(next, "all")) {
            boolean result = sender.hasPermission("xkit.admin");
            if (result) {
                all(sender);
            } else {
                sendInfo(sender);
            }
            return result;
        } else if (Main.eq(next, "del")) {
            return admin(sender, it, () -> del(sender, it.next()));
        } else if (Main.eq(next, "add")) {
            return admin(sender, it, () -> add(sender, it.next()));
        } else if (Main.eq(next, "kit")) {
            if (it.hasNext() && sender instanceof Player) {
                main.exec(() -> {
                    kit(Player.class.cast(sender), it.next());
                });
                return true;
            } else {
                sendInfo(sender);
            }
        } else if (Main.eq(next, "set")) {
            return admin(sender, it, () -> set(sender, it.next(), it));
        }
        return false;
    }

    private void all(CommandSender sender) {
        main.consume(() -> main.find(Kit.class).findList(), list -> {
            sender.sendMessage(ChatColor.GOLD + "* Kit list");
            list.forEach(kit -> {
                sender.sendMessage(ChatColor.GOLD + "- " + kit.getName());
                sender.sendMessage(ChatColor.GOLD + "  - permission " + kit.getPermission());
                sender.sendMessage(ChatColor.GOLD + "  - period " + kit.getPeriod());
                sender.sendMessage(ChatColor.GOLD + "  - item " + (kit.hasItem() ? "some" : "null"));
                sender.sendMessage(ChatColor.GOLD + "  - command " + kit.getCommand());
            });
        });
    }

    private boolean admin(CommandSender sender, Iterator<String> it, Runnable runnable) {
        boolean result = it.hasNext() && sender.hasPermission("xkit.admin");
        if (result) {
            main.exec(runnable);
        } else {
            sendInfo(sender);
        }
        return result;
    }

    private void del(CommandSender sender, String next) {
        Kit kit = fetch(next, false);
        if (Main.nil(kit)) {
            sender.sendMessage(ChatColor.RED + "礼包" + next + "不存在");
        } else {
            main.getDatabase().delete(kit);
            cache.remove(next);
            sender.sendMessage(ChatColor.GREEN + "礼包" + next + "已删除成功");
        }
    }

    private void add(CommandSender p, String name) {
        Kit fetch = fetch(name, false);
        if (Main.nil(fetch)) {
            Kit kit = main.getDatabase().createEntityBean(Kit.class);
            kit.setName(name);
            main.getDatabase().save(kit);
            cache.put(name, new Cache<Kit>(() -> update(kit.getId()), 300000));

            p.sendMessage(ChatColor.GREEN + "礼包" + name + "已定义成功");
        } else {
            p.sendMessage(ChatColor.RED + "礼包" + name + "已经被定义");
        }
    }

    private boolean set(CommandSender sender, String name, Iterator<String> it) {
        Kit kit = fetch(name, true);
        if (Main.nil(kit)) {
            sender.sendMessage(ChatColor.RED + "礼包" + name + "不存在");
        } else if (it.hasNext()) {
            return set(sender, kit, it);
        } else if (sender instanceof Player) {
            Player p = Player.class.cast(sender);
            main.run(() -> {
                Inventory pak = main.getInventory(name);
                if (kit.hasItem()) {
                    pak.setContents(Main.getItemList(kit));
                }
                p.openInventory(pak);
            });
            return true;
        }
        return false;
    }

    private boolean set(CommandSender sender, Kit kit, Iterator<String> it) {
        String next = it.next();
        if (Main.eq(next, "command")) {
            if (it.hasNext()) {
                return setCommand(sender, it, kit);
            } else {
                kit.setCommand(null);
                main.getDatabase().save(kit);
                sender.sendMessage(ChatColor.GREEN + "命令已删除");
            }
            return true;
        } else if (Main.eq(next, "permission")) {
            if (it.hasNext()) {
                kit.setPermission(it.next());
                main.getDatabase().save(kit);
                sender.sendMessage(ChatColor.GREEN + "权限已设置");
            } else {
                kit.setPermission(null);
                main.getDatabase().save(kit);
                sender.sendMessage(ChatColor.GREEN + "权限已删除");
            }
            return true;
        } else if (Main.eq(next, "period")) {
            if (it.hasNext()) {
                int period = Integer.parseInt(it.next());
                if (period < 1) {
                    period = 0;
                }
                kit.setPeriod(period);
                main.getDatabase().save(kit);
                sender.sendMessage(ChatColor.GREEN + "冷却已设置");
            } else {
                kit.setPeriod(0);
                main.getDatabase().save(kit);
                sender.sendMessage(ChatColor.GREEN + "冷却已取消");
            }
            return true;
        } else {
            sendInfo(sender);
        }
        return false;
    }

    private boolean setCommand(CommandSender sender, Iterator<String> it, Kit kit) {
        StringBuilder builder = new StringBuilder();
        while (it.hasNext()) {
            builder.append(it.next());
            if (it.hasNext()) {
                builder.append(" ");
            }
        }
        String command = builder.toString();
        Object t = JSONValue.parse(command);
        if (Main.nil(t) || !(t instanceof List)) {
            sender.sendMessage(ChatColor.RED + "命令不符合JSON格式");
        } else {
            kit.setCommand(command);
            main.getDatabase().save(kit);
            sender.sendMessage(ChatColor.GREEN + "命令已设置");
            return true;
        }
        return false;
    }

    private void kit(Player p, String name) {
        Kit kit = fetch(name, true);
        if (Main.nil(kit)) {
            p.sendMessage(ChatColor.RED + "礼包" + name + "不存在");
        } else if (Main.valid(kit)) {
            kit(p, kit);
        } else {
            p.sendMessage(ChatColor.RED + "礼包" + name + "尚未准备好");
        }
    }

    private void kit(Player p, Kit kit) {
        if (!kit.hasPermission() || p.hasPermission(kit.getPermission())) {
            if (!kit.hasPeriod() || period(p, kit)) {
                main.run(() -> kit1(p, kit));
            }
        } else {
            messenger.send(p, "receive.failed.permission");
        }
    }

    private void kit1(Player p, Kit kit) {
        if (kit.hasCommand()) {
            dispatch(p, kit.getCommand());
        }
        kitItem(p, kit);
        messenger.send(p, "receive.successful");
        KitReceivedEvent.call(p, kit);
    }

    private void kitItem(Player p, Kit kit) {
        if (kit.hasItem()) {// may null
            Inventory pak = main.getInventory();
            pak.setContents(Main.getItemList(kit));
            p.openInventory(pak);
        }
    }

    private boolean period(Player p, Kit kit) {
        KitOrder order = main.find(KitOrder.class)
                .where()
                .eq("player", p.getUniqueId())
                .eq("kitId", kit.getId())
                .gt("time", Main.now() - kit.getPeriod())
                .findUnique();
        boolean result = Main.nil(order);
        if (result) {
            main.getDatabase().save(KitOrder.of(p, kit));// Store only if have period
        } else {
            int time = order.getTime() + kit.getPeriod() - Main.now();
            String str = messenger.find("receive.failed.cooling");
            p.sendMessage(ChatColor.translateAlternateColorCodes(
                    '&',
                    str.replace("%time%", Integer.toString(time))));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void dispatch(Player p, String command) {
        List<String> list = List.class.cast(JSONValue.parse(command));
        for (String line : list) {
            main.dispatch(line.replace("%player%", p.getName()));
        }
    }

    public Kit fetch(String name, boolean update) {
        Cache<Kit> cached = cache.get(name);
        if (cached == null) {
            Kit kit = main.find(Kit.class)
                    .where()
                    .eq("name", name)
                    .findUnique();
            if (kit != null) {
                cache.put(name, new Cache<>(() -> update(kit.getId()), 300000));
            }
            return kit;
        }
        return cached.get(update);
    }

    private Kit update(int id) {
        return main.getDatabase().find(Kit.class, id);
    }

    private void sendInfo(CommandSender p) {
        if (p.hasPermission("xkit.admin")) {
            p.sendMessage(ChatColor.RED + "/xkit all");
            p.sendMessage(ChatColor.RED + "/xkit add <kit_name>");
            p.sendMessage(ChatColor.RED + "/xkit del <kit_name>");
            if (p instanceof Player) {
                p.sendMessage(ChatColor.RED + "/xkit set <kit_name>");
            }
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> permission [permission]");
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> period [period_time]");
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> command [command]...");
        }
        p.sendMessage(ChatColor.RED + "/xkit kit <kit_name>");
    }

}
