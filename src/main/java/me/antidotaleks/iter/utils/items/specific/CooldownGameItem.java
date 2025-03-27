package me.antidotaleks.iter.utils.items.specific;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Cooldown;
import me.antidotaleks.iter.utils.items.GameItem;
import org.jetbrains.annotations.ApiStatus;

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

    /**
     * Do not override this method, only {@link #getCooldown()}
     */
    @ApiStatus.Internal
    @Override
    public void cooldown() {
        cooldown = getCooldown();
    }

    /**
     * Do not override this method, only {@link #getCooldown()}
     */
    @ApiStatus.Internal
    @Override
    public void undoCooldown() {
        cooldown = 0;
    }


    /**
     * Do not override this method, only {@link #getCooldown()}
     */
    @ApiStatus.Internal
    @Override
    public void decrementCooldown() {
        cooldown--;
    }
}
