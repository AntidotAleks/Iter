package me.antidotaleks.iter;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.events.PlayerFinishTurnEvent;
import me.antidotaleks.iter.maps.Map;
import me.antidotaleks.iter.utils.TeamDisplay;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Game implements Listener {
    // Teams
    private final GamePlayer[][] teams;
    private final Player[][] teamsBukkit;
    // Turns
    private int currentTeamPlayIndex = 0;
    private final int[] teamPlayOrder;
    // Map
    private final Map map;
    private final Location mapLocation;
    // Other
    TeamDisplay teamDisplay;

    public Game(Player[][] players, Map map) {
        this.map = map;
        this.mapLocation = map.buildMap();
        this.teams = new GamePlayer[players.length][];
        this.teamsBukkit = players;
        this.teamPlayOrder = map.teamPlayOrder();


        for (int i = 0; i < teamsBukkit.length; i++) {

            Player[] team = teamsBukkit[i];
            int tempI = i;
            teams[i] = Arrays.stream(teamsBukkit[i])
                    .map(player -> new GamePlayer(player, this, map.getModifiers(tempI), 0))
                    .toArray(GamePlayer[]::new);

            for (Player player : team) {
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(true);
            }

            for (GamePlayer gamePlayer : teams[i]) {
                Bukkit.getPluginManager().registerEvents(gamePlayer, Iter.plugin);
            }
        }
    }

    public Map getMap() {
        return map;
    }



    public void start() {
        for (int teamIndex = 0; teamIndex < map.getTeamsAmount(); teamIndex++) {
            // Get the team and their spawn points
            GamePlayer[] team = teams[teamIndex];
            ArrayList<Point> tsp = map.getSpawnPoints(teamIndex);
            // Shuffle the spawn points
            Collections.shuffle(tsp);
            // Teleport players to their spawn points
            for (int playerIndex = 0; playerIndex < team.length; playerIndex++) {
                Point point = tsp.get(playerIndex); // Get team's spawn point at index playerIndex
                Location spawn = mapLocation.clone().add(point.x*3+5.5, 1, point.y*3+5.5); // Transform the point to a location
                GamePlayer gamePlayer = teams[teamIndex][playerIndex];

                gamePlayer.getPlayer().teleport(spawn);
                gamePlayer.getInfoDisplay().mount();

            }
        }

        teamDisplay = new TeamDisplay(this);
        roundStart();
    }

    public void stop() {
        map.removeMap(mapLocation);
        teamDisplay.remove();

        for (GamePlayer[] team : teams) {
            for (GamePlayer player : team) {
                HandlerList.unregisterAll(player);
                player.stop();
            }
        }

        for (Player[] team : teamsBukkit) {
            for (Player player : team) {
                player.teleport(mapLocation.getWorld().getSpawnLocation());
            }
        }

    }

    public void roundStart() {
        teamDisplay.updateTeamTurn();
        Arrays.stream(teamsBukkit).flatMap(Arrays::stream).forEach(player -> player.sendTitle(" ", "Team "+ (currentTeamPlay()+1) +" turn", 5, 35, 5));

        playersFinishedTurn.addAll(List.of(teams[currentTeamPlay()]));

        for (GamePlayer player : teams[currentTeamPlay()]) {
            player.startTurn();
        }
    }

    ArrayList<GamePlayer> playersFinishedTurn = new ArrayList<>();
    @EventHandler 
    public void playerFinishTurn(PlayerFinishTurnEvent event) {
        if(!playersFinishedTurn.contains(event.getPlayer()))
            return;

        playersFinishedTurn.remove(event.getPlayer());

        Iter.logger.info(String.format(
                "Player %s finished turn. %s",
                event.getPlayer().getPlayer().getName(), (playersFinishedTurn.isEmpty()) ? "All players finished their turn" : "Left: " + playersFinishedTurn.size()
        ));


        if (!playersFinishedTurn.isEmpty())
            return;

        stepPlayIndex();
        roundStart();
    }
    
    // Utils

    public int teamDisbalance(int teamI) {
        return 0;
    }

    public void stepPlayIndex() {
        currentTeamPlayIndex = (++currentTeamPlayIndex)%teamPlayOrder.length;
    }

    public int currentTeamPlay() {
        return teamPlayOrder[currentTeamPlayIndex];
    }

    public int teamAmount() {
        return teams.length;
    }

    // Getters

    /**
     * Get the map location
     * @return the map location is not cloned!
     */
    public Location getMapLocation() {
        return mapLocation;
    }

    public GamePlayer getPlayer(Player player) {
        return Arrays.stream(teams)
                .flatMap(Arrays::stream)
                .filter(gamePlayer -> gamePlayer.getPlayer().equals(player))
                .findFirst()
                .orElse(null);
    }

    public GamePlayer getPlayer(Point coords) {
        for (GamePlayer[] team : teams) {
            for (GamePlayer player : team) {
                if (player.getPosition().equals(coords))
                    return player;
            }
        }
        return null;
    }

    /**
     * Get the team of a player
     * @param player the player
     * @return the team of the player
     * @throws IllegalArgumentException if the player is not in the game, shouldn't happen
     */
    public GamePlayer[] getTeam(Player player) {
        return Arrays.stream(teams)
                .filter(team -> Arrays.stream(team).anyMatch(gamePlayer -> gamePlayer.getPlayer().equals(player)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not in the game"));
    }

    public int getTeamIndex(Player player) {
        for (int i = 0; i < teamsBukkit.length; i++) {
            if (Arrays.asList(teamsBukkit[i]).contains(player)) {
                return i;
            }
        }
        return -1;
    }

    public Player[][] getTeamsBukkit() {
        return teamsBukkit;
    }

    public enum TeamColor {
        // Pastel colors
        RED    (new Color(217, 93,  93 ),
                new Color(236, 204, 204)),
        BLUE   (new Color(63 , 130, 207),
                new Color(194, 211, 230)),
        GREEN  (new Color(121, 211, 62 ),
                new Color(202, 227, 186)),
        YELLOW (new Color(232, 196, 60 ),
                new Color(230, 222, 189)),
        PINK   (new Color(227, 106, 185),
                new Color(237, 195, 222)),
        ORANGE (new Color(219, 133, 37 ),
                new Color(228, 206, 182)),
        CYAN   (new Color(100, 196, 228),
                new Color(210, 229, 236)),
        PURPLE (new Color(148, 93 , 207),
                new Color(205, 184, 225)),
        WHITE  (new Color(236, 240, 241),
                new Color(248, 248, 250)),
        BLACK  (new Color(52 , 73 , 94 ),
                new Color(213, 213, 216)),
        ;

        public final Color color, lightColor;
        TeamColor(Color color, Color lightColor) {
            this.color = color;
            this.lightColor = lightColor;
        }

        public static TeamColor[] getColors(int teamAmount) {
            TeamColor[] colors = new TeamColor[teamAmount];
            for (int i = 0; i < teamAmount; i++) {
                colors[i] = values()[i];
            }
            return colors;
        }
    }
}
