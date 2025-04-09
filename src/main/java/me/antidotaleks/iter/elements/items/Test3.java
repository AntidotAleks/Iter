package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.utils.items.Conditional;
import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.GameItem;

public class Test3 extends ItemWalk implements Conditional {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public Test3(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 0;
    }

    @Override
    public String getName() {
        return "Free Walk Test Item No Blocks";
    }

    @Override
    public char getCardSymbol() {
        return '\uE006';
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    @Override
    public boolean isBlocking(GameItem item) {
        return false;
    }
}
