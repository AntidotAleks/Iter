package me.antidotaleks.iter;

import me.antidotaleks.iter.maps.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class GameManager {
    public static ArrayList<Map> maps = new ArrayList<>();
    public static ArrayList<Game> games = new ArrayList<>();
    public static void startGame(Map map) {
        Location loc = map.buildMap(new Location(Iter.defaultWorld, 0,0,0), new Location(Iter.defaultWorld, 1,1,1)); // Build a map and get its location
        Bukkit.getOnlinePlayers().forEach(player->{
            player.teleport(loc.clone().add(0, 5, 0));
            player.sendMessage("Game started");
        }); // Tp players to it TODO remove
        Game thisGame = new Game(Bukkit.getOnlinePlayers().toArray(new Player[0]), map); // Create new game instance
        games.add(thisGame);
    }
    public static void stopGame(Game game) {
        game.stop();
        games.remove(game);
    }
}
