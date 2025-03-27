package me.antidotaleks.iter.utils.items.specific;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.GameItem;
import me.antidotaleks.iter.utils.items.PreUsed;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class MovementGameItem extends GameItem implements PreUsed {
    /**
     * Create a new game item
     *
     * @param player the player that owns the item
     */
    public MovementGameItem(GamePlayer player) {
        super(player);
    }


    /**
     * Check if the player can use the item at given coordinates and if the player is not overlapping with other players at the same step.
     * Does not check the range of the item. Override this method to implement the item's additional checks with {@code super.usable(coords)}.
     * @param coords the coordinates to check
     * @return {@code true} if the player can use the item at the given coordinates, otherwise {@code false}
     */
    @Override
    public boolean usable(Point coords) {
        // Check if the player is not overlapping with players from other teams
        if (!isWalkable(coords))
            return false;

        // Check if the player is not overlapping with players in the same team at the same step at given coordinates
        int step = player.getItemsUsed().size();

        for (GamePlayer teammate : player.getTeam()) {
            if (teammate == player)
                continue;

            List<Map.Entry<GameItem, Point>> teammateItemsUsed = teammate.getItemsUsed();
            ArrayList<Integer> steps = getStepList(teammateItemsUsed);
            int teammateStep = steps.getLast();

            if (teammateStep >= step)
                teammateStep = steps.get(step);

            if (teammate.getStepPlanning().get(teammateStep).equals(coords))
                return false;
        }

        // If no player is overlapping, return true
        return true;
    }

    protected boolean isWalkable(Point coords) {
        GamePlayer other = player.getGame().getPlayer(coords);
        return other == null || Arrays.stream(player.getTeam()).anyMatch(p -> p == other);
    }

    protected ArrayList<Integer> getStepList(List<Map.Entry<GameItem, Point>> uses) {
        // Each value = previous value + 1 if current GameItem is ItemWalk
        ArrayList<Integer> steps = new ArrayList<>();
        steps.add(0);
        for (Map.Entry<GameItem, Point> use : uses)
            steps.add(steps.getLast() + (use.getKey() instanceof MovementGameItem ? 1 : 0));

        return steps;
    }


    @Override
    public void preUse(Point coords) {
        player.nextStep.add(coords);
    }

    public void undoPreUse() {
        player.nextStep.removeLast();
    }

    @Override
    public void use(Point coords) {
        player.setPosition(player.nextStep.getFirst());
        player.nextStep.removeFirst();
    }
}
