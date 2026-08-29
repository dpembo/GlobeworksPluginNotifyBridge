# GlobeworksBridges

Translates **Towny**, **SiegeWar**, and **LuckPerms** activity into `GenericHistoryEvent` for **NationHistoryBook**.

## Requirements

- GlobeworksAPI (hard)
- Towny / SiegeWar / LuckPerms — soft; each bridge enables only if present + config `enabled: true`

## Config

```yaml
bridges:
  towny:
    enabled: true
    population-milestones: [2, 5, 10, 25]
    nation-population-milestones: [2, 5, 10, 25, 50, 100]
  siegewar:
    enabled: true
  luckperms:
    enabled: true
    notable-groups: []   # fill with group names worth recording
```

## Event types emitted

| Source | eventType |
|--------|-----------|
| Towny | `towny.nation_founded`, `towny.nation_disbanded` |
| Towny | `towny.town_founded`, `towny.town_disbanded` |
| Towny | `towny.resident_joined_town` |
| Towny | `towny.town_population_milestone`, `towny.nation_population_milestone` |
| SiegeWar | `siegewar.siege_started`, `siegewar.siege_ended` |
| LuckPerms | `luckperms.rank_granted`, `luckperms.rank_removed` |

Add matching entries under `events:` in NationHistoryBook `config.yml` for templates/significance.

## Build

```bash
mvn install:install-file -Dfile=libs/globeworks-api-1.0.0.jar \
  -DgroupId=uk.globeworks -DartifactId=globeworks-api -Dversion=1.0.0 -Dpackaging=jar
mvn clean package
```
