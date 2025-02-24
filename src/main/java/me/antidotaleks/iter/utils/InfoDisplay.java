package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.elements.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class InfoDisplay {

    private final GamePlayer gamePlayer;
    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;

    public InfoDisplay(GamePlayer player) {
        this.gamePlayer = player;
        this.player = player.getPlayer();

        // Create scoreboard
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        objective = scoreboard.registerNewObjective(this.player.getName()+"_infoObjIter", Criteria.DUMMY, this.player.getName()+"'s info");
        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        objective.setDisplayName("");

        updateData();

    }

    public void updateData() {
        int health = gamePlayer.getHealth();
        int maxHealth = gamePlayer.getMaxHealth();
        int energy = gamePlayer.getEnergy();
        int maxEnergy = gamePlayer.getMaxEnergy();

        // Update scoreboard
        String infoString = String.format(" §c❤ %d/%d  §r|  §9♦ %d/%d",
                health, maxHealth, energy, maxEnergy);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format(
                "scoreboard objectives modify %s numberformat fixed \"%s\"",
                objective.getName(), infoString
        ));
    }

    public void remove() {
        scoreboard.clearSlot(DisplaySlot.BELOW_NAME);
        objective.unregister();
    }

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public Player getPlayer() {
        return player;
    }
}