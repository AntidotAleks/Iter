package me.antidotaleks.iter.utils;

import java.awt.*;

public enum TeamStyling {
    // Pastel colors
    RED    (new Color(217, 93,  93 ),
            new Color(236, 204, 204)),
    BLUE   (new Color(63 , 130, 207),
            new Color(194, 211, 230)),
    GREEN  (new Color(121, 211, 62 ),
            new Color(202, 227, 186)),
    YELLOW (new Color(232, 196, 60 ),
            new Color(230, 222, 189)),
    PINK   (new Color(227, 106, 185),
            new Color(237, 195, 222)),
    CYAN   (new Color(100, 196, 228),
            new Color(210, 229, 236)),
    ORANGE (new Color(219, 133, 37 ),
            new Color(228, 206, 182)),
    PURPLE (new Color(148, 93 , 207),
            new Color(205, 184, 225)),
    WHITE  (new Color(236, 240, 241),
            new Color(248, 248, 250)),
    BLACK  (new Color(52 , 73 , 94 ),
            new Color(213, 213, 216)),
    ;

    public final Color color, lightColor;
    TeamStyling(Color color, Color lightColor) {
        this.color = color;
        this.lightColor = lightColor;
    }

    public static TeamStyling[] getColors(int teamAmount) {
        if(teamAmount > values().length)
            throw new IllegalArgumentException("Too many teams");

        TeamStyling[] colors = new TeamStyling[teamAmount];
        System.arraycopy(values(), 0, colors, 0, teamAmount);
        return colors;
    }
}
