package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.specific.MovementCooldownGameItem;

public class ItemSwiftStep extends MovementCooldownGameItem {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public ItemSwiftStep(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 0;
    }

    @Override
    public int getCooldown() {
        return 2;
    }

    @Override
    public String getName() {
        return "Swift Step";
    }

    @Override
    public char getCardSymbol() {
        return '\uE003';
    }
}
