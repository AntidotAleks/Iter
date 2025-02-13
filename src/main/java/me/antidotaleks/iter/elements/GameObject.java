package me.antidotaleks.iter.elements;

import me.antidotaleks.iter.maps.Map;

public abstract class GameObject {

    private final Map gameMap;
    private int x, y;

    /**
     * Constructs a GameObject with the specified map and position coordinates.
     *
     * @param gameMap the {@link Map} this object belongs to
     * @param x       the x-coordinate of the object
     * @param y       the y-coordinate of the object
     */
    public GameObject(Map gameMap, int x, int y) {
        this.gameMap = gameMap;
        this.x = x;
        this.y = y;
    }


    /**
     * Sets the x-coordinate of the object.
     * <p>
     * This method is private and validates the provided x-coordinate, ensuring it falls within the allowed bounds
     * of the map's dimensions.
     *
     * @param x the new x-coordinate
     *
     * @throws IllegalArgumentException if the x-coordinate is out of bounds
     */
    public void setX(int x) {
        if (x < 0) throw new IllegalArgumentException("X out of bounds: " + x + " (min: 0)");
        if (x > gameMap.getSizeX())
            throw new IllegalArgumentException("X out of bounds: " + x + " (max: " + gameMap.getSizeX() + ")");

        this.x = x;
    }

    /**
     * Sets the y-coordinate of the object.
     * <p>
     * This method is private and validates the provided y-coordinate, ensuring it falls within the allowed bounds
     * of the map's dimensions.
     *
     * @param y the new y-coordinate
     *
     * @throws IllegalArgumentException if the y-coordinate is out of bounds
     */
    public void setY(int y) {
        if (y < 0) throw new IllegalArgumentException("Y out of bounds: " + y + " (min: 0)");
        if (y > gameMap.getSizeY())
            throw new IllegalArgumentException("Y out of bounds: " + y + " (max: " + gameMap.getSizeY() + ")");

        this.y = y;
    }


    /**
     * @return the x-coordinate of the object
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y-coordinate of the object
     */
    public int getY() {
        return y;
    }
}
