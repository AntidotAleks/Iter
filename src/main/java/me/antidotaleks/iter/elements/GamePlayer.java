package me.antidotaleks.iter.elements;

import com.comphenix.protocol.wrappers.Pair;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.items.*;
import me.antidotaleks.iter.events.PlayerFinishTurnEvent;
import me.antidotaleks.iter.utils.FakePlayer;
import me.antidotaleks.iter.utils.InfoDisplay;
import me.antidotaleks.iter.utils.items.Conditional;
import me.antidotaleks.iter.utils.items.Cooldown;
import me.antidotaleks.iter.utils.items.GameItem;
import me.antidotaleks.iter.utils.items.PreUsed;
import me.antidotaleks.iter.utils.items.specific.MovementGameItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static me.antidotaleks.iter.Iter.tryCatch;

public final class GamePlayer implements Listener {

    // Player

    private final Player player;
    private final GameTeam team;
    private final Game game;
    private FakePlayer fakePlayer;
    private InfoDisplay infoDisplay;
    private final Point pos = new Point();

    // Items

    private final ArrayList<Pair<@NotNull GameItem, @NotNull Boolean>> items = new ArrayList<>(); // Pair is modifiable
    private int slotSelected = 0;
    private final ArrayList<Map.Entry<@NotNull GameItem, @NotNull Point>> itemsUsed = new ArrayList<>(); // Map.Entry is not modifiable
    public final ArrayList<Point> stepPlan = new ArrayList<>();

    // Stats

    private int maxHealth = 30;
    private int health = maxHealth;
    private int maxEnergy = 3;
    private int energy = maxEnergy;
    private int flatDamage = 0;

    private boolean isDead = false;


    public GamePlayer(Player player, GameTeam team, Point spawnPosition, ConfigurationSection modifiers, double disbalanceModifier) {

        // Player data

        this.player = player;
        this.team = team;
        this.game = team.getGame();
        modifiers(modifiers, disbalanceModifier);

        // Items

        tryCatch(this::giveStartItems);

        // Setup

        Bukkit.getPluginManager().registerEvents(this, Iter.plugin);
        pos.setLocation(spawnPosition);
    }

    public void gameStart() {
        fakePlayer = new FakePlayer(this);
        infoDisplay = new InfoDisplay(this);

        updateItemBlocks();
        infoDisplay.updateInventory();
    }

    private void giveStartItems() {
        addItemUpdateless(new ItemWalk(this));
        addItemUpdateless(new ItemSwiftStep(this));
        addItemUpdateless(new ItemBasePunch(this));
        addItemUpdateless(new UnusableItemTestForUI(this));
        addItemUpdateless(new MegaCannon(this));
        addItemUpdateless(new FreeWalkingItemTest(this));
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

        infoDisplay.updateSelection();
    }

    // Turns

    boolean canPlay = false;

    public void roundStart() {
        canPlay = true;
        infoDisplay.showCursor();
        updateItemBlocks();
        updateInfo();
    }

    public void finishTurn() {
        canPlay = false;
        infoDisplay.hideCursor();

        Bukkit.getPluginManager().callEvent(new PlayerFinishTurnEvent(this));
    }

    public void roundEnd() {
        itemsUsed.clear();
        stepPlan.clear();
        // Just in case
        for (Pair<GameItem, Boolean> pair : items) {
            if (pair.getFirst() instanceof Cooldown itemCooldown) {
                itemCooldown.decrementCooldown();
                if (itemCooldown.getCooldown() == 0)
                    pair.setSecond(false);
            }
        }

        updateItemBlocks();
        updateInfo();
    }

    // Utils

    public void updateInfo() {
        infoDisplay.update();
    }

    public void stop() {
        infoDisplay.remove();
    }

    public int getNextItemPriority() {
        if (itemsUsed.isEmpty())
            return Integer.MIN_VALUE;

        return itemsUsed.getFirst().getKey().priority();
    }

    /**
     * Uses the next item in the queue. If there is no item to use, returns -1.
     * @return time of the item used
     */
    public int useNextItem() {
        if (itemsUsed.isEmpty())
            return -1;

        GameItem item = itemsUsed.getFirst().getKey();
        item.use(itemsUsed.getFirst().getValue());
        Iter.logger.info("Used item \"" + item.getName()+"\" at tile [" + itemsUsed.getFirst().getValue().x + ", " + itemsUsed.getFirst().getValue().y+"] by " + player.getName());

        itemsUsed.removeFirst();

        return item.useTime();
    }

