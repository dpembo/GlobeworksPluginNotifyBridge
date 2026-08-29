package uk.globeworks.bridges.towny;

import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
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

public final class TownyBridgeListener implements Listener {

    private final BridgesPlugin plugin;

    public TownyBridgeListener(BridgesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNewNation(NewNationEvent event) {
        if (!plugin.isEventEnabled("towny", "nation-founded")) return;
        Nation nation = event.getNation();
        if (nation == null) return;

        Map<String, String> payload = new HashMap<>();
        payload.put("nationName", TextUtil.strip(nation.getName()));
        if (nation.getKing() != null) {
            payload.put("king", TextUtil.strip(nation.getKing().getName()));
        }

        EventFire.fire(plugin,
            "towny.nation_founded",
            NationRef.of(nation.getUUID(), nation.getName()),
            payload,
            List.of());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeleteNation(DeleteNationEvent event) {
        if (!plugin.isEventEnabled("towny", "nation-disbanded")) return;

        Map<String, String> payload = new HashMap<>();
        payload.put("nationName", TextUtil.strip(event.getNationName()));

        EventFire.fire(plugin,
            "towny.nation_disbanded",
            NationRef.of(event.getNationUUID(), event.getNationName()),
            payload,
            List.of());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNewTown(NewTownEvent event) {
        if (!plugin.isEventEnabled("towny", "town-founded")) return;
        Town town = event.getTown();
        if (town == null) return;

        Map<String, String> payload = new HashMap<>();
        payload.put("townName", TextUtil.strip(town.getName()));
        payload.put("townId", town.getUUID().toString());

        NationRef nationRef = null;
        if (town.hasNation()) {
            try {
                Nation nation = town.getNationOrNull();
                if (nation != null) {
                    nationRef = NationRef.of(nation.getUUID(), nation.getName());
                    payload.put("nationName", TextUtil.strip(nation.getName()));
                }
            } catch (Exception ignored) {
            }
        }

        EventFire.fire(plugin, "towny.town_founded", nationRef, payload, List.of());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeleteTown(DeleteTownEvent event) {
        if (!plugin.isEventEnabled("towny", "town-disbanded")) return;

        Map<String, String> payload = new HashMap<>();
        payload.put("townName", TextUtil.strip(event.getTownName()));
        payload.put("townId", event.getTownUUID() != null ? event.getTownUUID().toString() : "");

        // Nation may already be gone; use subject-style payload only
        EventFire.fire(plugin, "towny.town_disbanded", null, payload, List.of());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidentJoinTown(TownAddResidentEvent event) {
        Town town = event.getTown();
        Resident resident = event.getResident();
        if (town == null || resident == null) return;

        int townResidents = town.getNumResidents();
        int nationResidents = 0;
        Nation nation = null;
        try {
            nation = town.getNationOrNull();
            if (nation != null) {
                nationResidents = nation.getNumResidents();
            }
        } catch (Exception ignored) {
        }

        UUID playerId = resident.getUUID();

        if (plugin.isEventEnabled("towny", "resident-joined-town")) {
            Map<String, String> payload = new HashMap<>();
            payload.put("townName", TextUtil.strip(town.getName()));
            payload.put("townId", town.getUUID().toString());
            payload.put("player", TextUtil.strip(resident.getName()));
            payload.put("townResidents", String.valueOf(townResidents));
            if (nation != null) {
                payload.put("nationName", TextUtil.strip(nation.getName()));
                payload.put("nationResidents", String.valueOf(nationResidents));
            }

            NationRef nationRef = nation != null
                ? NationRef.of(nation.getUUID(), nation.getName())
                : null;

            EventFire.fire(plugin,
            "towny.resident_joined_town",
                nationRef,
                payload,
                playerId != null ? List.of(playerId) : List.of());
        }

        if (plugin.isEventEnabled("towny", "population-milestone")) {
            fireTownMilestone(town, nation, townResidents, playerId);
            if (nation != null) {
                fireNationMilestone(nation, nationResidents, playerId);
            }
        }
    }

    private void fireTownMilestone(Town town, Nation nation, int count, UUID playerId) {
        List<Integer> milestones = plugin.getConfig()
            .getIntegerList("bridges.towny.population-milestones");
        if (!milestones.contains(count)) return;

        Map<String, String> payload = new HashMap<>();
        payload.put("townName", TextUtil.strip(town.getName()));
        payload.put("townId", town.getUUID().toString());
        payload.put("count", String.valueOf(count));
        payload.put("scope", "town");

        NationRef nationRef = nation != null
            ? NationRef.of(nation.getUUID(), nation.getName())
            : null;

        EventFire.fire(plugin,
            "towny.town_population_milestone",
            nationRef,
            payload,
            playerId != null ? List.of(playerId) : List.of());
    }

    private void fireNationMilestone(Nation nation, int count, UUID playerId) {
        List<Integer> milestones = plugin.getConfig()
            .getIntegerList("bridges.towny.nation-population-milestones");
        if (!milestones.contains(count)) return;

        Map<String, String> payload = new HashMap<>();
        payload.put("nationName", TextUtil.strip(nation.getName()));
        payload.put("count", String.valueOf(count));
        payload.put("scope", "nation");

        EventFire.fire(plugin,
            "towny.nation_population_milestone",
            NationRef.of(nation.getUUID(), nation.getName()),
            payload,
            playerId != null ? List.of(playerId) : List.of());
    }
}
