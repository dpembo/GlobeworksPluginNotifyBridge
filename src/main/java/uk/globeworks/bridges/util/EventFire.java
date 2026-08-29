package uk.globeworks.bridges.util;

import org.bukkit.Bukkit;
import uk.globeworks.api.GenericHistoryEvent;
import uk.globeworks.api.NationRef;
import uk.globeworks.bridges.BridgesPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EventFire {

    private EventFire() {}

    public static void fire(BridgesPlugin plugin, String eventType, NationRef nation,
                            Map<String, String> payload, List<UUID> actors) {
        try {
            GenericHistoryEvent.Builder b = GenericHistoryEvent.builder(eventType)
                .nation(nation)
                .payload(payload != null ? payload : Map.of())
                .actorIds(actors != null ? actors : List.of());
            GenericHistoryEvent event = b.build();
            Bukkit.getPluginManager().callEvent(event);
            plugin.debug("fired " + eventType
                + " id=" + event.getEventId()
                + (nation != null ? " nation=" + TextUtil.strip(nation.cachedName()) : " nation=null")
                + (payload != null && !payload.isEmpty() ? " payload=" + payload : ""));
        } catch (Throwable t) {
            plugin.getLogger().severe("[Bridges] failed to fire " + eventType + ": " + t.getMessage());
            t.printStackTrace();
        }
    }
}
