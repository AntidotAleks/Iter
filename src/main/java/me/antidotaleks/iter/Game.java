package me.antidotaleks.iter;

import me.antidotaleks.iter.maps.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;

public class Game {
    private final Player[][] players;
    private final Map map;
    private final Location mapLocation;

    public Game(Player[][] players, Map map) {

        this.players = players;
        this.map = map;
        this.mapLocation = map.buildMap();
    }

    public void start() {
        for (int teamI = 0; teamI < map.getTeamsAmount(); teamI++) {
            Player[] team = players[teamI];
            ArrayList<Point> tsp = map.getSpawnPoints(teamI);
            for (int playerI = 0; playerI < team.length; playerI++) {
                Point point = tsp.get(playerI);
                Location spawn = mapLocation.clone().add(point.x, 5, point.y);
                team[playerI].teleport(spawn);
            }
        }
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
