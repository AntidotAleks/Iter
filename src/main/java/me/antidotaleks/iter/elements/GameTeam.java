package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.utils.TeamDisplay;
import me.antidotaleks.iter.utils.TeamStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static me.antidotaleks.iter.Iter.tryCatch;

public class GameTeam {
    // Game players
    private final HashSet<GamePlayer> players;
    private final List<GamePlayer> playersArray;
    // Bukkit Players
    private final HashSet<Player> playersBukkit;
    private final List<Player> playersBukkitArray;
    // Misc
    public final Game game;
    private TeamDisplay teamDisplay;
    private final int teamIndex;
    public final TeamStyle style;

    public GameTeam(Game game, int teamIndex, Player[] players) {
        this.game = game;
        this.teamIndex = teamIndex;
        this.style = game.getTeamStyle(teamIndex);

        // Create GamePlayers

        final Iterator<Point> teamSpawnPoints = game.getMap().getSpawnPoints(teamIndex).iterator();
        final ConfigurationSection teamModifier = game.getMap().getModifiers(teamIndex);

        GamePlayer[] gamePlayers = Arrays.stream(players).map(
                player -> new GamePlayer(player, this, teamSpawnPoints.next(), teamModifier, 0.0)
        ).toArray(GamePlayer[]::new);

        // Create lists and sets

        this.players = (HashSet<GamePlayer>) Arrays.stream(gamePlayers).collect(Collectors.toSet());
        this.playersArray = this.players.stream().toList();

        this.playersBukkit = (HashSet<Player>) Arrays.stream(players).collect(Collectors.toSet());
        this.playersBukkitArray = playersBukkit.stream().toList();
    }

    // Utils

    public int getTeamHealthSum() {
        return getPlayers().stream()
                .mapToInt(GamePlayer::getHealth)
                .sum();
    }

    public int getTeamMaxHealthSum() {
        return getPlayers().stream()
                .mapToInt(GamePlayer::getMaxHealth)
                .sum();
    }

    // Getters

    public List<GamePlayer> getPlayers() {
        return playersArray;
    }

    public List<Player> getPlayersBukkit() {
        return playersBukkitArray;
    }

    public boolean hasPlayer(GamePlayer player) {
        return players.contains(player);
    }

    public boolean hasPlayer(Player player) {
        return playersBukkit.contains(player);
    }

    public void forEachPlayer(Consumer<? super GamePlayer> action) {
        players.forEach(action);
    }

    public void forEachPlayerBukkit(Consumer<? super Player> action) {
        playersBukkit.forEach(action);
    }

    public Game getGame() {
        return game;
    }

    public int getTeamIndex() {
        return teamIndex;
    }


    // Team display

    public void createDisplay() {
        if (teamDisplay != null)
            return;

        teamDisplay = new TeamDisplay(this);
    }

    public void updateTeamDisplay() {
        teamDisplay.updateBossbar();
    }

    public void stop() {
        if(teamDisplay == null)
            return;

        teamDisplay.remove();
        teamDisplay = null;

        players.forEach(player -> {
            HandlerList.unregisterAll(player);
            tryCatch(player::stop);
        });
    }
}
