package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.specific.MovementGameItem;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ItemWalk extends MovementGameItem {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public ItemWalk(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 1;
    }

    @Override
    public boolean usable(@NotNull Point coords, int step) {
        // Walkable distance is 1 tile
        if(tilesAwayTaxi(getCurrentPosition(), coords) != 1)
            return false;

        return super.usable(coords, step);
    }

    @Override
    public String getName() {
        return "Walk";
    }

    @Override
    public char getCardSymbol() {
        return '\uE001';
    }
}
