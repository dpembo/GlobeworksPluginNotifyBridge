package uk.globeworks.bridges;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import uk.globeworks.bridges.luckperms.LuckPermsBridge;
import uk.globeworks.bridges.siegewar.SiegeWarBridgeListener;
import uk.globeworks.bridges.towny.TownyBridgeListener;

public final class BridgesPlugin extends JavaPlugin {

    private LuckPermsBridge luckPermsBridge;
    private boolean debug;

    @Override
    public void onEnable() {

        String logo = "\r\n";
        logo += """
 _______ _____   _______ ______ _______ 
|     __|     |_|       |   __ \\    ___|
|    |  |       |   -   |   __ <    ___|
|_______|_______|_______|______/_______|
 ________ _______ ______ __  __ _______ 
|  |  |  |       |   __ \\  |/  |     __|
|  |  |  |   -   |      <     <|__     |
|________|_______|___|__|__|\\__|_______|               

          <PluginNotiyBridges>
""";
        getLogger().info(logo);
        getLogger().info("Globework " + getPluginMeta().getVersion() + " loaded (event bus classes available).");        



        saveDefaultConfig();
        this.debug = getConfig().getBoolean("debug", false);

        if (isBridgeEnabled("towny") && pluginPresent("Towny")) {
            getServer().getPluginManager().registerEvents(new TownyBridgeListener(this), this);
            getLogger().info("Towny bridge enabled.");
        } else {
            getLogger().info("Towny bridge disabled or Towny not present.");
        }

        if (isBridgeEnabled("siegewar") && pluginPresent("SiegeWar")) {
            getServer().getPluginManager().registerEvents(new SiegeWarBridgeListener(this), this);
            getLogger().info("SiegeWar bridge enabled.");
        } else {
            getLogger().info("SiegeWar bridge disabled or SiegeWar not present.");
        }

        if (isBridgeEnabled("luckperms") && pluginPresent("LuckPerms")) {
            luckPermsBridge = new LuckPermsBridge(this);
            if (luckPermsBridge.subscribe()) {
                getLogger().info("LuckPerms bridge enabled.");
            } else {
                getLogger().warning("LuckPerms bridge failed to subscribe.");
            }
        } else {
            getLogger().info("LuckPerms bridge disabled or LuckPerms not present.");
        }

        PluginCommand cmd = getCommand("gwbridges");
        if (cmd != null) {
            cmd.setExecutor(this::handleBridgesCommand);
            cmd.setTabCompleter((sender, command, alias, args) -> {
                if (args.length == 1) {
                    return java.util.List.of("debug", "reload").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .toList();
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
                    return java.util.List.of("on", "off").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .toList();
                }
                return java.util.List.of();
            });
        }

        getLogger().info("GlobeworksBridges enabled. debug=" + debug);
    }

    @Override
    public void onDisable() {
        if (luckPermsBridge != null) {
            luckPermsBridge.unsubscribe();
        }
    }

    private boolean handleBridgesCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("globeworks.bridges.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "GlobeworksBridges — debug=" + debug);
            sender.sendMessage(ChatColor.GRAY + "/gwbridges debug [on|off]  |  /gwbridges reload");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            this.debug = getConfig().getBoolean("debug", false);
            if (luckPermsBridge != null) {
                luckPermsBridge.reloadNotableGroups();
            }
            sender.sendMessage(ChatColor.GREEN + "Config reloaded. debug=" + debug
                + (luckPermsBridge != null ? " (LuckPerms groups refreshed)" : ""));
            return true;
        }
        if (args[0].equalsIgnoreCase("debug")) {
            if (args.length >= 2) {
                this.debug = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
                getConfig().set("debug", this.debug);
                saveConfig();
            } else {
                this.debug = !this.debug;
                getConfig().set("debug", this.debug);
                saveConfig();
            }
            sender.sendMessage(ChatColor.GREEN + "Bridges debug " + (debug ? "ON" : "OFF"));
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Usage: /gwbridges debug [on|off] | /gwbridges reload");
        return true;
    }

    public void debug(String message) {
        if (debug) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isBridgeEnabled(String key) {
        return getConfig().getBoolean("bridges." + key + ".enabled", false);
    }

    public boolean isEventEnabled(String bridge, String eventKey) {
        return getConfig().getBoolean("bridges." + bridge + ".events." + eventKey, true);
    }

    private boolean pluginPresent(String name) {
        Plugin p = Bukkit.getPluginManager().getPlugin(name);
        return p != null && p.isEnabled();
    }
}
