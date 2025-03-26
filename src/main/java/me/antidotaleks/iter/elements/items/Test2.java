package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.Conditional;
import me.antidotaleks.iter.elements.Cooldown;
import me.antidotaleks.iter.elements.GamePlayer;

public class Test2 extends ItemWalk implements Cooldown, Conditional {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public Test2(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 3;
    }

    @Override
    public String getName() {
        return "Test2";
    }

    @Override
    public char getCardSymbol() {
        return '\uE005';
    }

    @Override
    public int getCooldown() {
        return 2;
    }
}
