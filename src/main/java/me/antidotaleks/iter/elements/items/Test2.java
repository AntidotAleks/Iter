package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Conditional;
import me.antidotaleks.iter.utils.items.specific.CooldownGameItem;

import java.awt.*;

public class Test2 extends CooldownGameItem implements Conditional {
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
    public int getCooldown() {
        return 2;
    }

    @Override
    public boolean usable(Point coords) {
        return false;
    }

    @Override
    public void use(Point coords) {

    }

    @Override
    public String getName() {
        return "Test2";
    }

    @Override
    public char getCardSymbol() {
        return '\uE005';
    }
}
