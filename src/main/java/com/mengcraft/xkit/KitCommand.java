package com.mengcraft.xkit;

import com.mengcraft.xkit.entity.Kit;
import com.mengcraft.xkit.entity.KitOrder;
import com.mengcraft.xkit.event.KitReceivedEvent;
import com.mengcraft.xkit.util.BiRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.json.simple.JSONValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import static com.mengcraft.xkit.KitPlugin.nil;

/**
 * Created on 16-9-23.
 */
public class KitCommand implements CommandExecutor {

    private final BiRegistry<CommandSender, Iterator<String>> root = new BiRegistry<>();
    private KitPlugin main;

    KitCommand(KitPlugin main) {
        this.main = main;
        root.register("add-token", (sender, input) -> admin(sender, input, () -> addToken(sender, input.next(), input)));
        root.register("add", (sender, input) -> admin(sender, input, () -> add(sender, input.next())));
        root.register("del", (sender, input) -> admin(sender, input, () -> del(sender, input.next())));
        root.register("set", (sender, input) -> admin(sender, input, () -> set(sender, input.next(), input)));
        root.register("all", (sender, input) -> all(sender));
        root.register("kit", (sender, input) -> kit(sender, input));
    }

    private boolean admin(CommandSender sender, Iterator<String> it, Runnable runnable) {
        boolean result = it.hasNext() && sender.hasPermission("xkit.admin");
        if (result) {
            KitPlugin.exec(runnable);
        } else {
            sendInfo(sender);
        }
        return result;
    }

    private void addToken(CommandSender sender, String name, Iterator<String> it) {
        Player p = Bukkit.getPlayerExact(name);
        if (nil(p)) {
            sender.sendMessage(ChatColor.RED + "玩家不在线");
        }

        String token = it.next();
        int amount = it.hasNext() ? Integer.parseInt(it.next()) : 1;

        main.getUseTokenMgr().supply(p, token, amount);

        sender.sendMessage(ChatColor.GREEN + "操作已完成");
    }

    private void add(CommandSender p, String name) {
        Kit fetch = fetch(name, false);
        if (nil(fetch)) {
            Kit kit = main.getDataSource().createEntityBean(Kit.class);
            kit.setName(name);
            main.save(kit);
            p.sendMessage(ChatColor.GREEN + "礼包" + name + "已定义成功");
        } else {
            p.sendMessage(ChatColor.RED + "礼包" + name + "已经被定义");
        }
    }

    private void del(CommandSender sender, String next) {
        Kit kit = fetch(next, false);
        if (nil(kit)) {
            sender.sendMessage(ChatColor.RED + "礼包" + next + "不存在");
        } else {
            main.getDataSource().delete(kit);
            L2Pool.expire(kit);
            sender.sendMessage(ChatColor.GREEN + "礼包" + next + "已删除成功");
        }
    }

    private boolean set(CommandSender sender, String name, Iterator<String> it) {
        Kit kit = fetch(name, true);
        if (nil(kit)) {
            sender.sendMessage(ChatColor.RED + "礼包" + name + "不存在");
        } else if (it.hasNext()) {
            return set(sender, kit, it);
        } else if (sender instanceof Player) {
            Player p = Player.class.cast(sender);
            main.run(() -> {
                Inventory pak = main.getInventory(name);
                if (!nil(kit.getItem())) {
                    pak.setContents(KitPlugin.itemListFrom(kit));
                }
                p.openInventory(pak);
            });
            return true;
        }
        return false;
    }

    private void all(CommandSender sender) {
        if (!sender.hasPermission("xkit.admin")) {
            return;
        }
        main.consume(() -> main.find(Kit.class).findList(), list -> {
            sender.sendMessage(ChatColor.GOLD + "* Kit list");
            list.forEach(kit -> {
                sender.sendMessage(ChatColor.GOLD + "- " + kit.getName());
                sender.sendMessage(ChatColor.GOLD + "  - permission " + kit.getPermission());
                sender.sendMessage(ChatColor.GOLD + "  - token " + kit.getUseToken());
                sender.sendMessage(ChatColor.GOLD + "  - period " + kit.getPeriod());
                sender.sendMessage(ChatColor.GOLD + "  - day " + kit.getNext());
                sender.sendMessage(ChatColor.GOLD + "  - item " + (nil(kit.getItem()) || kit.getItem().isEmpty() ? "null" : "some"));
                sender.sendMessage(ChatColor.GOLD + "  - command " + kit.getCommand());
            });
        });
    }

