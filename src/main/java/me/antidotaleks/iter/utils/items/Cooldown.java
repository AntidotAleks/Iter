package me.antidotaleks.iter.utils.items;

public interface Cooldown {
    /**
     * @return the cooldown of the item in rounds
     */
    int getMaxCooldown();
    int getCooldown();
    void putOnCooldown();
    void removeCooldown();
    void decrementCooldown();
}
