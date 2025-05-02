package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Conditional;
import me.antidotaleks.iter.utils.items.GameItem;
import me.antidotaleks.iter.utils.items.specific.MovementCooldownGameItem;
import me.antidotaleks.iter.utils.items.specific.MovementGameItem;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class MegaCannon extends MovementCooldownGameItem implements Conditional {
    /**
     * Item that allows the player to walk
     * @param player the player that owns the item
     */
    public MegaCannon(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 2;
    }

    @Override
    public int getMaxCooldown() {
        return 3;
    }

    @Override
    public boolean usable(@NotNull Point coords, int step) {
        // Walkable distance is 1 tile, selection on the enemy, pushes away from the player selected
        return tilesAwayTaxi(getCurrentPosition(), coords) == 1 &&
                TargetSelector.ENEMY.canUseAt(coords, player, step) &&
                super.usable(oppositeFromPlayer(coords), step);
    }

    @Override
    public void use(Point coords) {
        GamePlayer playerHit = player.getGame().getPlayer(coords);
        playerHit.damage(4+player.getFlatDamage());

        super.use(oppositeFromPlayer(coords));
    }

    private Point oppositeFromPlayer(Point point) {
        Point playerBack = player.getPosition();
        playerBack.translate(playerBack.x-point.x, playerBack.y-point.y);
        return playerBack;
    }

    @Override
    public String getName() {
        return "Mega Cannon";
    }

    @Override
    public char getCardSymbol() {
        return '\uE005';
    }

    @Override
    public boolean isBlocked() {
        return player.getItemsUsed().stream().anyMatch(pair -> pair.getKey() instanceof MovementGameItem);
    }

    @Override
    public boolean isBlocking(GameItem item) {
        return item instanceof MovementGameItem;
    }
}
