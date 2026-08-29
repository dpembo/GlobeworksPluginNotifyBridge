package uk.globeworks.bridges.luckperms;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import uk.globeworks.api.NationRef;
import uk.globeworks.bridges.BridgesPlugin;
import uk.globeworks.bridges.util.EventFire;
import uk.globeworks.bridges.util.TextUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Subscribes to LuckPerms' own EventBus (not Bukkit).
 * Only inheritance nodes for configured notable groups are recorded.
 */
public final class LuckPermsBridge {

    private final BridgesPlugin plugin;
    private final Set<String> notableGroups = new HashSet<>();
    private EventSubscription<NodeAddEvent> addSub;
    private EventSubscription<NodeRemoveEvent> removeSub;

    public LuckPermsBridge(BridgesPlugin plugin) {
        this.plugin = plugin;
        reloadNotableGroups();
    }

    /** Re-read notable-groups from config (call after /gwbridges reload). */
    public void reloadNotableGroups() {
        notableGroups.clear();
        for (String g : plugin.getConfig().getStringList("bridges.luckperms.notable-groups")) {
            if (g != null && !g.isBlank()) {
                notableGroups.add(g.toLowerCase());
            }
        }
        plugin.getLogger().info("LuckPerms notable-groups: "
            + (notableGroups.isEmpty() ? "(none — rank events filtered out)" : notableGroups));
    }

    public boolean subscribe() {
        try {
            LuckPerms api = LuckPermsProvider.get();
            EventBus bus = api.getEventBus();

            // Always subscribe so config reload can take effect without a full restart
            addSub = bus.subscribe(plugin, NodeAddEvent.class, this::onNodeAdd);
            removeSub = bus.subscribe(plugin, NodeRemoveEvent.class, this::onNodeRemove);
            plugin.debug("LuckPerms EventBus subscriptions active");
            return true;
        } catch (Throwable t) {
            plugin.getLogger().severe("LuckPerms subscribe failed: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    public void unsubscribe() {
        if (addSub != null) {
            addSub.close();
            addSub = null;
        }
        if (removeSub != null) {
            removeSub.close();
            removeSub = null;
        }
    }

    private void onNodeAdd(NodeAddEvent event) {
        handle(event.getTarget(), event.getNode(), true);
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        handle(event.getTarget(), event.getNode(), false);
    }

    private void handle(PermissionHolder target, Node node, boolean granted) {
        String nodeType = node.getType().name();
        String key = node.getKey();

        if (!(target instanceof User user)) {
            plugin.debug("LP skip: target is not User (" + target.getClass().getSimpleName()
                + ") node=" + key);
            return;
        }

        // Accept inheritance nodes; also treat keys like "group.king" as group refs
        String group = null;
        if (node.getType() == NodeType.INHERITANCE && node instanceof InheritanceNode inheritance) {
            group = inheritance.getGroupName();
        } else if (key != null && key.toLowerCase().startsWith("group.")) {
            group = key.substring("group.".length());
        }

        if (group == null) {
            plugin.debug("LP skip: not a group/inheritance node type=" + nodeType + " key=" + key);
            return;
        }

        if (notableGroups.isEmpty()) {
            plugin.debug("LP skip: notable-groups empty; got group=" + group);
            return;
        }

        if (!notableGroups.contains(group.toLowerCase())) {
            plugin.debug("LP skip: group '" + group + "' not in notable-groups " + notableGroups);
            return;
        }

        plugin.debug("LP match: group=" + group + " granted=" + granted
            + " user=" + user.getUsername());
        final String groupFinal = group;
        Bukkit.getScheduler().runTask(plugin, () -> fireRankEvent(user, groupFinal, granted));
    }

    private void fireRankEvent(User user, String group, boolean granted) {
        UUID uuid = user.getUniqueId();
        String playerName = user.getUsername() != null ? user.getUsername() : uuid.toString();

        Map<String, String> payload = new HashMap<>();
        payload.put("player", TextUtil.strip(playerName));
        payload.put("playerId", uuid.toString());
        payload.put("group", group);
        payload.put("action", granted ? "granted" : "removed");

        NationRef nationRef = null;
        try {
            Resident res = TownyUniverse.getInstance().getResident(uuid);
            if (res != null && res.hasNation()) {
                Nation nation = res.getNation();
                if (nation != null) {
                    nationRef = NationRef.of(nation.getUUID(), nation.getName());
                    payload.put("nationName", TextUtil.strip(nation.getName()));
                }
            }
        } catch (Throwable t) {
            plugin.debug("LP Towny nation lookup skipped: " + t.getMessage());
        }

        String type = granted ? "luckperms.rank_granted" : "luckperms.rank_removed";
        EventFire.fire(plugin, type, nationRef, payload, List.of(uuid));
    }
}
