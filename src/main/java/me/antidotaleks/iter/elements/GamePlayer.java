package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.Game;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.utils.InfoDisplay;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;

import java.awt.*;

public final class GamePlayer implements Listener {
    private final Player player;
    private final Game game;
    private final InfoDisplay infoDisplay;

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
        infoDisplay = new InfoDisplay(this);
    }



    public void modifiers(ConfigurationSection modifiers, double disbalanceModifier) {
        if (modifiers == null)
            modifiers = null;

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

    // Events

    @EventHandler
    public void playerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(player))
            return;

        Point tilePos = getTilePos();
        if (tilePos == null)
            return;

        Iter.logger.info("Tile pos: " + tilePos);
    }

    // Utils

    public Point getTilePos() {
        if (player == null)
            return null;

        if (player.getLocation().getPitch() <= 0) // cancel if player looks up
            return null;


        RayTraceResult result = player.rayTraceBlocks(66);
        if (result == null || result.getHitBlock() == null || result.getHitBlock().getLocation().getBlockY() != 0)
            return null;

        Location interactLoc = result.getHitBlock().getLocation();
        interactLoc.subtract(this.game.getMapLocation()).subtract(4, 0, 4).multiply(1d/3);

        Point coords = new Point(interactLoc.getBlockX(), interactLoc.getBlockZ());

        if(game.getMap().isWall(coords.x, coords.y))
            return null;

        return coords;
    }

    public void updateInfo() {
        infoDisplay.updateData();
    }

    public void stop() {
        infoDisplay.remove();
    }

    // Getters/Setters

    public InfoDisplay getInfoDisplay() {
        return infoDisplay;
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

    public void restoreEnergy(int energy) {
        setEnergy(this.energy + energy);
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
