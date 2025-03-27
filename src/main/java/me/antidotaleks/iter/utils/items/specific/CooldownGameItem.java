package me.antidotaleks.iter.utils.items.specific;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Cooldown;
import me.antidotaleks.iter.utils.items.GameItem;

public abstract class CooldownGameItem extends GameItem implements Cooldown {
    /**
     * Create a new game item
     *
     * @param player the player that owns the item
     */
    public CooldownGameItem(GamePlayer player) {
        super(player);
    }

    private int cooldown;

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
