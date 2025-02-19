package me.antidotaleks.iter;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.maps.Map;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Game {
    private final GamePlayer[][] teams;
    private final Player[][] teamsBukkit;
    private int currentTeamPlay = 0;
    private final int[] teamPlayOrder;
    private final Map map;
    private final Location mapLocation;

    public Game(Player[][] players, Map map) {
        this.map = map;
        this.mapLocation = map.buildMap();
        this.teams = new GamePlayer[players.length][];
        this.teamsBukkit = players;
        this.teamPlayOrder = map.teamPlayOrder();


        for (int i = 0; i < teamsBukkit.length; i++) {

            Player[] team = teamsBukkit[i];
            ArrayList<GamePlayer> teamList = new ArrayList<>(Arrays.asList(teams[i]));

            for (Player player : team) {
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(true);
                teamList.add(
                        new GamePlayer(player, this, map.getModifiers(i), teamDisbalance(i))
                );
            }
            teams[i] = teamList.toArray(new GamePlayer[0]);
        }
    }



    public void start() {
        for (int teamI = 0; teamI < map.getTeamsAmount(); teamI++) {
            // Get the team and their spawn points
            Player[] team = teamsBukkit[teamI];
            ArrayList<Point> tsp = map.getSpawnPoints(teamI);
            // Shuffle the spawn points
            Collections.shuffle(tsp);
            // Teleport players to their spawn points
            for (int playerI = 0; playerI < team.length; playerI++) {
                Point point = tsp.get(playerI);
                Location spawn = mapLocation.clone().add(point.x*3+5.5, 1, point.y*3+5.5);
                team[playerI].teleport(spawn);
            }
        }
    }

    public void stop() {
        map.removeMap(mapLocation);

        for (Player[] team : teamsBukkit) {
            for (Player player : team) {
                Location spawn = player.getWorld().getSpawnLocation();
                player.teleport(spawn);
            }
        }
    }
    public int teamDisbalance(int teamI) {
        return 0;
    }

    public void stepPlayIndex() {
        currentTeamPlay = (++currentTeamPlay)%teamPlayOrder.length;
    }
}
