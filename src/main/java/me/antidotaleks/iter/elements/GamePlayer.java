package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.Game;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.events.PlayerFinishTurnEvent;
import me.antidotaleks.iter.utils.InfoDisplay;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.RayTraceResult;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class GamePlayer implements Listener {

    // Player

    private final Player player;
    private final Game game;
    private final InfoDisplay infoDisplay;
    private final Point pos = new Point(0, 0);

    // Items

    private final ArrayList<GameItem> items = new ArrayList<>();
    private int slotSelected = 0;
    private final ArrayList<Map.Entry<GameItem, Point>> itemsUsed = new ArrayList<>();
    // Necessary items
    private final ItemWalk itemWalk;

    // Stats

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

        // Items
        itemWalk = new ItemWalk(this);

        items.add(itemWalk);
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
        if (!event.getPlayer().equals(player) || !canPlay)
            return;

        Point tilePos = getTilePos();
        if (tilePos == null)
            return;

        GameItem item = items.get(slotSelected);

        if (!item.usable(tilePos))
            return;
        if(!useEnergy(item.getEnergyUsage()))
            return;

        itemsUsed.add(Map.entry(item, tilePos));
        this.updateInfo();

        if (item instanceof PreUsed)
            ((PreUsed) item).preUse(tilePos);


        Iter.logger.info("Tile pos: [" + tilePos.x + ", " + tilePos.y+"]");
    }

    @EventHandler
    public void undo(PlayerDropItemEvent event) {
        if (!event.getPlayer().equals(player) || !canPlay)
            return;
        event.setCancelled(true);

        if(itemsUsed.isEmpty())
            return;

        // If shift+q, undo all
        if (!player.isSneaking()) {
            undoLast();
            this.updateInfo();
            return;
        }

        while (!itemsUsed.isEmpty())
            undoLast();
        this.updateInfo();
    }

    @EventHandler
    public void nextItemAndTurn(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().equals(player))
            return;
        event.setCancelled(true);

        if(player.isSneaking()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                            "Finished turn")
            );
            finishTurn();
            return;
        }

        slotSelected = (slotSelected + 1) % items.size();
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                "Selected item: " + List.of(items.get(slotSelected).getClass().getName().split("\\.")).getLast() +
                " (" + (slotSelected+1) + "/" + items.size() + ")")
        );
    }

    // Turns

    boolean canPlay = false;

    public void startTurn() {
        canPlay = true;
    }

    public void finishTurn() {
        Bukkit.getPluginManager().callEvent(new PlayerFinishTurnEvent(this));
        canPlay = false;
    }

    // Utils

    public Point getPosition() {
        return pos;
    }

    private void undoLast() {
        this.restoreEnergy(itemsUsed.getLast().getKey().getEnergyUsage());

        GameItem lastItem = itemsUsed.getLast().getKey();
        if (lastItem instanceof PreUsed)
            ((PreUsed) lastItem).undoPreUse();

        itemsUsed.removeLast();
    }

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

    public GamePlayer[] getTeam() {
        return game.getTeam(player);
    }

    public List<Map.Entry<GameItem, Point>> getItemsUsed() {
        return Collections.unmodifiableList(itemsUsed);
    }

    public List<Point> getStepPlanning() {
        return itemWalk.getStepPlanning();
    }

    public int getTeamIndex() {
        return game.getTeamIndex(player);
    }
}
