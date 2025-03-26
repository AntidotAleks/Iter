package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GameItem;
import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.elements.PreUsed;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ItemWalk extends GameItem implements PreUsed {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public ItemWalk(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 1;
    }

    @Override
    public boolean usable(Point coords) {
        // Walkable distance is 1 tile
        if(tilesAway(getCurrentPosition(), coords) != 1)
            return false;

        // Check if the player is not overlapping with players from other teams
        if (!isWalkable(coords))
            return false;

        // Check if the player is not overlapping with players in the same team at the same step at given coordinates
        int step = player.getItemsUsed().size();

        for (GamePlayer teammate : player.getTeam()) {
            if (teammate == player)
                continue;

            List<Map.Entry<GameItem, Point>> used = teammate.getItemsUsed();
            ArrayList<Integer> steps = getStepList(used);
            int teammateStep = steps.getLast();

            if (teammateStep >= step)
                teammateStep = steps.get(step);

            if (teammate.getStepPlanning().get(teammateStep).equals(coords))
                return false;
        }

        // If no player is overlapping, return true
        return true;
    }

    private boolean isWalkable(Point coords) {
        GamePlayer other = player.getGame().getPlayer(coords);
        return other == null || Arrays.stream(player.getTeam()).anyMatch(p -> p == other);
    }

    private ArrayList<Integer> getStepList(List<Map.Entry<GameItem, Point>> uses) {
        // Each value = previous value + 1 if current GameItem is ItemWalk
        ArrayList<Integer> steps = new ArrayList<>();
        steps.add(0);
        for (Map.Entry<GameItem, Point> use : uses)
            steps.add(steps.getLast() + (use.getKey() instanceof ItemWalk ? 1 : 0));

        return steps;
    }

    private final ArrayList<Point> nextStep = new ArrayList<>();

    @Override
    public void preUse(Point coords) {
        nextStep.add(coords);
    }

    public void undoPreUse() {
        nextStep.removeLast();
    }

    public List<Point> getStepPlanning() {
        return Collections.unmodifiableList(nextStep);
    }

    @Override
    public void use(Point coords) {
        player.setPosition(nextStep.getFirst());
        nextStep.removeFirst();
    }

    @Override
    public String getName() {
        return "Walk";
    }

    @Override
    public char getCardSymbol() {
        return '\uE001';
    }
}
