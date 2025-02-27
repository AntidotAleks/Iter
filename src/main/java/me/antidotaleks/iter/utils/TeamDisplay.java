package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.Game;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class TeamDisplay {

    private final Game game;
    private final BossBar[] bossBars;

    public TeamDisplay(Game game) {
        this.game = game;

        this.bossBars = new BossBar[game.getTeamsBukkit().length];

        for (int i = 0; i < game.getTeamsBukkit().length; i++) {
            this.bossBars[i] = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);

            for (Player player : game.getTeamsBukkit()[i])
                bossBars[i].addPlayer(player);
        }
    }

    // Utils

    public void updateTeamTurn() {
        for (int i = 0; i < bossBars.length; i++) {
            BossBar bossBar = bossBars[i];
            int currentTeamIndex = game.currentTeamPlay();
            int teamsAmount = game.teamAmount();

            if (i == currentTeamIndex)
                bossBar.setTitle("Your turn");
            else {
                int inXTurns = (i - currentTeamIndex + teamsAmount) % teamsAmount;
                bossBar.setTitle(String.format(
                        "Team %d's turn (Yours in %d turn%s)",
                        currentTeamIndex + 1, inXTurns, inXTurns == 1 ? "" : "s"
                ));
            }
        }
    }

    public void remove() {
        for (BossBar bossBar : bossBars)
            bossBar.removeAll();
    }

    // Getters

    public Game getGame() {
        return game;
    }
}
