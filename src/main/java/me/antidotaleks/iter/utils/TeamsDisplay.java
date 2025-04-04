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
    private final TeamStyling[] teamStylings;

    public TeamsDisplay(Game game) {
        this.game = game;

        Player[][] teamsBukkit = game.getTeamsBukkit();
        int teamAmount = teamsBukkit.length;
        this.teamAudiences = new Audience[teamAmount];
        this.bossbars = new BossBar[teamAmount];
        // this.bossbarTexts = new Component[teamAmount][];
        // this.teamStylings = game.getTeamStylings();
        this.bossbarTexts = new Component[5][];
        this.teamStylings = TeamStyling.getColors(5); // TODO: temp

        for (int i = 0; i < teamAmount; i++) {
            this.teamAudiences[i] = getTeamAudience(teamsBukkit[i]);
            this.bossbars[i] = BossBar.bossBar(Component.empty(), BossBar.MAX_PROGRESS, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
            // this.bossbarTexts[i] = new Component[teamAmount]; // TODO: temp

            this.bossbars[i].addViewer(this.teamAudiences[i]); // Show the bossbar for current turn
        }
        for (int i = 0; i < 5; i++) {
            this.bossbarTexts[i] = new Component[5];
        } //TODO: temp

        setupBossbarTexts();

        game.getAllGamePlayers().forEach(player -> player.getInfoDisplay().showTopBars());
    }

    // Utils

    private Audience getTeamAudience(Player[] players) {
        return Audience.audience(
                Arrays.stream(players).map(Iter.audiences::player).toList()
        );
    }

    public void updateBossbars() {
        for (int bossbarIndex = 0; bossbarIndex < bossbars.length; bossbarIndex++) {
            BossBar bossbar = bossbars[bossbarIndex];
            int currentTeamPlayIndex = game.currentTeamPlay();
            int teamsAmount = game.teamAmount();

            Component bossbarText = bossbarTexts[bossbarIndex][currentTeamPlayIndex];

            updateBossbarText(bossbar, bossbarText);
        }
    }

    private void setupBossbarTexts() {
        int teamAmount = teamStylings.length;

        for (int currentTurn = 0; currentTurn < teamAmount; currentTurn++)
            for (int currentTeamIndex = 0; currentTeamIndex < teamAmount; currentTeamIndex++) {
                bossbarTexts[currentTeamIndex][currentTurn] =
                        getBossbarText(currentTurn, currentTeamIndex, teamAmount, teamStylings);
            }
    }

    private static final Style TOPBAR_FONT = Style.empty().font(Key.key("topbar"));
    private static final String[]
            TOPBAR_S_TEAM = new String[]{"\uE000", "\uE001", "\uE002"},
            TOPBAR_S_TURNS = new String[]{"\uE003", "\uE004", "\uE005", "\uE006", "\uE007", "\uE008"};
    private static Component getBossbarText(int currentTeamPlaying, int currentTeamShownTo, int teamAmount, TeamStyling[] teamStylings) {
        Component bossbarText = Component.empty();

        for (int i = 0; i < teamStylings.length; i++) {
            TeamStyling teamStyling = teamStylings[i];
            final String name = teamStyling.name();

            bossbarText = bossbarText
                    .append(text(TOPBAR_S_TEAM[0], TOPBAR_FONT.color(teamStyling.lightTextColor)))
                    .append(offset(-44))
                    .append(text(TOPBAR_S_TEAM[1], TOPBAR_FONT.color(teamStyling.textColor)))
                    .append(offset(-21 - name.length() * 3))
                    .append(text(name, MONO_OFFSET_FONTS[0]))
                    .append((currentTeamShownTo != i) ? offset(22 - name.length() * 3 + 2) : // Every other team
                            offset(-22 - name.length() * 3)
                                    .append(text(TOPBAR_S_TEAM[2], TOPBAR_FONT))
                                    .append(offset(4)) // Player's team
                    );
        }

        Style colored_font = TOPBAR_FONT.color(teamStylings[currentTeamPlaying].lightTextColor);

        if (currentTeamPlaying == currentTeamShownTo) {
            bossbarText = bossbarText
                    .append(offset(-46*(teamAmount - currentTeamShownTo)))
                    .append(text(TOPBAR_S_TURNS[2], colored_font))
                    .append(offset(-46*(currentTeamShownTo + 1) + 5));

            return bossbarText.compact();
        }

        final int inXTurns = (currentTeamShownTo - currentTeamPlaying + teamAmount) % teamAmount;
        final boolean positive = currentTeamPlaying < currentTeamShownTo;

        if (positive) {
            final int lineLength = inXTurns - 1;
            // Spacing and pointer of current team playing
            bossbarText = bossbarText
                    .append(offset((-teamAmount + currentTeamPlaying) * 46))
                    .append(text(TOPBAR_S_TURNS[0], colored_font))
                    .append(offset(-1));

            // Lines
            for (int i = 0; i < lineLength; i++)
                bossbarText = bossbarText
                        .append(text(TOPBAR_S_TURNS[5], colored_font))
                        .append(offset(-1));

            // pointer of this team and spacing
            bossbarText = bossbarText
                    .append(text(TOPBAR_S_TURNS[1], colored_font))
                    .append(offset(-46*currentTeamShownTo - 24));


        } else {
            final int leftLineLength = currentTeamShownTo - 1,
            rightLineLength = teamAmount - currentTeamPlaying - 1;

            // Left edge
            bossbarText = bossbarText
                    .append(offset(-46*teamAmount - 5))
                    .append(text(TOPBAR_S_TURNS[3], colored_font))
                    .append(offset(-1));

            // Lines on the left
            for (int i = 0; i < leftLineLength; i++)
                bossbarText = bossbarText
                        .append(text(TOPBAR_S_TURNS[5], colored_font))
                        .append(offset(-1));

            // pointers and spacing
            bossbarText = bossbarText
                    .append(text(TOPBAR_S_TURNS[1], colored_font))
                    .append(offset((teamAmount - inXTurns - 1) * 46 + 20))
                    .append(text(TOPBAR_S_TURNS[0], colored_font))
                    .append(offset(-1));

            // Lines on the right
            for (int i = 0; i< rightLineLength; i++)
                bossbarText = bossbarText
                        .append(text(TOPBAR_S_TURNS[5], colored_font))
                        .append(offset(-1));

            // Right edge
            bossbarText = bossbarText
                    .append(text(TOPBAR_S_TURNS[4], colored_font))
                    .append(offset(-46*teamAmount - 3));
        }


        return bossbarText.compact();
    }

    private void updateBossbarText(BossBar bossbar, Component bossbarText) {
        for (int i = 0; i < bossbars.length; i++) {
            String percentage = (int) Math.ceil(((double) game.getTeamHealth(i) / game.getTeamMaxHealth(i)) * 100) + "%";

            // Percentages
            bossbarText = bossbarText
                    .append(offset(22 - (int) Math.floor(percentage.length() * 2.5)))
                    .append(text(percentage, TOPBAR_FONT))
                    .append(offset(22 - (int) Math.ceil(percentage.length() * 2.5) + 3));
        }

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