    private void kit(CommandSender sender, Iterator<String> input) {
        if (!input.hasNext()) {
            return;
        }
        String kit = input.next();
        if (input.hasNext()) {
            if (!sender.hasPermission("xkit.admin")) {
                return;
            }
            Player p = Bukkit.getPlayerExact(input.next());
            if (nil(p)) {
                sender.sendMessage(ChatColor.RED + "!!! xKit -> player not found");
            } else {
                KitPlugin.exec(() -> kit(p, kit, true));
            }
        } else {
            if (!(sender instanceof Player)) {
                return;
            }
            KitPlugin.exec(() -> kit((Player) sender, kit, false));
        }
    }

    private void sendInfo(CommandSender p) {
        if (p.hasPermission("xkit.admin")) {
            p.sendMessage(ChatColor.RED + "/xkit all");
            p.sendMessage(ChatColor.RED + "/xkit add <kit_name>");
            p.sendMessage(ChatColor.RED + "/xkit add-token <player> <token> [amount]");
            p.sendMessage(ChatColor.RED + "/xkit del <kit_name>");
            if (p instanceof Player) {
                p.sendMessage(ChatColor.RED + "/xkit set <kit_name>");
            }
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> permission [permission]");
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> period [period_second]");
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> day [period_day]");
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> token [token_name]");
            p.sendMessage(ChatColor.RED + "/xkit set <kit_name> command [command]...");
        }
        p.sendMessage(ChatColor.RED + "/xkit kit <kit_name> [player_name]");
    }

    Kit fetch(String name, boolean update) {
        if (update) {
            L2Pool.expire("kit:name:" + name);
        }
        return L2Pool.kitByName(name);
    }

