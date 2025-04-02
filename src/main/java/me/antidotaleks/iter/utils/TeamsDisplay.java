package me.antidotaleks.iter.utils;

import me.antidotaleks.iter.Game;
import me.antidotaleks.iter.Iter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.entity.Player;

import java.util.Arrays;

import static me.antidotaleks.iter.Iter.MONO_OFFSET_FONTS;
import static me.antidotaleks.iter.Iter.offset;
import static net.kyori.adventure.text.Component.text;

public final class TeamsDisplay {

    private final Game game;
    private final Audience[] teamAudiences;
    private final BossBar[] bossbars;
    private final Component[][] bossbarTexts;

    public TeamsDisplay(Game game) {
        this.game = game;

        Player[][] teamsBukkit = game.getTeamsBukkit();
        int teamAmount = teamsBukkit.length;
        this.teamAudiences = new Audience[teamAmount];
        this.bossbars = new BossBar[teamAmount];
        this.bossbarTexts = new Component[teamAmount][];

        for (int i = 0; i < teamAmount; i++) {
            this.teamAudiences[i] = getTeamAudience(teamsBukkit[i]);
            this.bossbars[i] = BossBar.bossBar(Component.empty(), BossBar.MAX_PROGRESS, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
            this.bossbarTexts[i] = new Component[teamAmount];

            this.bossbars[i].addViewer(this.teamAudiences[i]);
        }

        setupBossbarTexts();
    }

    // Utils

    private Audience getTeamAudience(Player[] players) {
        return Audience.audience(
                Arrays.stream(players).map(Iter.audiences::player).toList()
        );
    }

    public void updateTeamTurn() {
        for (int bossbarIndex = 0; bossbarIndex < bossbars.length; bossbarIndex++) {
            BossBar bossbar = bossbars[bossbarIndex];
            int currentTeamPlayIndex = game.currentTeamPlay();
            int teamsAmount = game.teamAmount();

            Component bossbarText = bossbarTexts[bossbarIndex][currentTeamPlayIndex];

            updateBossbarText(bossbar, bossbarText);
        }
    }

    private void setupBossbarTexts() {
        int teamAmount = bossbars.length;
        TeamStyling[] teamStylings = game.getTeamStylings();

        for (int currentTurn = 0; currentTurn < teamAmount; currentTurn++)
            for (int currentTeamIndex = 0; currentTeamIndex < teamAmount; currentTeamIndex++) {
                bossbarTexts[currentTurn][currentTeamIndex] =
                        getBossbarText(currentTurn, currentTeamIndex, teamAmount, teamStylings);
            }
    }

    private static final Style TOPBAR_FONT = Style.empty().font(Key.key("topbar"));
    private static final String[] TOPBAR_BACKGROUND_SYMBOLS = new String[]{"\uE000", "\uE001"};
    private Component getBossbarText(int currentTurn, int currentTeamIndex, int teamAmount, TeamStyling[] teamStylings) {
        Component bossbarText = Component.empty();

        for (TeamStyling teamStyling : teamStylings) {
            final String name = teamStyling.name();

            bossbarText = bossbarText
                    .append(text(TOPBAR_BACKGROUND_SYMBOLS[0], TOPBAR_FONT.color(teamStyling.lightTextColor)))          // Frame
                    .append(offset(-44))                                                                                // Frame left side
                    .append(text(TOPBAR_BACKGROUND_SYMBOLS[1], TOPBAR_FONT.color(teamStyling.textColor)))               // Painted transparent background
                    .append(offset(-21 - name.length() * 3))                                                            // center + text padding
                    .append(text(name, MONO_OFFSET_FONTS[0].color(teamStyling.lightTextColor)))                         // Team name
                    .append(offset(21 - name.length() * 3 + 2));                                                        // Frame right side (padding) + frame padding
        }

        final int inXTurns = (currentTeamIndex - currentTurn + teamAmount) % teamAmount;
        String turnText = currentTurn == currentTeamIndex ? "Your turn" :
                String.format(
                        "Team %s's turn (Yours in %d turn%s)",
                        teamStylings[currentTurn].name(), inXTurns, inXTurns == 1 ? "" : "s"
                );

        bossbarText = bossbarText
                .append(offset(-teamAmount*45))                                                                         // Left side
                .append(text(turnText, MONO_OFFSET_FONTS[2]))                                                           // Turn text
                .append(offset(teamAmount*45 - turnText.length() * 6));                                                 // Right side (padding)

        return bossbarText.compact();
    }

    private void updateBossbarText(BossBar bossbar, Component bossbarText) {
        bossbar.name(bossbarText);
    }

    public void remove() {
        for (int i = 0; i < bossbars.length; i++) {
            BossBar bossBar = bossbars[i];
            bossBar.removeViewer(teamAudiences[i]);
            bossbars[i] = null;
        }
    }
}
