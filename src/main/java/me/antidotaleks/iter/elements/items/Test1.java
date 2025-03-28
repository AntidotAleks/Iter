package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.specific.CooldownGameItem;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class Test1 extends CooldownGameItem {
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
    public int getCooldown() {
        return 12;
    }

    @Override
    public boolean usable(@NotNull Point coords) {
        return false;
    }

    @Override
    public void use(Point coords) {

    }

    @Override
    public String getName() {
        return "Test1";
    }

    @Override
    public char getCardSymbol() {
        return '\uE004';
    }
}
