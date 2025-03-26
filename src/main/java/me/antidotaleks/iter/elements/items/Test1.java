package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.utils.items.Cooldown;
import me.antidotaleks.iter.elements.GamePlayer;

public class Test1 extends ItemWalk implements Cooldown {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public Test1(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 10;
    }

    @Override
    public String getName() {
        return "Test1";
    }

    @Override
    public char getCardSymbol() {
        return '\uE004';
    }

    @Override
    public int getCooldown() {
        return 12;
    }
}
