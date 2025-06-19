package me.antidotaleks.iter.utils;

import net.kyori.adventure.text.format.TextColor;

import java.awt.*;

public enum TeamStyle {
    // Pastel colors
    RED    (new Color(217, 93,  93 ),
            new Color(246, 220, 220)),
    BLUE   (new Color(63 , 130, 207),
            new Color(224, 233, 243)),
    GREEN  (new Color(121, 211, 62 ),
            new Color(220, 245, 220)),
    YELLOW (new Color(232, 196, 60 ),
            new Color(242, 240, 220)),
    PINK   (new Color(227, 106, 185),
            new Color(246, 225, 239)),
    CYAN   (new Color(100, 196, 228),
            new Color(233, 242, 246)),
    ORANGE (new Color(219, 133, 37 ),
            new Color(241, 230, 219)),
    PURPLE (new Color(148, 93 , 207),
            new Color(230, 220, 240)),
    WHITE  (new Color(236, 240, 241),
            new Color(252, 252, 253)),
    BLACK  (new Color(52 , 73 , 94 ),
            new Color(220, 220, 220)),

    ;

    public final Color color, lightColor;
    public final TextColor textColor, lightTextColor;
    TeamStyle(Color color, Color lightColor) {
        this.color = color;
        this.lightColor = lightColor;
        this.textColor = TextColor.color(color.getRGB());
        this.lightTextColor = TextColor.color(lightColor.getRGB());
    }

    public static TeamStyle[] getColors(int teamAmount) {
        if(teamAmount < 1 || teamAmount > values().length)
            throw new IllegalArgumentException("Team amount must be between 1 and " + values().length);

        TeamStyle[] colors = new TeamStyle[teamAmount];
        System.arraycopy(values(), 0, colors, 0, teamAmount);
        return colors;
    }
}
