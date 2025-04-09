package me.antidotaleks.iter.elements.items;

import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.utils.items.GameItem;
import me.antidotaleks.iter.elements.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

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
    public boolean usable(@NotNull Point coords, int step) {
        if (tilesAwayTaxi(getCurrentPosition(), coords) != 1)
            return false;
        return TargetSelector.ENEMY.canUseAt(coords, player, step);
    }

    @Override
    public void use(Point coords) {
        GamePlayer playerHit = player.getGame().getPlayer(coords);
        playerHit.damage(1+player.getFlatDamage());

        particles(player.getPosition(), coords);
    }

    @Override
    public String getName() {
        return "Base Punch";
    }

    @Override
    public char getCardSymbol() {
        return '\uE002';
    }

    private void particles(Point from, Point to) {
        Location fromLoc = player.getGame().toWorldLocation(from).add(0, 1, 0);
        Location toLoc = player.getGame().toWorldLocation(to).add(0, 1, 0);
        Vector direction = toLoc.clone().subtract(fromLoc).toVector().multiply(0.3);
        Iter.overworld.spawnParticle(Particle.SWEEP_ATTACK, fromLoc.add(direction), 1, 0,0,0, 0, null, true);
        Iter.overworld.spawnParticle(Particle.CRIT, toLoc, 10, 0,0,0, 0.5, null, true);
    }
}
