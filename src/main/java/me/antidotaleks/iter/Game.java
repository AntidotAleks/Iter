package me.antidotaleks.iter;

import me.antidotaleks.iter.maps.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Game {
    private final Player[][] players;
    private final Map map;
    private final Location mapLocation;

    public Game(Player[][] players, Map map) {

        this.players = players;
        this.map = map;
        this.mapLocation = map.buildMap();
    }

    public void stop() {
        map.removeMap(mapLocation);
        for (Player[] team : players) {
            for (Player player : team) {
                player.teleport(mapLocation.getWorld().getSpawnLocation());
            }
        }
    }
}
