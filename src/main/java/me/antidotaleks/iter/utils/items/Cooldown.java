package me.antidotaleks.iter.utils.items;

public interface Cooldown {
    /**
     * @return the cooldown of the item in rounds
     */
    int getCooldown();
    void cooldown();
    void undoCooldown();
    void decrementCooldown();
}
