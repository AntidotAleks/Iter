package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.Game;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class TopDisplay {

    private final Game game;
    private final BossBar bossBar;

    public TopDisplay(Game game) {
        this.game = game;
        this.bossBar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);

        for (Player[] team : game.getTeamsBukkit()) {
            for (Player player : team)
                bossBar.addPlayer(player);
        }
    }

    // Utils

    public void setTitle(String title) {
        bossBar.setTitle(title);
    }

    public void remove() {
        bossBar.removeAll();
    }

    // Getters

    public Game getGame() {
        return game;
    }
}
