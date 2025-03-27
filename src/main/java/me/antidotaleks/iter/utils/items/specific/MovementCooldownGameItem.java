package me.antidotaleks.iter.utils.items.specific;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Cooldown;

public abstract class MovementCooldownGameItem extends MovementGameItem implements Cooldown {
    private int cooldown;

    /**
     * Create a new game item
     *
     * @param player the player that owns the item
     */
    public MovementCooldownGameItem(GamePlayer player) {
        super(player);
    }

    @Override
    public void cooldown() {
        cooldown = getCooldown();
    }

    @Override
    public void undoCooldown() {
        cooldown = 0;
    }

    @Override
    public void decrementCooldown() {
        cooldown--;
    }
}
