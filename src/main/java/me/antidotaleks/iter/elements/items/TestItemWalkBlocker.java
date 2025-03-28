package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Conditional;
import me.antidotaleks.iter.utils.items.GameItem;
import me.antidotaleks.iter.utils.items.specific.CooldownGameItem;
import me.antidotaleks.iter.utils.items.specific.MovementGameItem;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TestItemWalkBlocker extends CooldownGameItem implements Conditional {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public TestItemWalkBlocker(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 1;
    }

    @Override
    public int getMaxCooldown() {
        return 2;
    }

    @Override
    public boolean usable(@NotNull Point coords) {
        return true;
    }

    @Override
    public void use(Point coords) {

    }

    @Override
    public String getName() {
        return "Test Item Walk Blocker/d";
    }

    @Override
    public char getCardSymbol() {
        return '\uE005';
    }

    @Override
    public boolean isBlocked() {
        return player.getItemsUsed().stream().anyMatch(pair -> pair.getKey() instanceof MovementGameItem);
    }

    @Override
    public boolean isBlocking(GameItem item) {
        return item instanceof MovementGameItem;
    }
}
