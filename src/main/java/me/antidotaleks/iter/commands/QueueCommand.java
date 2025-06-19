package me.antidotaleks.iter.commands;

import me.antidotaleks.iter.GameManager;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.Teaming;
import me.antidotaleks.iter.events.PlayerCancelQueueEvent;
import me.antidotaleks.iter.events.PlayerQueueEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class QueueCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("Only players can use this command");
            return true;
        }
        if (!Teaming.isHost(player)) {
            player.sendMessage("You are not a host");
            return true;
        }
        if (strings.length == 0) {
            player.sendMessage("/start [start|stop]");
            return true;
        }

        switch (strings[0].toLowerCase()) {
            case "start":
                Player[] team = Teaming.getTeamWithHost(player);
                try {
                    PlayerQueueEvent event = new PlayerQueueEvent(player, team, GameManager.MAP_NAMES);
                    Bukkit.getPluginManager().callEvent(event);
                } catch (IllegalArgumentException e) {
                    Iter.logger.warning(e.getMessage());
                    player.sendMessage("Failed to start queue");
                    return true;
                }
                player.sendMessage("Queue started");
                break;
            case "stop":
                try {
                    PlayerCancelQueueEvent event = new PlayerCancelQueueEvent(player);
                    Bukkit.getPluginManager().callEvent(event);
                } catch (IllegalArgumentException e) {
                    Iter.logger.warning(e.getMessage());
                    player.sendMessage("Failed to stop queue");
                    return true;
                }
                player.sendMessage("Queue stopped");
                break;
            case "stopallgames":
                player.sendMessage("Stopping all games");
                for (int i = 0; i < GameManager.games.size(); i++)
                    GameManager.stopGame(GameManager.games.getLast());
                
                player.sendMessage("All games stopped");
                break;
            default:
                player.sendMessage("/start [start|stop]");
                return true;
        }


        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        if(strings.length == 1) {
            return List.of("start", "stop", "stopAllGames");
        }
        return null;
    }
}
