package me.antidotaleks.iter.utils.items.specific;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.GameItem;
import me.antidotaleks.iter.utils.items.PreUsed;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

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
    public boolean usable(@NotNull Point coords, int step) {
        return TargetSelector.EMPTY_GROUND.canUseAt(coords, player, step);
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
