package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Conditional;
import me.antidotaleks.iter.utils.items.GameItem;
import me.antidotaleks.iter.utils.items.specific.CooldownGameItem;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class UnusableItemTestForUI extends CooldownGameItem implements Conditional {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public UnusableItemTestForUI(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 10;
    }

    @Override
    public int getMaxCooldown() {
        return 12;
    }

    @Override
    public boolean usable(@NotNull Point coords, int step) {
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

    @Override
    public boolean isBlocked() {
        return true;
    }

    @Override
    public boolean isBlocking(GameItem item) {
        return false;
    }
}
