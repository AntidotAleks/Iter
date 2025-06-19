package me.antidotaleks.iter.elements;

import com.comphenix.protocol.wrappers.Pair;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.items.*;
import me.antidotaleks.iter.events.PlayerFinishTurnEvent;
import me.antidotaleks.iter.utils.FakePlayer;
import me.antidotaleks.iter.utils.InfoDisplay;
import me.antidotaleks.iter.utils.StepPlan;
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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;

import static me.antidotaleks.iter.Iter.tryCatch;

public final class GamePlayer implements Listener {

    // Player

    public final Player bukkitPlayer;
    public final GameTeam team;
    public final Game game;
    private FakePlayer fakePlayer;
    private InfoDisplay infoDisplay;
    private final Point pos = new Point();

    // Items

    /** List of items in player's inventory. Each item is a pair of {@link GameItem} and boolean for if item being blocked. */
    private final ArrayList<Pair<@NotNull GameItem, @NotNull Boolean>> items = new ArrayList<>(); // Pair is modifiable
    private int slotSelected = 0;
    /** List of items used in this turn. Each item is a pair of {@link GameItem} and {@link Point} for the tile it was used on. */
    private final ArrayDeque<Map.Entry<@NotNull GameItem, @NotNull Point>> itemsUsed = new ArrayDeque<>(); // Map.Entry is not modifiable
    public final StepPlan stepPlan;

    // Stats

    private int maxHealth = 30;
    private int health = maxHealth;
    private int maxEnergy = 3;
    private int energy = maxEnergy;
    private int flatDamage = 0;

    private boolean isAlive = true;


    public GamePlayer(Player bukkitPlayer, GameTeam team, Point spawnPosition, ConfigurationSection modifiers, double disbalanceModifier) {

        // Player data

        this.bukkitPlayer = bukkitPlayer;
        this.team = team;
        this.game = team.getGame();
        this.stepPlan = new StepPlan(this);
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
        infoDisplay.updateInventoryItemData();
        bukkitPlayer.getInventory().setHeldItemSlot(4);
    }

    private void giveStartItems() {
        addItemUpdateless(new ItemWalk(this));
        addItemUpdateless(new ItemSwiftStep(this));
        addItemUpdateless(new ItemBasePunch(this));
        addItemUpdateless(new UnusableItemTestForUI(this));
        addItemUpdateless(new MegaCannon(this));
        // addItemUpdateless(new FreeWalkingItemTest(this));
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
    public void playerInteractFromEvent(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(bukkitPlayer) || !canPlay || event.getHand() != EquipmentSlot.HAND)
            return;
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_AIR)
            interact();
        else
            undoLast();
        game.gameDisplay.updateForPlayer(this);

