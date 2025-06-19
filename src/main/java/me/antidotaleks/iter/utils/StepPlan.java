package me.antidotaleks.iter.utils;

import kotlin.collections.ArrayDeque;
import me.antidotaleks.iter.elements.GamePlayer;

import java.awt.*;
import java.util.List;

public class StepPlan {
    public final GamePlayer gamePlayer;
    private final GameDisplay gameDisplay;
    private final List<Step> steps = new ArrayDeque<>();

    public StepPlan(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
        this.gameDisplay = gamePlayer.game.gameDisplay;
    }

    public void add(Point point) {
        Step step = new Step(point, null);
        steps.add(step);
        step.path = gameDisplay.drawPath(gamePlayer);
    }

    public Point get(int index) {
        return steps.get(index).point;
    }
    public Point getFirst() {
        if (steps.isEmpty()) return null;
        return steps.getFirst().point;
    }
    public Point getLast() {
        if (steps.isEmpty()) return null;
        return steps.getLast().point;
    }

    public void remove(int index) {
        if (index < 0 || index >= steps.size()) return;
        Step step = steps.remove(index);
        gameDisplay.removePath(step.path);
    }
    public void removeFirst() {
        if (steps.isEmpty()) return;
        Step firstStep = steps.removeFirst();
        gameDisplay.removePath(firstStep.path);
    }
    public void removeLast() {
        if (steps.isEmpty()) return;
        Step lastStep = steps.removeLast();
        gameDisplay.removePath(lastStep.path);
    }

    public int size() {
        return steps.size();
    }
    public boolean isEmpty() {
        return steps.isEmpty();
    }
    public void clear() {
        for (Step step : steps) {
            gameDisplay.removePath(step.path);
        }
        steps.clear();
    }

    private static class Step {
        private final Point point;
        private GameDisplay.Path path;
        public Step(Point point, GameDisplay.Path path) {
            this.point = point;
            this.path = path;
        }
    }
}
