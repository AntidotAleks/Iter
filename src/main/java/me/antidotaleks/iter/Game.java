package me.antidotaleks.iter;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.events.PlayerFinishTurnEvent;
import me.antidotaleks.iter.maps.Map;
import me.antidotaleks.iter.utils.TeamDetails;
import me.antidotaleks.iter.utils.TeamDisplay;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Game implements Listener {
    // Teams
    private final GamePlayer[][] teams;
    private final Player[][] teamsBukkit;
    private final TeamDetails[] teamDetails;
    // Turns
    private int currentTeamPlayIndex = 0;
    private final int[] teamPlayOrder;
    // Map
    private final Map map;
    private final Location mapLocation;
    // Other
    TeamDisplay teamDisplay;

    public Game(Player[][] playersInTeams, Map map) {
        Iter.logger.info("Creating game with map " + map.getDisplayName());
        this.map = map;
        this.mapLocation = map.buildMap();
        this.teamPlayOrder = map.teamPlayOrder();
        this.teamDetails = TeamDetails.getColors(playersInTeams.length);

        // shuffle teams by teamPlayOrder

        List<Player[]> teamList = Arrays.asList(playersInTeams);
        Collections.shuffle(teamList);
        teamsBukkit = teamList.toArray(new Player[0][0]);

        // Create GamePlayers and register events for them

        this.teams = new GamePlayer[teamsBukkit.length][];
        for (int teamIndex = 0; teamIndex < teamsBukkit.length; teamIndex++) {

            // Get the team and their info

            GamePlayer[] team = teams[teamIndex] = new GamePlayer[teamsBukkit[teamIndex].length];

            final Player[] teamBukkit = teamsBukkit[teamIndex];
            final ArrayList<Point> teamSpawnPoints = map.getSpawnPoints(teamIndex);
            final ConfigurationSection teamModifiers = map.getModifiers(teamIndex);

            // Set vanilla player settings

            for (Player player : teamBukkit) {
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.addPotionEffect(
                        new org.bukkit.potion.PotionEffect(
                                PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false
                        )
                );
            }

            // Create GamePlayers

            for (int playerIndex = 0; playerIndex < teamBukkit.length; playerIndex++)
                team[playerIndex] = new GamePlayer(teamBukkit[playerIndex], this, (Point) teamSpawnPoints.get(playerIndex).clone(), teamModifiers, 0.0);

            for (GamePlayer player : team)
                player.teleport(player.getWorldPosition().add(0,3.5,0));
        }

    }

    public Map getMap() {
        return map;
    }



    public void start() {
        Iter.logger.info("Starting game");

        for (int teamIndex = 0; teamIndex < map.getTeamsAmount(); teamIndex++) {
            // Get the team and their spawn points
            GamePlayer[] team = teams[teamIndex];

            // Teleport players to their spawn points
            for (int playerIndex = 0; playerIndex < team.length; playerIndex++) {
                GamePlayer gamePlayer = teams[teamIndex][playerIndex];

                gamePlayer.getInfoDisplay().mount();
                gamePlayer.setPosition(gamePlayer.getPosition());
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

    private void roundStart() {
        teamDisplay.updateTeamTurn();
        getAllPlayers().forEach(player -> player.sendTitle(" ", "Team "+ teamDetails[currentTeamPlay()].toString() +" turn", 5, 35, 5));

        playersFinishedTurn.addAll(List.of(teams[currentTeamPlay()]));

        for (GamePlayer player : teams[currentTeamPlay()]) {
            player.startTurn();
        }
    }

    private void roundEnd() {

        new BukkitRunnable() { @Override public void run() {
            boolean allItemsUsed = true;

            for (GamePlayer player : teams[currentTeamPlay()]) {
                allItemsUsed = !player.useNextItem() && allItemsUsed;
                // Becomes false if at least 1 player had an item to use
            }

            if(allItemsUsed) {
                for (GamePlayer player : teams[currentTeamPlay()]) {
                    player.setEnergy(player.getMaxEnergy());
                    player.updateInfo();
                }

                cancel();
                stepPlayIndex();
                roundStart();
            }
        }}.runTaskTimer(Iter.plugin, 0, 10);
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

        roundEnd();
    }
    
    // Utils

    public int teamDisbalance(int teamI) {
        return 0;
    }

    public void stepPlayIndex() {
        currentTeamPlayIndex = (++currentTeamPlayIndex)%teamPlayOrder.length;
    }

    public int currentTeamPlay() {
        return currentTeamPlayIndex;
    }

    public int teamAmount() {
        return teams.length;
    }

    public Location toWorldLocation(Point point) {
        if (point == null)
            return null;
        return mapLocation.clone().add(point.x*3+5.5, 1, point.y*3+5.5);
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

    public TeamDetails[] getTeamDetails() {
        return teamDetails;
    }

    public TeamDetails getTeamDetails(Player player) {
        return teamDetails[getTeamIndex(player)];
    }

    public List<Player> getAllPlayers() {
        return Arrays.stream(teamsBukkit).flatMap(Arrays::stream).toList();
    }
}
