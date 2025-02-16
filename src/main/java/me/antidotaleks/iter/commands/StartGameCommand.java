package me.antidotaleks.iter.commands;

import me.antidotaleks.iter.GameManager;
import me.antidotaleks.iter.maps.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class StartGameCommand implements TabExecutor {
    Random random = new Random();
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        while (!GameManager.games.isEmpty()) {
            GameManager.stopGame(GameManager.games.getFirst());
        }// TODO remove later
        System.out.println("Choose random map");
        Map map = GameManager.maps.get(random.nextInt(GameManager.maps.size())); // Choose random map
        System.out.println("Random map chosen");
        GameManager.startGame(map, null); // Start new game on random map. TODO change 'randomness'
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {

        return null;
    }
}
