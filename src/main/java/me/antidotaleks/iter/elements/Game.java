package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.events.PlayerFinishTurnEvent;
import me.antidotaleks.iter.maps.Map;
import me.antidotaleks.iter.utils.RoundCompletionProcessor;
import me.antidotaleks.iter.utils.TeamStyle;
import me.antidotaleks.iter.utils.items.GameItem;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.antidotaleks.iter.Iter.tryCatch;

public class Game implements Listener {
    // Teams
    private final List<GameTeam> teams;
    private final TeamStyle[] teamStyles;
    // Turns
    private int currentTeamPlayIndex = 0;
    // Map
    private final Map map;
    private final Location mapLocation;


    public Game(Player[][] playersInTeams, Map map) {
        Iter.logger.info("Creating game with map " + map.getDisplayName());
        this.map = map;
        this.mapLocation = map.buildMap();
        this.teamStyles = TeamStyle.getColors(playersInTeams.length);

        // Create teams array to set the order of teams

        GameTeam[] teamsArray = new GameTeam[playersInTeams.length];
        int[] teamPlayOrder = map.teamPlayOrder();

        for (int teamIndex = 0; teamIndex < playersInTeams.length; teamIndex++) {

            // Create GameTeams

            GameTeam team = new GameTeam(this, teamPlayOrder[teamIndex], playersInTeams[teamIndex]);
            teamsArray[teamPlayOrder[teamIndex]] = team;

            // Setup players

            team.forEachPlayer(player ->
                    player.teleport(player.getWorldPosition().add(0, 3.5, 0))
            );

            team.forEachPlayerBukkit(player -> {
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.addPotionEffect(
                        new org.bukkit.potion.PotionEffect(
                                PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false)
                );
            });
        }

        // Convert teamsArray to teams
        teams = Arrays.asList(teamsArray);

    }

    public Map getMap() {
        return map;
    }



    public void startGame() {
        Iter.logger.info("Starting game");
        teams.forEach(team -> {
            team.forEachPlayer(GamePlayer::gameStart);
            team.createDisplay();
        });

        roundStart();
    }

    public void stopGame() {
        map.removeMap(mapLocation);
        teams.forEach(GameTeam::removeTeamDisplay);

        teams.forEach(team -> {
            team.forEachPlayer(player -> {
                HandlerList.unregisterAll(player);
                tryCatch(player::stop);
            });

            team.forEachPlayerBukkit(player ->
                player.teleport(mapLocation.getWorld().getSpawnLocation())
            );
        });
    }

    private void roundStart() {
        teams.forEach(GameTeam::updateTeamDisplay);
        getAllBukkitPlayers().forEach(player -> player.sendTitle(" ", "Team "+ teamStyles[currentTeamPlayIndex()].toString() +" turn", 5, 35, 5));

        GameTeam nextTeam = teams.get(currentTeamPlayIndex);
        playersLeftThisTurn.addAll(nextTeam.getPlayers());
        nextTeam.forEachPlayer(GamePlayer::roundStart);
    }

    private void roundFinishing() {
        new RoundCompletionProcessor(teams.get(currentTeamPlayIndex), this::roundEnd).start();
    }

    private void roundEnd() {
        teams.get(currentTeamPlayIndex).forEachPlayer(player -> {
            player.roundEnd();
            player.setEnergy(player.getMaxEnergy());
            player.updateInfo();
        });

        incrementPlayIndex();
        roundStart();
    }

    ArrayList<GamePlayer> playersLeftThisTurn = new ArrayList<>();
    @EventHandler 
    public void playerFinishTurn(PlayerFinishTurnEvent event) {
        if(!playersLeftThisTurn.contains(event.getPlayer()))
            return;

        playersLeftThisTurn.remove(event.getPlayer());

        Iter.logger.info(String.format(
                "Player %s finished turn. %s",
                event.getPlayer().getPlayer().getName(), (playersLeftThisTurn.isEmpty()) ? "All players finished their turn" : "Left: " + playersLeftThisTurn.size()
        ));

        // If all players finished their turn, finish round

        if (playersLeftThisTurn.isEmpty())
            roundFinishing();
    }
    
    // Utils

    public int teamDisbalance(int teamI) {
        return 0;
    }

    public void incrementPlayIndex() {
        currentTeamPlayIndex = (++currentTeamPlayIndex)%teams.size();
    }

    public int currentTeamPlayIndex() {
        return currentTeamPlayIndex;
    }

    public GameTeam currentTeamPlay() {
        return teams.get(currentTeamPlayIndex);
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
        return getAllGamePlayers().stream()
                .filter(gamePlayer -> gamePlayer.getPlayer().equals(player))
                .findFirst()
                .orElse(null);
    }

    /**
     * Use in {@link GameItem#use(Point)}
     * @param coords coordinates, where the item was used
     * @return player at given position, {@code null} if there is no player at given coords
     */
    public GamePlayer getPlayer(Point coords) {
        return getAllGamePlayers().stream()
                .filter(player -> player.getPosition().equals(coords))
                .findFirst().orElse(null);
    }

    /**
     * Use in {@link GameItem#usable(Point, int)}
     * @param coords coordinates, where the item was used
     * @param step ordinal player's item use
     * @return player at given position in specific time (step), {@code null} if there is no player at given coords and time
     */
    public GamePlayer getPlayer(Point coords, int step) {
        if (step <= 0)
            return getPlayer(coords);

        return getAllGamePlayers().stream()
                .filter(player -> player.getPositionAtStep(step).equals(coords))
                .findFirst().orElse(null);
    }

    /**
     * Get the team of a player
     * @param player the player
     * @return the team of the player
     * @throws IllegalArgumentException if the player is not in the game, shouldn't happen
     */
    public GameTeam getTeam(Player player) {
        return teams.stream()
                .filter(team -> team.hasPlayer(player))
                .findFirst().orElse(null);
    }

    public GameTeam getTeam(GamePlayer player) {
        return teams.stream()
                .filter(team -> team.hasPlayer(player))
                .findFirst().orElse(null);
    }

    public int getTeamIndex(Player player) {
        return teams.indexOf(getTeam(player));
    }

    public int getTeamIndex(GamePlayer player) {
        return teams.indexOf(getTeam(player));
    }

    public int getTeamIndex(GameTeam team) {
        return teams.indexOf(team);
    }

    public List<GameTeam> getTeams() {
        return teams;
    }

    public int getTeamsAmount() {
        return teams.size();
    }

    public TeamStyle[] getTeamStyles() {
        return teamStyles;
    }

    public TeamStyle getTeamStyle(int teamIndex) {
        return teamStyles[teamIndex];
    }

    public List<GamePlayer> getAllGamePlayers() {
        return teams.stream().flatMap(team -> team.getPlayers().stream()).toList();
    }

    public List<Player> getAllBukkitPlayers() {
        return teams.stream().flatMap(team -> team.getPlayersBukkit().stream()).toList();
    }
}
