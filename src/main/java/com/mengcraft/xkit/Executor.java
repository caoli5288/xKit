package com.mengcraft.xkit;

import com.avaje.ebean.EbeanServer;
import com.mengcraft.xkit.entity.KitDefine;
import com.mengcraft.xkit.entity.KitPlayerEvent;
import com.mengcraft.xkit.lib.ItemUtil;
import com.mengcraft.xkit.util.ArrayBuilder;
import com.mengcraft.xkit.util.ArrayVector;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Executor implements CommandExecutor, Listener {

    private final ItemUtil util;
    private final Main main;
    private final String[] info;
    private final Map<String, KitDefine> map = new HashMap<>();
    private final EbeanServer source;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        ArrayVector<String> vector = new ArrayVector<>(args);

        String action = vector.next();

        if (action == null) {
            sender.sendMessage(info);
        } else if (action.equals("def") && sender.hasPermission("xkit.admin")) {
            define(sender, vector.next());
        } else if (action.equals("kit")) {
            if (vector.remain() < 1) {
                sender.sendMessage(info);
            } else {
                kit(sender, vector.next(), vector.next());
            }
        } else {
            sender.sendMessage(info);
        }

        return true;
    }

    @EventHandler
    public void handle(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() == main.getHolder()) {
            save(inventory.getTitle(), inventory.getContents());
        }
    }

    private void kit(CommandSender sender, String kit, String name) {
        if (!(sender.hasPermission("xkit.use." + kit) || sender.hasPermission("xkit.admin"))) {
            sender.sendMessage(ChatColor.DARK_RED + "你没有权限这么做");
            return;
        }
        if (!(name == null || sender.getName().equals(name) || sender.hasPermission("xkit.admin"))) {
            sender.sendMessage(ChatColor.DARK_RED + "你没有权限这么做");
            return;
        }
        Player who = name == null && Player.class.isInstance(sender)
                ? (Player) sender
                : main.getServer().getPlayerExact(name);

        KitDefine define = map.get(kit);

        if (!(sender.hasPermission("xkit.admin") || checkInterval(who.getName(), define))) {
            sender.sendMessage(ChatColor.DARK_RED + "你暂时不能这么做");
            return;
        }
        if (who != null && kit != null) {
            ItemStack[] stacks = convert(define.getData());
            HashMap<?, ItemStack> out = who.getInventory().addItem(stacks);
            if (out.size() != 0) {
                Location location = who.getLocation();
                World world = who.getWorld();
                for (ItemStack stack : out.values()) {
                    world.dropItem(location, stack);
                }
                who.sendMessage(ChatColor.GREEN + "你礼包中的一些物品掉落了！");
                sender.sendMessage(ChatColor.DARK_RED + "DONE WITH"
                        + " SOME ITEMS DROP ON FLOOR！");
            }
            KitPlayerEvent event = new KitPlayerEvent();
            event.setName(who.getName());
            event.setKitDefine(define);
            main.execute(() -> source.save(event));
        } else {
            String[] out = {
                    ChatColor.DARK_RED + "PLAYER NOT ONLINE,",
                    ChatColor.DARK_RED + "OR KIT NOT DEFINE!"
            };
            sender.sendMessage(out);
        }
        sender.sendMessage(ChatColor.GREEN + "DONE!");
    }

    private boolean checkInterval(String name, KitDefine define) {
        return source.find(KitPlayerEvent.class)
                .where()
                .eq("name", name)
                .gt("time", new Timestamp(System.currentTimeMillis() - define.getInterval() * 3600000))
                .findUnique() == null;
    }

    private void define(CommandSender sender) {
        if (map.size() != 0) {
            for (String line : map.keySet()) {
                sender.sendMessage(ChatColor.GOLD + "- " + line);
            }
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "EMPTY!");
        }
    }

    private void define(CommandSender sender, String next) {
        if (sender instanceof Player && next != null) {
            Player player = (Player) sender;
            if (map.get(next) != null) {
                KitDefine define = map.get(next);
                ItemStack[] stacks = convert(define.getData());
                Inventory inventory = main.getInventory(next);
                inventory.setContents(stacks);
                player.openInventory(inventory);
            } else {
                player.openInventory(main.getInventory(next));
            }
        } else if (next != null) {
            sender.sendMessage(info);
        } else {
            define(sender);
        }
    }

    @SuppressWarnings("all")
    private ItemStack[] convert(String data) {
        ArrayBuilder<ItemStack> builder = new ArrayBuilder<>();
        try {
            List<String> list = (List) new JSONParser().parse(data);
            for (String line : list) {
                builder.append(util.convert(line));
            }
        } catch (Exception e) {
            main.getLogger().warning(e.getMessage());
        }
        return builder.build(ItemStack.class);
    }

    public void save(String next, ItemStack[] contents) {
        List<String> list = convert(contents);
        if (list.size() != 0) {
            KitDefine define = source.find(KitDefine.class)
                    .where()
                    .eq("name", next)
                    .findUnique();
            if (define == null) {
                define = source.createEntityBean(KitDefine.class);
                define.setName(next);
            }
            define.setData(JSONArray.toJSONString(list));
            source.save(define);
            map.put(next, define);
        } else if (map.get(next) != null) {
            source.delete(map.remove(next));
        }
    }

    @SuppressWarnings("deprecation")
    private List<String> convert(ItemStack[] contents) {
        List<String> list = new ArrayList<>();
        for (ItemStack stack : contents) {
            if (stack != null && stack.getTypeId() != 0) {
                add(list, stack);
            }
        }
        return list;
    }

    private void add(List<String> list, ItemStack stack) {
        try {
            list.add(util.convert(stack));
        } catch (Exception e) {
            main.getLogger().warning(e.getMessage());
        }
    }

    public Executor(Main main, ItemUtil util) {
        this.main = main;
        this.info = new String[]{
                ChatColor.GOLD + "/xkit def (shown all defined kits)",
                ChatColor.GOLD + "/xkit def <kit_name> (not console)",
                ChatColor.GOLD + "/xkit kit <kit_name> <player_name>"
        };
        this.util = util;
        this.source = main.getDatabase();
        for (KitDefine define : main.getDatabase().find(KitDefine.class).findList()) {
            this.map.put(define.getName(), define);
        }
    }

    public void bind() {
        main.getCommand("xkit").setExecutor(this);
        main.getServer().getPluginManager().registerEvents(this, main);
    }

}
