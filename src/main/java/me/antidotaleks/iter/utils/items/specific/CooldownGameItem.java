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
        putOnCooldown();
    }

    private int cooldown;

    /**
     * Do not override this method, only {@link #getMaxCooldown()}
     */
    @ApiStatus.Internal
    @Override
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Do not override this method, only {@link #getMaxCooldown()}
     */
    @ApiStatus.Internal
    @Override
    public void putOnCooldown() {
        cooldown = getMaxCooldown();
    }

    /**
     * Do not override this method, only {@link #getMaxCooldown()}
     */
    @ApiStatus.Internal
    @Override
    public void removeCooldown() {
        cooldown = 0;
    }


    /**
     * Do not override this method, only {@link #getMaxCooldown()}
     */
    @ApiStatus.Internal
    @Override
    public void decrementCooldown() {
        cooldown = Math.max(0, cooldown - 1);
    }
}
