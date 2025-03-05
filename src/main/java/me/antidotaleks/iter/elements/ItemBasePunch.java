package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.Iter;

import java.awt.Point;

public class ItemBasePunch extends GameItem {


    public ItemBasePunch(GamePlayer player) {
        super(player);
    }

    @Override
    public int getEnergyUsage() {
        return 1;
    }

    @Override
    public boolean usable(Point coords) {
        if (tilesAway(getCurrentPosition(), coords) != 1)
            return false;
        return TargetSelector.ENEMY.isAcceptable(coords, player);
    }

    @Override
    public void use(Point coords) {
        player.getGame().getPlayer(coords).damage(1+player.getFlatDamage());
        Iter.logger.info(player.getPlayer().getName() + " punched " + player.getGame().getPlayer(coords).getPlayer().getName());
    }
}
