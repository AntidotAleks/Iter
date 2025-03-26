package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.Conditional;
import me.antidotaleks.iter.elements.GamePlayer;

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
        return "Test3";
    }

    @Override
    public char getCardSymbol() {
        return '\uE006';
    }
}
