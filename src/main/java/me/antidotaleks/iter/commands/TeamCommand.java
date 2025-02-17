package me.antidotaleks.iter.commands;

import me.antidotaleks.iter.Teaming;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TeamCommand implements TabExecutor {



    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can use this command");
            return true;
        }

        if (strings.length == 0)
            return true;

        if (strings.length == 1) {
            switch (strings[0]) {
                case "invite":
                    commandSender.sendMessage("/team invite [player]");
                    break;
                case "kick":
                    commandSender.sendMessage("/team kick [player]");
                    break;
                case "join":
                    commandSender.sendMessage("/team join [player]");
                    break;
                case "accept":
                    Teaming.accept((Player) commandSender, null);
                    break;
                case "decline":
                    Teaming.decline((Player) commandSender, null);
                    break;
                case "leave":
                    Teaming.leave((Player) commandSender);
                    break;
                case "list":
                    Teaming.list((Player) commandSender);
                    break;
                default:
                    return true;
            }
        }
        if (strings.length == 2) {
            Player player = Bukkit.getPlayer(strings[1]);
            if (player == null || commandSender.equals(player)) {
                commandSender.sendMessage("You can't do that");
                return true;
            }
            switch (strings[0]) {
                case "invite":
                    Teaming.invite((Player) commandSender, player);
                case "kick":
                    Teaming.kick((Player) commandSender, player);
                case "join":
                    Teaming.requestToJoin((Player) commandSender, player);
                case "accept":

                case "decline":


                    break;
                default:
                    commandSender.sendMessage("Invalid arguments, expected: /team <join|leave|invite|kick|list|accept|decline> [player]");
                    return true;
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        int argIndex = strings.length - 1;
        if (argIndex == 0) {
            return List.of("join", "leave", "invite", "kick", "list", "accept", "decline");
        }
        if (argIndex == 1) {
            switch (strings[0]) {
                case "invite":
                case "kick":
                case "join":
                case "accept":
                case "decline":
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        }
        return List.of();
    }
}
