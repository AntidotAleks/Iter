package me.antidotaleks.iter.utils.items.specific;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.GameItem;

import java.awt.*;

public interface GameItemInterface {
    int getEnergyUsage();

    /**
     * Checks if the item can be used at the given coordinates
     * @param coords the coordinates to check
     * @return {@code true} if the item can be used at the given coordinates, otherwise {@code false}
     * @see #getEnergyUsage()
     * @see GameItem.TargetSelector#isAcceptable(Point, GamePlayer)
     */
    boolean usable(Point coords);

    void use(Point coords);

    String getName();
}
