package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.Game;
import me.antidotaleks.iter.utils.TeamDisplay;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GameTeam {
    // Game players
    private final HashSet<GamePlayer> players;
    private final List<GamePlayer> playersArray;
    // Bukkit Players
    private final HashSet<Player> playersBukkit;
    private final List<Player> playersBukkitArray;
    // Misc
    private final Game game;
    private TeamDisplay teamDisplay;

    public GameTeam(Game game, GamePlayer[] playersInTeam) {
        this.game = game;
        players = (HashSet<GamePlayer>) Arrays.stream(playersInTeam).collect(Collectors.toSet());
        playersArray = players.stream().toList();

        playersBukkit = (HashSet<Player>) players.stream().map(GamePlayer::getPlayer).collect(Collectors.toSet());
        playersBukkitArray = playersBukkit.stream().toList();
    }

    public void createTeamDisplay() {
        if (teamDisplay != null)
            return;

        teamDisplay = new TeamDisplay(this);
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
}
