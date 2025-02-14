package me.antidotaleks.iter;

import me.antidotaleks.iter.maps.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Game {
    private final Player[] players;
    private final Map map;

    public Game(Player[] players, Map map) {
        this.players = players;
        this.map = map;
    }

    private static final Location defaultPosition = new Location(Iter.defaultWorld,0,1,0);

    public void stop() {
        map.removeMap();
    }
}
