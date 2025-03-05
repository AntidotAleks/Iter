package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.elements.GameItem;
import me.antidotaleks.iter.elements.GamePlayer;

import java.awt.*;

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
        GamePlayer playerHit = player.getGame().getPlayer(coords);

        playerHit.damage(1+player.getFlatDamage());
        playerHit.updateInfo();
    }
}