    private void interact() {
        Point tilePos = getLookTilePosition();
        if (tilePos == null)
            return;

        Pair<GameItem, Boolean> itemBlockPair = items.get(slotSelected);
        GameItem item = itemBlockPair.getFirst();

        if (itemBlockPair.getSecond()) // if blocked
            return;
        if (!item.usable(tilePos, itemsUsed.size()+1)) // if not usable on this tile
            return;
        if (!useEnergy(item.getEnergyUsage())) // if not enough energy
            return;
        if (item instanceof Cooldown itemCooldown && itemCooldown.getCooldown() > 0) // if on cooldown
            return;


        itemsUsed.add(Map.entry(item, tilePos));

        if (item instanceof PreUsed itemPreUsed)
            itemPreUsed.preUse(tilePos);
        if (item instanceof Cooldown itemCooldown)
            itemCooldown.putOnCooldown();

        updateItemBlocks();

        infoDisplay.updateSelection();
        // Iter.logger.info(player.getName()+" used at tile: [" + tilePos.x + ", " + tilePos.y+"]");
    }

    private void undoLast() {
        if(itemsUsed.isEmpty())
            return;

        GameItem lastItem = itemsUsed.getLast().getKey();

        this.restoreEnergy(lastItem.getEnergyUsage());

        if (lastItem instanceof PreUsed itemPreUsed)
            itemPreUsed.undoPreUse();
        if (lastItem instanceof Cooldown itemCooldown)
            itemCooldown.removeCooldown();

        itemsUsed.removeLast();
        updateItemBlocks();
        // Iter.logger.info(player.getName()+" undo use");
    }

    private void updateItemBlocks() {
        items.forEach(itemPairToCheck ->
                itemPairToCheck.setSecond(
                        (itemPairToCheck.getFirst() instanceof Cooldown itemCd && itemCd.getCooldown() > 0)
                                ||
                        itemPairToCheck.getFirst() instanceof Conditional conditional && conditional.isBlocked()
                                ||
                        itemsUsed.stream()
                        .map(Map.Entry::getKey)
                        .anyMatch(item -> item instanceof Conditional conditional &&
                                conditional.isBlocking(itemPairToCheck.getFirst()))
                )
        );

        infoDisplay.updateSelection();
    }

    /**
     * Teleports actual player to given location. For in-game player movement use {@link #setPosition(Point)}
     * @param loc world location
     */
    public void teleport(Location loc) {
        List<Entity> passengers = player.getPassengers();
        passengers.forEach(player::removePassenger);
        loc.setDirection(player.getLocation().getDirection());
        player.teleport(loc);
        passengers.forEach(player::addPassenger);
    }

    private void addItemUpdateless(GameItem item) {
        items.add(new Pair<>(item, false));
    }

    // Getters/Setters

    public void addItem(GameItem item) {
        addItemUpdateless(item);
        updateItemBlocks();
        infoDisplay.updateInventory();
    }

    public Point getPosition() {
        return (Point) pos.clone();
    }

    public Point getPositionAtStep(int step) {
        if (step <= 0 || stepPlan.isEmpty()) // if player haven't moved yet or if there is no step plan (players on other team)
            return getPosition();

        ArrayList<Integer> stepList = getStepList(itemsUsed);

        if (stepList.getLast() == -1)
            return getPosition();

        // If player haven't used "step" amount of cards, get their last position
        if (stepList.size() <= step)
            return (Point) this.stepPlan.get(stepList.getLast()).clone();

        // Returns player's position at given step
        return (Point) this.stepPlan.get(stepList.get(step)).clone();
    }

    private static ArrayList<Integer> getStepList(List<Map.Entry<GameItem, Point>> uses) {
        // Each value = previous value + 1 if current GameItem is ItemWalk
        ArrayList<Integer> steps = new ArrayList<>();
        steps.add(-1);
        for (Map.Entry<GameItem, Point> use : uses)
            steps.add(steps.getLast() + (use.getKey() instanceof MovementGameItem ? 1 : 0));

        return steps;
    }

    /**
     * Moves in-game player position to a given point including linked {@link FakePlayer}.
     * For teleporting actual player use {@link #teleport(Location)}
     * @param pos point on a {@link Game} map
     */
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

    public GameTeam getTeam() {
        return team;
    }

    public List<Pair<GameItem, Boolean>> getItems() {
        return List.copyOf(items);
    }

    public int getCurrentItemIndex() {
        return slotSelected;
    }

    public List<Map.Entry<GameItem, Point>> getItemsUsed() {
        return Collections.unmodifiableList(itemsUsed);
    }

    public List<Point> getStepPlanning() {
        return Collections.unmodifiableList(stepPlan);
    }

    public FakePlayer getFakePlayer() {
        return fakePlayer;
    }
}
