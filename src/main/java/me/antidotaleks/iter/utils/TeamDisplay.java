package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.Game;
import me.antidotaleks.iter.Iter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Arrays;

public final class TeamDisplay {

    private final Game game;
    private final Audience[] teamAudiences;
    private final BossBar[] bossbars;

    public TeamDisplay(Game game) {
        this.game = game;

        Player[][] teamsBukkit = game.getTeamsBukkit();
        int teamAmount = teamsBukkit.length;
        this.teamAudiences = new Audience[teamAmount];
        this.bossbars = new BossBar[teamAmount];

        for (int i = 0; i < teamAmount; i++) {
            this.teamAudiences[i] = getTeamAudience(teamsBukkit[i]);
            this.bossbars[i] = BossBar.bossBar(Component.empty(), BossBar.MAX_PROGRESS, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);

            this.bossbars[i].addViewer(this.teamAudiences[i]);
        }
    }

    // Utils

    private Audience getTeamAudience(Player[] players) {
        return Audience.audience(
                Arrays.stream(players).map(Iter.audiences::player).toList()
        );
    }

    public void updateTeamTurn() {
        for (int bossbarIndex = 0; bossbarIndex < bossbars.length; bossbarIndex++) {
            BossBar bossBar = bossbars[bossbarIndex];
            int currentTeamPlayIndex = game.currentTeamPlay();
            int teamsAmount = game.teamAmount();

            if (bossbarIndex == currentTeamPlayIndex)
                bossBar.name( Component.text("Your turn"));
            else {
                int inXTurns = (bossbarIndex - currentTeamPlayIndex + teamsAmount) % teamsAmount;
                bossBar.name( Component.text( String.format(
                        "Team %s's turn (Yours in %d turn%s)",
                        game.getTeamDetails()[currentTeamPlayIndex], inXTurns, inXTurns == 1 ? "" : "s"
                )));
            }
        }
    }

    public void remove() {
        for (int i = 0; i < bossbars.length; i++) {
            BossBar bossBar = bossbars[i];
            bossBar.removeViewer(teamAudiences[i]);
        }
    }

    // Getters

    public Game getGame() {
        return game;
    }
}