    private boolean set(CommandSender sender, Kit kit, Iterator<String> it) {
        String next = it.next();
        if (KitPlugin.eq(next, "token")) {
            if (it.hasNext()) {
                String tok = it.next();
                kit.setUseToken(tok);
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "令牌已设置");
            } else {
                kit.setUseToken(null);
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "令牌已取消");
            }
        } else if (KitPlugin.eq(next, "command")) {
            if (it.hasNext()) {
                return setCommand(sender, it, kit);
            } else {
                kit.setCommand(null);
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "命令已删除");
            }
            return true;
        } else if (KitPlugin.eq(next, "permission")) {
            if (it.hasNext()) {
                kit.setPermission(it.next());
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "权限已设置");
            } else {
                kit.setPermission(null);
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "权限已删除");
            }
            return true;
        } else if (KitPlugin.eq(next, "period")) {
            if (it.hasNext()) {
                int day = kit.getNext();
                if (day > 0) {
                    sender.sendMessage(ChatColor.RED + "与天数设置冲突");
                    return false;
                }
                int period = Integer.parseInt(it.next());
                if (period < 0) {
                    period = 0;
                }
                kit.setPeriod(period);
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "冷却已设置");
            } else {
                kit.setPeriod(0);
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "冷却已取消");
            }
            return true;
        } else if (KitPlugin.eq(next, "day")) {
            if (it.hasNext()) {
                int period = kit.getPeriod();
                if (period > 0) {
                    sender.sendMessage(ChatColor.RED + "与周期设置冲突");
                    return false;
                }
                int day = Integer.parseInt(it.next());
                kit.setNext(Math.max(0, day));
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "冷却已设置");
            } else {
                kit.setPeriod(0);
                main.save(kit);
                sender.sendMessage(ChatColor.GREEN + "冷却已取消");
            }
            return true;
        } else {
            sendInfo(sender);
        }
        return false;
    }

    private void kit(Player p, String name, boolean force) {
        Kit kit = fetch(name, true);
        if (nil(kit)) {
            p.sendMessage(ChatColor.RED + "礼包" + name + "不存在");
        } else if (KitPlugin.valid(kit)) {
            kit(p, kit, force);
        } else {
            p.sendMessage(ChatColor.RED + "礼包" + name + "尚未准备好");
        }
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
        if (nil(t) || !(t instanceof List)) {
            sender.sendMessage(ChatColor.RED + "命令不符合JSON格式");
        } else {
            kit.setCommand(command);
            main.save(kit);
            sender.sendMessage(ChatColor.GREEN + "命令已设置");
            return true;
        }
        return false;
    }

    private void kit(Player p, Kit kit, boolean force) {
        if (force) {
            preKitOrder(p, kit, true);
            return;
        }

        if (!nil(kit.getPermission()) && !kit.getPermission().isEmpty() && !p.hasPermission(kit.getPermission())) {
            KitPlugin.getMessenger().send(p, "receive.failed.permission");
            return;
        }

        if (kit.getPeriod() == 0 && kit.getNext() == 0) {
            preKitOrder(p, kit);
            return;
        }

        KitOrder order = L2Pool.orderBy(p, kit);
        if (nil(order)) {
            preKitOrder(p, kit);
            return;
        }

        long next = nextKit(kit, order);
        if (!(next > 0)) {
            preKitOrder(p, kit);
            return;
        }

        String str = KitPlugin.getMessenger().find("receive.failed.cooling");
        p.sendMessage(str.replace("%time%", String.valueOf(next)).replace('&', ChatColor.COLOR_CHAR));
    }

    private void preKitOrder(Player p, Kit kit) {
        preKitOrder(p, kit, false);
    }

    private void preKitOrder(Player p, Kit kit, boolean force) {
        if (force) {
            kitOrder(p, kit);
        }
        String useToken = kit.getUseToken();
        if (useToken == null || useToken.isEmpty()) {
            kitOrder(p, kit);
        }
        try {
            main.getUseTokenMgr().consume(p, useToken, 1);
        } catch (IllegalStateException e) {
            KitPlugin.getMessenger().send(p, "no_token", "&c没有足够的Token");
            return;
        }
        kitOrder(p, kit);
    }

    private void kitOrder(Player p, Kit kit) {
        KitOrder kitOrder = KitOrder.of(p, kit);
        L2Pool.put(kitOrder);
        main.save(kitOrder);
        main.run(() -> kit1(p, kit));
    }

    public static long nextKit(Kit kit, KitOrder order) {
        int period = kit.getPeriod();
        if (period > 0) {
            return order.getTime() + kit.getPeriod() - KitPlugin.now();
        }
        int day = kit.getNext();
        Instant next = Instant.ofEpochSecond(order.getTime()).plus(day, ChronoUnit.DAYS);
        return LocalDateTime.ofInstant(next, ZoneId.systemDefault()).toLocalDate().atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond() - KitPlugin.now();
    }

    private void kit1(Player p, Kit kit) {
        if (!nil(kit.getCommand()) && !kit.getCommand().isEmpty()) {
            dispatch(p, kit.getCommand());
        }
        kitItem(p, kit);
        KitPlugin.getMessenger().send(p, "receive.successful");
        KitReceivedEvent.call(p, kit);
    }

    @SuppressWarnings("unchecked")
    private void dispatch(Player p, String command) {
        List<String> list = (List<String>) JSONValue.parse(command);
        for (String line : list) {
            line = Formatter.format(p, line);
            main.dispatch(line.replace("%player%", p.getName()));
        }
    }

    private void kitItem(Player p, Kit kit) {
        if (!nil(kit.getItem()) && !kit.getItem().isEmpty()) {// may null
            Inventory pak = main.getInventory();
            pak.setContents(KitPlugin.itemListFrom(kit));
            p.openInventory(pak);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command __, String label, String[] input) {
        Iterator<String> inputItr = Arrays.asList(input).iterator();
        if (inputItr.hasNext()) {
            try {
                root.handle(inputItr.next(), sender, inputItr);
                return true;
            } catch (Exception e) {
                main.getLogger().log(Level.WARNING, "", e);
            }
        } else {
            sendInfo(sender);
        }
        return false;
    }

}
