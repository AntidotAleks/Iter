package me.antidotaleks.iter.maps;

import me.antidotaleks.iter.Iter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Map {
    Location mapLocation;
    int sizeX, sizeY;
    int[][] map;

    HashMap<String, Object> settings = new HashMap<>();
    public Map(File mapFile) {
        getData(mapFile);


    }

    private void getData(File mapFile) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(mapFile);

        List<Integer> rawPos = yaml.getIntegerList("copyPosStart");
        final Location posStart = new Location(Iter.defaultWorld, rawPos.get(0), rawPos.get(1), rawPos.get(2));
        rawPos = yaml.getIntegerList("copyPosEnd");
        final Location posEnd = new Location(Iter.defaultWorld, rawPos.get(0), rawPos.get(1), rawPos.get(2));
        mapLocation = buildMap(posStart, posEnd);

        List<String> mapData = yaml.getStringList("map");
        mapData.removeFirst();
        setupMapData(mapData);



    }

    /**
     * Initializes and sets up the internal map/walls representation based on the given map data.
     * This method processes the input strings representing the map, removes spaces, calculates
     * the size of the map, and populates a 2D array with wall and floor information.
     *
     * @param mapData List of strings, each string representing a row of the map where:
     *                - 'X' or '#' indicates a wall or an obstacle,
     *                - '.' or other characters (except space) signify floor or open space.
     *                The strings are expected to have consistent lengths and may include spaces
     *                that will be removed during processing.
     */
    private void setupMapData(List<String> mapData) {
        if(mapData == null || mapData.isEmpty()) return;

        mapData.replaceAll(s -> s.replace(" ", ""));

        sizeX = mapData.size();
        sizeY = mapData.getFirst().replace(" ", "").length();

        map = new int[sizeX*2 + 1][sizeY*2 + 1]; // Grid of walls and floors, size is doubled for between-grid objects

        // Replace first and last row with walls
        for (int j = 0; j < sizeX*2+1; j++) {
            map[0][j] = 1;       // First row
            map[sizeX*2][j] = 1; // Last row
        }

        // Replace first and last column (excluding corners, already covered)
        for (int i = 1; i < sizeY*2; i++) {
            map[i][0] = 1;       // First column
            map[i][sizeY*2] = 1; // Last column
        }

        for (int x = 0; x < sizeX; x++)
            for (int y = 0; y < sizeY; y++) {

                // X: odd, Y: odd
                char poi = mapData.get(x).charAt(y);
                map[x*2+1][y*2+1] = (poi == '#' || poi == 'X') ? 1 : 0;

                // X: even, Y: even
                if(x > 0 && y > 0) {
                    String chars = new String(new char[]{
                            mapData.get(x-1).charAt(y-1), // Top left
                            mapData.get(x).charAt(y-1),  // Top right
                            mapData.get(x-1).charAt(y), // Bottom left
                            poi                        // Bottom right
                    });
                    if(chars.chars().anyMatch(c -> c == 'X')) {
                        map[x*2][y*2] = 1;
                    }
                    if(chars.chars().filter(c -> c == '#').count() >= 2) {
                        map[x*2][y*2] = 1;
                    }
                }

                // X: even, Y: odd
                if(x > 0) {
                    String chars = new String(new char[]{
                            mapData.get(x-1).charAt(y), // Left
                            poi                        // Right
                    });
                    if(chars.chars().anyMatch(c -> c == 'X' || c == '#')) {
                        map[x*2][y*2+1] = 1;
                    }
                }

                // X: odd, Y: even
                if(y > 0) {
                    String chars = new String(new char[]{
                            mapData.get(x).charAt(y-1), // Top
                            poi                        // Bottom
                    });
                    if(chars.chars().anyMatch(c -> c == 'X' || c == '#')) {
                        map[x*2+1][y*2] = 1;
                    }
                }

            }
    }


    /**
     * Copies built map from set position to new auto-generated location
     * @return New {@link Location} where map was build
     */
    public Location buildMap(Location copyPosStart, Location copyPosEnd) {
        Location mapLoc = getNewMapLocation();

        // Copy all Block Displays
        BoundingBox box = BoundingBox.of(copyPosStart, copyPosEnd);
        Collection<Entity> bds = Iter.defaultWorld.getNearbyEntities(box, e -> e.getType() == EntityType.BLOCK_DISPLAY);
        bds.forEach(entity -> {
            Location loc = entity.getLocation();
            loc.subtract(copyPosStart);
            loc.add(mapLoc);
            //noinspection UnstableApiUsage
            entity.copy(loc);
        });

        for (int x = 0; x <= (int) box.getWidthX(); x++)
        for (int y = 0; y <= (int) box.getHeight(); y++)
        for (int z = 0; z <= (int) box.getWidthZ(); z++) {
            BlockData block = Iter.defaultWorld.getBlockData(x+(int) box.getMinX(),y+(int) box.getMinY(),z+(int) box.getMinZ());
            if (block.getMaterial() != Material.AIR)
                Iter.defaultWorld.setBlockData(mapLoc.clone().add(x,y,z), block);
        }

        return mapLoc;
    }

    /**
     * Removes map (removes all blocks and displayBlock entities)
     */
    public void removeMap() {
        // Get useful cords
        Location start = mapLocation.clone().add(-1, -1, -1);
        Location end = mapLocation.clone().add(sizeX*3, sizeX+sizeY+9, sizeY*3);


        // Kill all Block Displays
        BoundingBox box = BoundingBox.of(start, end);
        Collection<Entity> bds = Iter.defaultWorld.getNearbyEntities(box, e -> e.getType() == EntityType.BLOCK_DISPLAY);
        bds.forEach(Entity::remove);

        //Removes all blocks
        for (int x = start.getBlockX(); x < end.getBlockX(); x++)
        for (int y = start.getBlockY(); y < end.getBlockY(); y++)
        for (int z = start.getBlockZ(); z < end.getBlockZ(); z++) {
            if (Iter.defaultWorld.getBlockData(x,y,z).getMaterial() != Material.AIR)
                Iter.defaultWorld.setBlockData(x,y,z, air);
        }
    }

    private static final BlockData air = Bukkit.createBlockData(Material.AIR);

    private static int i = 0;
    private static Location getNewMapLocation() {
        //noinspection IntegerDivisionInFloatingPointContext
        Location loc = new Location(Iter.defaultWorld, (i%8+1)<<8, 0, (i/8+1)<<8);
        if(i++>=64) i=0;
        return loc;
    }


    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }
}