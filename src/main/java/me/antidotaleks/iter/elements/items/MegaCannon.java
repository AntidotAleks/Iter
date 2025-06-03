package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.Iter;
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
        Iter.logger.info("Using MegaCannon: " + coords + " at step " + step + " at " + coords + " + mirroring " + oppositeFromPlayer(coords));
        // Walkable distance is 1 tile, selection on the enemy, pushes away from the player selected
        return tilesAwayTaxi(getCurrentPosition(), coords) == 1 &&
                TargetSelector.ENEMY.canUseAt(coords, player, step) &&
                super.usable(oppositeFromPlayer(coords), step);
    }

    @Override
    public void use(Point coords) {
        super.use(oppositeFromPlayer(coords));

        GamePlayer playerHit = player.getGame().getPlayer(coords);
        if (playerHit == null) {
            Iter.logger.warning("Player hit is null at " + coords + " for player " + player.getPlayer().getName());
            return;
        }
        playerHit.damage(4+player.getFlatDamage());
    }

    private Point oppositeFromPlayer(Point point) {
        Point playerBack = player.getPosition();
        int dx = playerBack.x - point.x;
        int dy = playerBack.y - point.y;
        playerBack.translate(dx, dy);
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
