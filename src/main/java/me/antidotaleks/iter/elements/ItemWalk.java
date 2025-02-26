package me.antidotaleks.iter.elements;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        if(player.getPosition().distance(coords) > 1.1 || player.getPosition().equals(coords))
            return false;

        // Can't walk on non-empty tiles
        if (!TargetSelector.isEmptyGround(coords, player))
            return false;

        // Get current step to not overlap with other players
        int step = player.getItemsUsed().size();

        // Check if the player is not overlapping with other players at the same step at given coordinates
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
        player.getPosition().setLocation(nextStep.getFirst());
        nextStep.removeFirst();
    }
}
