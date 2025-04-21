package me.antidotaleks.iter;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.elements.GameTeam;
import me.antidotaleks.iter.events.PlayerFinishTurnEvent;
import me.antidotaleks.iter.maps.Map;
import me.antidotaleks.iter.utils.TeamStyling;
import me.antidotaleks.iter.utils.TeamDisplay;
import me.antidotaleks.iter.utils.items.GameItem;
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
import java.util.List;
import java.util.*;

import static me.antidotaleks.iter.Iter.tryCatch;

public class Game implements Listener {
    // Teams
    private final List<GameTeam> teams = new ArrayList<>();
    private final TeamStyling[] teamStylings;
    private TeamDisplay teamDisplay;
    // Turns
    private int currentTeamPlayIndex = 0;
    private final int[] teamPlayOrder;
    // Map
    private final Map map;
    private final Location mapLocation;


    public Game(Player[][] playersInTeams, Map map) {
        Iter.logger.info("Creating game with map " + map.getDisplayName());
        this.map = map;
        this.mapLocation = map.buildMap();
        this.teamPlayOrder = map.teamPlayOrder();
        this.teamStylings = TeamStyling.getColors(playersInTeams.length);

        // shuffle teams by teamPlayOrder

        List<Player[]> teamList = Arrays.asList(playersInTeams);
        Collections.shuffle(teamList);


        for (int teamIndex = 0; teamIndex < playersInTeams.length; teamIndex++) {

            // Get the team and their info

            final Iterator<Point> teamSpawnPoints = map.getSpawnPoints(teamIndex).iterator();
            final ConfigurationSection teamModifier = map.getModifiers(teamIndex);

            // Create GameTeams

            GamePlayer[] players = Arrays.stream(playersInTeams[teamIndex]).map(
                    player -> new GamePlayer(player, this, teamSpawnPoints.next(), teamModifier, 0.0)
            ).toArray(GamePlayer[]::new);

            GameTeam team = new GameTeam(this, players);
            teams.add(team);

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

    }

    public Map getMap() {
        return map;
    }



    public void startGame() {
        Iter.logger.info("Starting game");
        teams.forEach(team -> {
            team.forEachPlayer(player -> player.getInfoDisplay().mount());
            team.createTeamDisplay();
        });

        roundStart();
    }

    public void stopGame() {
        map.removeMap(mapLocation);
        teamDisplay.remove();

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
        teamDisplay.updateBossbar();
        getAllBukkitPlayers().forEach(player -> player.sendTitle(" ", "Team "+ teamStylings[currentTeamPlayIndex()].toString() +" turn", 5, 35, 5));

        GameTeam nextTeam = teams.get(currentTeamPlayIndex);
        playersFinishedTurn.addAll(nextTeam.getPlayers());
        nextTeam.forEachPlayer(GamePlayer::roundStart);
    }

    private void roundFinishing() {
        GameTeam team = teams.get(currentTeamPlayIndex);

        new BukkitRunnable() { @Override public void run() {
            final boolean[] allItemsUsed = {true};
            team.forEachPlayer(player -> {
                allItemsUsed[0] = !player.useNextItem() && allItemsUsed[0];
                // Becomes false if at least 1 player had an item to use
            });
            teamDisplay.updateBossbar();
            getAllGamePlayers().forEach(GamePlayer::updateInfo);

            if(allItemsUsed[0]) {
                cancel();
                roundEnd();
            }
        }}.runTaskTimer(Iter.plugin, 0, 10);
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

        // If all players finished their turn, finish round

        if (!playersFinishedTurn.isEmpty())
            roundFinishing();
    }
    
    // Utils

    public int teamDisbalance(int teamI) {
        return 0;
    }

    public void incrementPlayIndex() {
        currentTeamPlayIndex = (++currentTeamPlayIndex)%teamPlayOrder.length;
    }

    public int currentTeamPlayIndex() {
        return currentTeamPlayIndex;
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

    public int getTeamIndex(Player player) {
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

    public TeamStyling[] getTeamStylings() {
        return teamStylings;
    }

    public TeamStyling getTeamDetails(Player player) {
        return teamStylings[getTeamIndex(player)];
    }

    public List<GamePlayer> getAllGamePlayers() {
        return teams.stream().flatMap(team -> team.getPlayers().stream()).toList();
    }

    public List<Player> getAllBukkitPlayers() {
        return teams.stream().flatMap(team -> team.getPlayersBukkit().stream()).toList();
    }
}
