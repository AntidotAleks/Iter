package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.Game;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.items.*;
import me.antidotaleks.iter.events.PlayerFinishTurnEvent;
import me.antidotaleks.iter.utils.FakePlayer;
import me.antidotaleks.iter.utils.InfoDisplay;
import me.antidotaleks.iter.utils.TeamStyling;
import me.antidotaleks.iter.utils.items.GameItem;
import me.antidotaleks.iter.utils.items.PreUsed;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class GamePlayer implements Listener {

    // Player

    private final Player player;
    private final Game game;
    private final TeamStyling teamStyling;
    private final FakePlayer fakePlayer;
    private final InfoDisplay infoDisplay;
    private final Point pos = new Point();

    // Items

    private final ArrayList<GameItem> items = new ArrayList<>();
    private int slotSelected = 0;
    private final ArrayList<Map.Entry<GameItem, Point>> itemsUsed = new ArrayList<>();
    public final ArrayList<Point> nextStep = new ArrayList<>();
    // Necessary items
    public final ItemWalk itemWalk;

    // Stats

    private int maxHealth = 30;
    private int health = maxHealth;
    private int maxEnergy = 3;
    private int energy = maxEnergy;
    private int flatDamage = 0;

    private boolean isDead = false;


    public GamePlayer(Player player, Game game, Point spawnPosition, ConfigurationSection modifiers, double disbalanceModifier) {

        // Player data

        this.player = player;
        this.game = game;
        modifiers(modifiers, disbalanceModifier);

        itemWalk = new ItemWalk(this);
        manageItems();

        teamStyling = game.getTeamDetails(player);
        fakePlayer = new FakePlayer(this);
        infoDisplay = new InfoDisplay(this);

        // Setup

        player.setPlayerListName(ChatColor.of(teamStyling.color) +"["+ teamStyling +"] "+ ChatColor.of(teamStyling.lightColor) + player.getName());
        Bukkit.getPluginManager().registerEvents(this, Iter.plugin);
        setPosition(spawnPosition);

        // Items
    }

    private void manageItems() {
        items.add(itemWalk);
        items.add(new ItemSwiftStep(this));
        items.add(new ItemBasePunch(this));
        items.add(new Test1(this));
        items.add(new Test2(this));
        items.add(new Test3(this));
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

    // Events

    /* RMB - use item, LMB - undo last item */
    @EventHandler
    public void playerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(player) || !canPlay || event.getHand() != EquipmentSlot.HAND)
            return;
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_AIR)
            interact();
        else
            undoLast();

        updateInfo();
    }

    /* Q - finish turn */
    @EventHandler
    public void finishTurnFromEvent(PlayerDropItemEvent event) {
        if (!event.getPlayer().equals(player))
            return;
        event.setCancelled(true);
        if(!canPlay)
            return;

        finishTurn();
    }

    /* F - next item */
    @EventHandler
    public void nextItemFromEvent(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().equals(player))
            return;
        event.setCancelled(true);

        if (!player.isSneaking())
            slotSelected = (slotSelected + 1) % items.size();
        else
            slotSelected = (slotSelected - 1 + items.size()) % items.size();

        infoDisplay.updateCards();
    }

    // Turns

    boolean canPlay = false;

    public void startTurn() {
        canPlay = true;
        infoDisplay.showCursor();
    }

    public void finishTurn() {
        canPlay = false;
        infoDisplay.hideCursor();

        Bukkit.getPluginManager().callEvent(new PlayerFinishTurnEvent(this));
    }

    // Utils

    public void updateInfo() {
        infoDisplay.updateData();
    }

    public void stop() {
        infoDisplay.remove();
        fakePlayer.remove();
    }

    public boolean useNextItem() {
        if (itemsUsed.isEmpty())
            return false;

        GameItem item = itemsUsed.getFirst().getKey();
        item.use(itemsUsed.getFirst().getValue());
        itemsUsed.removeFirst();

        Iter.logger.info("Used item: " + item.getClass().getName()+", now updating info");
        return true;
    }

    private void interact() {
        Point tilePos = getLookTilePosition();
        if (tilePos == null)
            return;

        GameItem item = items.get(slotSelected);

        if (!item.usable(tilePos))
            return;
        if(!useEnergy(item.getEnergyUsage()))
            return;

        itemsUsed.add(Map.entry(item, tilePos));

        if (item instanceof PreUsed)
            ((PreUsed) item).preUse(tilePos);


        Iter.logger.info("Interact at tile: [" + tilePos.x + ", " + tilePos.y+"]");
    }

    private void undoLast() {
        if(itemsUsed.isEmpty())
            return;

        GameItem lastItem = itemsUsed.getLast().getKey();

        this.restoreEnergy(lastItem.getEnergyUsage());

        if (lastItem instanceof PreUsed)
            ((PreUsed) lastItem).undoPreUse();

        itemsUsed.removeLast();
    }

    private void fillTileAround(boolean blocked) {
        Location tilePos = getWorldPosition();

        BlockData from = blocked ? Iter.AIR_DATA : Iter.BARRIER_DATA,
                    to = blocked ? Iter.BARRIER_DATA : Iter.AIR_DATA;

        tilePos.add(-2, 1, -1);

        for (int i = 0; i < 3; i++) {
            replaceFromTo(tilePos, from, to);
            tilePos.add(4, 0, 0);
            replaceFromTo(tilePos, from, to);
            tilePos.add(-4, 0, 1);
        }
        for (int i = 0; i < 3; i++) {
            tilePos.add(1, 0, -4);
            replaceFromTo(tilePos, from, to);
            tilePos.add(0, 0, 4);
            replaceFromTo(tilePos, from, to);
        }

    }

    private void replaceFromTo(Location loc, BlockData from, BlockData to) {
        if(loc.getBlock().getBlockData().equals(from))
            player.sendBlockChange(loc, to);
    }

    public void teleport(Location loc) {
        List<Entity> passengers = player.getPassengers();
        passengers.forEach(player::removePassenger);
        loc.setDirection(player.getLocation().getDirection());
        player.teleport(loc);
        passengers.forEach(player::addPassenger);
    }

    // Getters/Setters

    public Point getPosition() {
        return pos;
    }

    public void setPosition(Point pos) {
        this.pos.setLocation(pos);
        fakePlayer.teleport(getWorldPosition());
    }

    public Location getWorldPosition() {
        return game.toWorldLocation(pos);
    }

    public Point getLookTilePosition() {
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

    public Location getLookTileWorldPosition() {
        Point lookTilePos = getLookTilePosition();
        return game.toWorldLocation(lookTilePos);
    }

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

    public List<GameItem> getItems() {
        return List.copyOf(items);
    }

    public int getCurrentItemIndex() {
        return slotSelected;
    }

    public GameItem getCurrentItem() {
        return items.get(slotSelected);
    }

    public List<Map.Entry<GameItem, Point>> getItemsUsed() {
        return Collections.unmodifiableList(itemsUsed);
    }

    public List<Point> getStepPlanning() {
        return Collections.unmodifiableList(nextStep);
    }

    public int getTeamIndex() {
        return game.getTeamIndex(player);
    }

    public TeamStyling getTeamDetails() {
        return teamStyling;
    }

    public FakePlayer getFakePlayer() {
        return fakePlayer;
    }
}
