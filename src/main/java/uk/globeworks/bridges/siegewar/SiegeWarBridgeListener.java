package uk.globeworks.bridges.siegewar;

import com.gmail.goosius.siegewar.events.SiegeEndEvent;
import com.gmail.goosius.siegewar.events.SiegeWarStartEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import uk.globeworks.api.NationRef;
import uk.globeworks.bridges.BridgesPlugin;
import uk.globeworks.bridges.util.EventFire;
import uk.globeworks.bridges.util.TextUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SiegeWarBridgeListener implements Listener {

    private final BridgesPlugin plugin;

    public SiegeWarBridgeListener(BridgesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSiegeStart(SiegeWarStartEvent event) {
        if (!plugin.isEventEnabled("siegewar", "siege-started")) return;

        Nation nation = event.getNation();
        Town target = event.getTargetTown();
        Player starter = event.getSiegeStarter();

        Map<String, String> payload = new HashMap<>();
        payload.put("siegeType", TextUtil.strip(event.getSiegeType()));
        if (target != null) {
            payload.put("townName", TextUtil.strip(target.getName()));
            payload.put("townId", target.getUUID().toString());
        }
        if (nation != null) {
            payload.put("attackerNation", TextUtil.strip(nation.getName()));
        }
        if (event.getTownOfSiegeStarter() != null) {
            payload.put("starterTown", TextUtil.strip(event.getTownOfSiegeStarter().getName()));
        }

        NationRef ref = nation != null
            ? NationRef.of(nation.getUUID(), nation.getName())
            : null;

        List<UUID> actors = starter != null ? List.of(starter.getUniqueId()) : List.of();

        EventFire.fire(plugin, "siegewar.siege_started", ref, payload, actors);

        // Also attribute to defending town's nation if different
        if (target != null && target.hasNation()) {
            try {
                Nation defNation = target.getNationOrNull();
                if (defNation != null && (nation == null || !defNation.getUUID().equals(nation.getUUID()))) {
                    Map<String, String> defPayload = new HashMap<>(payload);
                    defPayload.put("role", "defender");
                    EventFire.fire(plugin, "siegewar.siege_started",
                        NationRef.of(defNation.getUUID(), defNation.getName()),
                        defPayload,
                        actors);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSiegeEnd(SiegeEndEvent event) {
        if (!plugin.isEventEnabled("siegewar", "siege-ended")) return;

        Nation nation = event.getNation();
        Map<String, String> payload = new HashMap<>();
        payload.put("siegeType", TextUtil.strip(event.getSiegeType()));
        payload.put("winner", TextUtil.strip(event.getSiegeWinner()));
        payload.put("attacker", TextUtil.strip(event.getAttackerName()));
        payload.put("defender", TextUtil.strip(event.getDefenderName()));
        payload.put("townName", TextUtil.strip(event.getBesiegedTownName()));

        NationRef ref = nation != null
            ? NationRef.of(nation.getUUID(), nation.getName())
            : null;

        EventFire.fire(plugin, "siegewar.siege_ended", ref, payload, List.of());
    }
}
