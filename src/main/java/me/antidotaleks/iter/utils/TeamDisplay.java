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
        for (int bossbarIndex = 0; bossbarIndex < bossBars.length; bossbarIndex++) {
            BossBar bossBar = bossBars[bossbarIndex];
            int currentTeamPlayIndex = game.currentTeamPlay();
            int teamsAmount = game.teamAmount();

            if (bossbarIndex == currentTeamPlayIndex)
                bossBar.setTitle("Your turn");
            else {
                int inXTurns = (bossbarIndex - currentTeamPlayIndex + teamsAmount) % teamsAmount;
                bossBar.setTitle(String.format(
                        "Team %s's turn (Yours in %d turn%s)",
                        game.getTeamDetails()[currentTeamPlayIndex], inXTurns, inXTurns == 1 ? "" : "s"
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