        updateInfo();
    }

    /* F - finish turn */
    @EventHandler
    public void finishTurnFromEvent(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().equals(bukkitPlayer))
            return;
        event.setCancelled(true);

        if(!canPlay)
            return;

        finishTurn();
    }

    /* Scroll wheel - change item */
    @EventHandler
    public void changeItemSelectedFromEvent(PlayerItemHeldEvent event) {
        if (!event.getPlayer().equals(bukkitPlayer))
            return;
        event.setCancelled(true);

        int newSlot = event.getNewSlot();
        int itemAmount = items.size();

        slotSelected += newSlot-4; // change current slot, 4 is the default middle slot
        slotSelected = ((slotSelected % itemAmount) + itemAmount) % itemAmount; // make sure it is in range of itemAmount

        bukkitPlayer.getInventory().setHeldItemSlot(4); // Return to middle slot

        infoDisplay.updateSelection();
    }

    @EventHandler
    public void cancelDrop(PlayerDropItemEvent event) {
        if (!event.getPlayer().equals(bukkitPlayer))
            return;
        event.setCancelled(true);
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
        setEnergy(getMaxEnergy());
        updateInfo();

        itemsUsed.clear();
        stepPlan.clear();

        for (Pair<GameItem, Boolean> pair : items) {
            if (!(pair.getFirst() instanceof Cooldown itemCooldown))
                continue;

            itemCooldown.decrementCooldown();
            if (itemCooldown.getCooldown() == 0)
                pair.setSecond(false);
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
        Iter.logger.info("Used item \"" + item.getName()+"\" at tile [" + itemsUsed.getFirst().getValue().x + ", " + itemsUsed.getFirst().getValue().y+"] by " + bukkitPlayer.getName());

        itemsUsed.removeFirst();
        infoDisplay.updateCardUseHistory();

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
        infoDisplay.updateCardUseHistory();

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

        infoDisplay.updateCardUseHistory();
        updateItemBlocks();
        infoDisplay.updateSelection();
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
        List<Entity> passengers = bukkitPlayer.getPassengers();
        passengers.forEach(bukkitPlayer::removePassenger);
        loc.setDirection(bukkitPlayer.getLocation().getDirection());
        bukkitPlayer.teleport(loc);
        passengers.forEach(bukkitPlayer::addPassenger);
    }

    private void addItemUpdateless(GameItem item) {
        items.add(new Pair<>(item, false));
    }

    // Getters/Setters

    public void addItem(GameItem item) {
        addItemUpdateless(item);
        updateItemBlocks();
        infoDisplay.updateInventoryItemData();
    }

    public Point getPosition() {
        return (Point) pos.clone();
    }

    /**
     * Returns player's position at given step.
     * If player haven't moved yet or if there is no step plan at given step, returns last (current) position.
     * Don't use {@link StepPlan#size()}, it returns the size of step plan, not the last step number.
     * @param step step index
     * @return player's position at given step
     */
    public Point getPositionAtStep(int step) {
        if (step <= 0 || stepPlan.isEmpty()) // if player haven't moved yet or if there is no step plan (players on other team)
            return getPosition();

        ArrayList<Integer> stepList = getStepList(itemsUsed);
        Iter.logger.info("Player " + bukkitPlayer.getName() + " step list: " + stepList);

        if (stepList.getLast() == -1)
            return getPosition();

        // If player haven't used "step" amount of cards, get their last position
        if (stepList.size() <= step)
            return (Point) this.stepPlan.get(stepList.getLast()).clone();

        // Returns player's position at given step
        int stepIndex = stepList.get(step);
        if (stepIndex < 0)
            return getPosition();
        return (Point) this.stepPlan.get(stepList.get(step)).clone();
    }

    private static ArrayList<Integer> getStepList(Deque<Map.Entry<GameItem, Point>> uses) {
        // Each value = previous value + 1 if current GameItem is ItemWalk
        ArrayList<Integer> steps = new ArrayList<>();
        steps.add(-1);
        for (Map.Entry<GameItem, Point> use : uses)
            steps.add(steps.getLast() + (use.getKey() instanceof MovementGameItem ? 1 : 0));

        return steps;
    }

    public int getCurrentStep() {
        if (!game.playersLeftThisTurn.contains(this))
            return 0;
        return itemsUsed.size();
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
        if (bukkitPlayer == null)
            return null;

        if (bukkitPlayer.getLocation().getPitch() <= 0) // cancel if player looks up
            return null;


        RayTraceResult result = bukkitPlayer.rayTraceBlocks(66);
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
            this.isAlive = false;
            infoDisplay.removeFakePlayer();
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
        return !isAlive;
    }

    public boolean isAlive() {
        return isAlive;
    }

    /** Returns a list of items in player's inventory. Each item is a pair of {@link GameItem} and boolean for if item being blocked. */
    public List<Pair<GameItem, Boolean>> getItems() {
        return List.copyOf(items);
    }

    public int getCurrentItemIndex() {
        return slotSelected;
    }

    /** Returns a list of items used in this turn. Each item is a pair of {@link GameItem} and {@link Point} for the tile it was used on. */
    public List<Map.Entry<GameItem, Point>> getItemsUsed() {
        return itemsUsed.stream().toList();
    }

    public FakePlayer getFakePlayer() {
        return fakePlayer;
    }
}
