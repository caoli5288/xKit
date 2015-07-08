package com.mengcraft.xkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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

import com.avaje.ebean.EbeanServer;
import com.mengcraft.xkit.Main.Holder;
import com.mengcraft.xkit.lib.ItemUtil;
import com.mengcraft.xkit.util.ArrayBuilder;
import com.mengcraft.xkit.util.ArrayVector;

public class Executor implements CommandExecutor {

    private class EventExecutor implements Listener {

        @EventHandler
        public void handle(InventoryCloseEvent event) {
            Inventory inventory = event.getInventory();
            if (inventory.getHolder() instanceof Holder) {
                save(inventory.getTitle(),
                        inventory.getContents());
            }
        }
    }

    private final ItemUtil util;

    private final Main main;
    private final String[] info;

    private final Map<String, Define> map = new HashMap<>();

    private final Listener listener = new EventExecutor();

    private final EbeanServer server;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        ArrayVector<String> vector = new ArrayVector<>(args);

        String action = vector.next();

        if (action == null) sender.sendMessage(info);

        else if (action.equals("def")) define(sender, vector.next());
        else if (action.equals("kit")) {
            if (vector.remain() != 2) {
                sender.sendMessage(info);
            } else {
                kit(sender, vector.next(), vector.next());
            }
        }
        else sender.sendMessage(info);

        return true;
    }

    private void kit(CommandSender sender, String kit, String name) {
        Player who = main.getServer().getPlayerExact(name);
        Define def = map.get(kit);
        if (who != null && kit != null) {
            ItemStack[] stacks = convert(def.data);
            HashMap<?, ItemStack> out = who.getInventory()
                    .addItem(stacks);
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
        } else {
            String[] out = {
                    ChatColor.DARK_RED + "PLAYER NOT ONLINE,",
                    ChatColor.DARK_RED + "OR KIT NOT DEFINE!"
            };
            sender.sendMessage(out);
        }
        sender.sendMessage(ChatColor.GREEN + "DONE!");
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
                Define define = map.get(next);
                ItemStack[] stacks = convert(define.data);
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
            Define define = server.find(Define.class)
                    .where()
                    .eq("name", next)
                    .findUnique();
            if (define == null) {
                define = new Define();
                define.name = next;
            }
            define.data = JSONArray.toJSONString(list);
            server.save(define);
            map.put(next, define);
        } else if (map.get(next) != null) {
            server.delete(map.remove(next));
        }
    }

    private List<String> convert(ItemStack[] contents) {
        List<String> list = new ArrayList<>();
        for (ItemStack stack : contents) {
            if (stack != null && stack.getType() != Material.AIR) {
                try {
                    list.add(util.convert(stack));
                } catch (Exception e) {
                    main.getLogger().warning(e.getMessage());
                }
            }
        }
        return list;
    }

    public Executor(Main in) {
        this.main = in;
        this.info = new String[] {
                ChatColor.GOLD + "/xkit def (shown all defined kits)",
                ChatColor.GOLD + "/xkit def <kit_name> (not console)",
                ChatColor.GOLD + "/xkit kit <kit_name> <player_name>"
        };
        this.util = main.getUtil();
        in.getServer().getPluginManager().registerEvents(listener, in);
        this.server = in.getDatabase();
        List<Define> list = in.getDatabase().find(Define.class).findList();
        for (Define define : list) {
            this.map.put(define.name, define);
        }
    }

}
