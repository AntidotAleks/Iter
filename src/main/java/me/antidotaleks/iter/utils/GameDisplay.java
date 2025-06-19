package me.antidotaleks.iter.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import kotlin.collections.ArrayDeque;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.Game;
import me.antidotaleks.iter.elements.GamePlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class GameDisplay {
    private final Game game;
    private final ArrayDeque<Path> paths = new ArrayDeque<>();

    public GameDisplay(Game game) {
        this.game = game;
    }

    /**
     * Draws the path for the given game player.
     * The path is drawn from the last 2 positions in the stepPlan of the game player.
     *
     * @param gamePlayer the game player to draw the path for
     */
    public Path drawPath(GamePlayer gamePlayer) {
        if (gamePlayer == null) return null;

        int currentStep = gamePlayer.getCurrentStep();
        Iter.logger.info("Drawing path for player " + gamePlayer.bukkitPlayer.getName() + " at step " + currentStep);
        Point start = gamePlayer.getPositionAtStep(currentStep - 1);
        Point end = gamePlayer.getPositionAtStep(currentStep);

        return drawPath(gamePlayer, start, end, currentStep);
    }

    public Path drawPath(@NotNull GamePlayer gamePlayer, @NotNull Point start, @NotNull Point end, int currentStep) {
        // Get path data
        int startRaise = paths.stream().filter(path -> path.gamePlayer.equals(gamePlayer) && path.end.equals(start))
                .map(path -> path.endRaise).findFirst().orElse(0); // Get the raise of the last point on that player's path
        int endRaise = paths.stream().filter(path -> path.start.equals(end))
                .map(path -> path.startRaise + 1).findFirst().orElse(0); // Get the raise above the highest point

        // Create path
        updateForPlayer(gamePlayer); // Placed before path creation to update only previous paths. The newly created path will be drawn after this call for everyone, including this player
        Path path = new Path(gamePlayer, start, end, currentStep, startRaise, endRaise, gamePlayer.team.style.color);
        paths.addFirst(path);
        updateForPlayers(path);

        return path;
    }

    public void removePath(Path path) {
        if (path == null) return;
        path.remove();
        paths.remove(path);

        var gamePlayer = path.gamePlayer;
        updateForPlayer(gamePlayer); // Update the remaining paths for the player

        var team = gamePlayer.team;
        var toUpdate = paths.stream().filter(p -> p.gamePlayer.equals(gamePlayer) && p.step <= Path.CHANGES).toList();
        game.getAllGamePlayers().forEach(pl-> Path.drawAll(pl.getCurrentStep(), pl, toUpdate));
    }


    public void updateForPlayer(GamePlayer gamePlayer) {
        int currentStep = gamePlayer.getCurrentStep();
        int from = currentStep - Path.CHANGES;
        int to = currentStep + Path.CHANGES;
        paths.stream().filter(path -> from <= path.step && path.step <= to)
                .forEach(path -> path.draw(currentStep, gamePlayer));
    }
    public void updateAllForPlayer(GamePlayer gamePlayer) {
        int currentStep = gamePlayer.getCurrentStep();
        paths.stream().filter(path -> path.gamePlayer.equals(gamePlayer))
                .forEach(path -> path.draw(currentStep, gamePlayer));
    }
    private void updateForPlayers(Path path) {
        for (GamePlayer gamePlayer : game.getAllGamePlayers()) {
            path.draw(gamePlayer.getCurrentStep(), gamePlayer);
        }
    }

    public void decrementPaths() {
        paths.forEach(path -> path.step--);
    }

    public static class Path {
        private final GamePlayer gamePlayer;
        private final Point start;
        private final Point end;
        private int step;
        private final int startRaise;
        private final int endRaise;
        private final Color color;

        TextDisplay display;

        public Path(GamePlayer gamePlayer, Point start, Point end, int step, int startRaise, int endRaise, Color color) {
            this.gamePlayer = gamePlayer;
            this.start = start;
            this.end = end;
            this.step = step;
            this.startRaise = startRaise;
            this.endRaise = endRaise;
            this.color = color;
        }

        private static final int CHANGES = 3;
        private static final float CHANGE_DELTA = 0.2f;
        private void draw(int currentStep, GamePlayer gamePlayer) {
            var currentColor = color;
            float alpha = Math.clamp( 1 - Math.abs((float) step - currentStep) * CHANGE_DELTA,
                    1 - CHANGE_DELTA * CHANGES, 1.0f);
            if (currentStep < step)  // Next steps are lighter
                currentColor = new Color(
                        (color.getRed() >> 1) + 128,
                        (color.getGreen() >> 1) + 128,
                        (color.getBlue() >> 1) + 128,
                        (int) (alpha * 255));
            else if (currentStep > step)  // Previous steps just fade out
                currentColor = new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        (int) (alpha * 255));

            if(display == null)
                spawn();

            update(currentColor, gamePlayer);
        }
        private static void drawAll(int currentStep, GamePlayer gamePlayer, List<Path> paths) {
            for (Path path : paths)
                path.draw(currentStep, gamePlayer);
        }

        private void remove() {
            if (display == null) return;

            display.remove();
            display = null;
        }

        private static final float stepHeight = 0.1f; // Height of each overlay step in blocks
        private static final float lineWidth = 0.25f;
        private static final float skewFactor = 3.6365f*stepHeight; // Magic number to skew by 1 block
        private static final Matrix4f baseMatrix = new Matrix4f(
                5.714286f, 0f        ,0f  ,-0.0714286f,
                0f       , 3.636363f ,0f  , 0f        ,
                0f       , 0f        ,0.1f, 0f        ,
                0f       , 0f        ,0f  , 1f        ); // TextDisplay transformation matrix to scale it to 1x1 block

        private void spawn() {
            Iter.logger.info("Raise from " + startRaise + " to " + endRaise + " for player " + gamePlayer.bukkitPlayer.getName() + " at step " + step);

            var game = gamePlayer.game;
            var midpoint = game.toWorldLocation(start).add(game.toWorldLocation(end)).multiply(0.5).add(0, 0.005+startRaise*stepHeight, 0);
            var length = (float) Math.sqrt(Math.pow(start.x - end.x, 2) + Math.pow(start.y - end.y, 2));
            var scale = 3*length - lineWidth;

            display = Iter.overworld.spawn(midpoint, TextDisplay.class, textDisplay -> {
                textDisplay.setText(" ");
                Matrix4f matrix = new Matrix4f(baseMatrix); // Clone

                matrix.scale(lineWidth, scale, 1); // Make it a line
                matrix.m13(matrix.m13() - scale/2); // Translate Y to center the text display to midpoint
                matrix.m21(skewFactor*(endRaise-startRaise)); // Raise the further end up by skewing
                matrix.rotateX((float) Math.toRadians(90)); // Rotate to face up
                matrix.rotateY((float) Math.atan2(end.x - start.x, -end.y + start.y)); // Rotate to face the path direction

                textDisplay.setTransformationMatrix(matrix.transpose());
                textDisplay.setBrightness(new Display.Brightness(15, 15));
            });
        }

        private void update(Color color, GamePlayer gamePlayer) {
            // display.setBackgroundColor(org.bukkit.Color.fromARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue()));
            // Change color for specific player only

            ProtocolManager pm = Iter.protocolManager;
            PacketContainer colorPacket = Iter.protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);

            WrappedDataWatcher.Serializer intSerializer = WrappedDataWatcher.Registry.get(Integer.class);
            int integerColor =
                    color.getAlpha() << 24 |
                    color.getRed()   << 16 |
                    color.getGreen() <<  8 |
                    color.getBlue();

            var watcher = new WrappedDataWatcher();
            watcher.setObject(25, intSerializer, integerColor);

            final List<WrappedDataValue> wrappedDataValueList = watcher.getWatchableObjects().stream().filter(Objects::nonNull).map(entry -> {
                final var object = entry.getWatcherObject();
                return new WrappedDataValue(object.getIndex(), object.getSerializer(), entry.getRawValue());
            }).toList();

            colorPacket.getEntityModifier(display.getWorld()).write(0, display);
            colorPacket.getDataValueCollectionModifier().write(0, wrappedDataValueList);

            try {
                pm.sendServerPacket(gamePlayer.bukkitPlayer, colorPacket);
            } catch (Exception e) {
                Iter.logger.warning("Failed to send color packet for path display to player " + gamePlayer.bukkitPlayer.getName());
                e.printStackTrace();
            }
        }
    }
}
