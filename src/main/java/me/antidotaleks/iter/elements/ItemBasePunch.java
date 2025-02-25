package me.antidotaleks.iter.elements;

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
        return TargetSelector.ENEMY.isAcceptable(coords, player);
    }

    @Override
    public void use(Point coords) {
        player.getGame().getPlayer(coords).damage(1+player.getFlatDamage());

    }
}
