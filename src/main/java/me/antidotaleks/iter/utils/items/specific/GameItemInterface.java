package me.antidotaleks.iter.utils.items.specific;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.GameItem;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface GameItemInterface {
    int getEnergyUsage();

    /**
     * Checks if the item can be used at the given coordinates
     * @param coords the coordinates to check
     * @return {@code true} if the item can be used at the given coordinates, otherwise {@code false}
     * @see #getEnergyUsage()
     * @see GameItem.TargetSelector#canUseAt(Point, GamePlayer, int)
     */
    boolean usable(@NotNull Point coords, int step);

    void use(Point coords);

    String getName();
}
