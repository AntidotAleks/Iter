package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.Game;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class GamePlayer {
    private final Player player;
    private final Game game;
    private int maxHealth = 30;
    private int health = maxHealth;
    private int maxEnergy = 3;
    private int energy = maxEnergy;
    private int flatDamage = 0;

    private boolean isDead = false;


    public GamePlayer(Player player, Game game, ConfigurationSection modifiers, double disbalanceModifier) {
        this.player = player;
        this.game = game;
        modifiers(modifiers, disbalanceModifier);
    }

    public void modifiers(ConfigurationSection modifiers, double disbalanceModifier) {
        maxHealth = (int) Math.round(
                maxHealth * modifiers.getDouble("health", 1) * ( disbalanceModifier*.5 + 1 )
        );
        health = (int) Math.round(
                health * modifiers.getDouble("health", 1) * ( disbalanceModifier*.5 + 1)
        );
        maxEnergy = (int) Math.round(
                maxEnergy * modifiers.getDouble("energy", 1) * ( disbalanceModifier + 1)
        );
        flatDamage = (int) Math.round(
                flatDamage + modifiers.getDouble("flatDamage", 0) + disbalanceModifier
        );


    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.clamp(health, 0, maxHealth);
        if (this.health == 0) {
            this.isDead = true;
        }
    }

    public void damage(int finalDamage) {
        setHealth(health - finalDamage);
    }

    public void heal(int amount) {
        setHealth(health + amount);
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.clamp(energy, 0, maxEnergy);
    }

    /**
     * @return true if energy was used, false if not enough energy
     */
    public boolean useEnergy(int energy) {
        if (this.energy < energy) {
            return false;
        }
        setEnergy(this.energy - energy);
        return true;
    }

    public int getFlatDamage() {
        return flatDamage;
    }

    public boolean isDead() {
        return isDead;
    }

    public Player getPlayer() {
        return player;
    }

    public Game getGame() {
        return game;
    }
}
