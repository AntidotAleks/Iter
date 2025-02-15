package me.antidotaleks.iter.maps;

import com.google.common.base.Preconditions;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Map {
    private final String mapName;
    private final Location posStart, posEnd;
    private final int sizeX, sizeY;
    private int[][] map;
    private final int[] playersInTeams;

    public Map(File mapFile) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(mapFile);

        List<Integer> rawPos = yaml.getIntegerList("copyPosStart");
        posStart = new Location(Iter.defaultWorld, rawPos.get(0), rawPos.get(1), rawPos.get(2));
        rawPos = yaml.getIntegerList("copyPosEnd");
        posEnd = new Location(Iter.defaultWorld, rawPos.get(0), rawPos.get(1), rawPos.get(2));

        mapName = mapFile.getName().replace(".yaml", "");
        String mapDataStr = yaml.getString("map").replace(" ", "");
        String[] mapData = mapDataStr.split("\n");
        Preconditions.checkArgument(!mapDataStr.isEmpty(), "Map data is empty");
        Preconditions.checkArgument(mapDataStr.chars().filter(Character::isDigit).distinct().count() >= 2, "Not enough teams in map");

        sizeX = mapData.length;
        sizeY = mapData[0].length();

        // Count amount of spawn points for each team, up to 10 teams
        int[] count = new int[10];
        mapDataStr.chars()
                .filter(Character::isDigit)
                .forEach(c -> count[c - '0']++);
        // Remove all 0s
        playersInTeams = Arrays.stream(count).filter(i -> i != 0).toArray();


        setupMapData(mapData);


    }

    /**
     * Initializes and sets up the internal map/walls representation based on the given map data.
     * This method processes the input strings representing the map, removes spaces, calculates
     * the size of the map, and populates a 2D array with wall and floor information.
     *
     * @param mapData List of strings, each string representing a row of the map where:
     *                - 'X' or '#' indicates a wall or an obstacle, where X blocks vision around corners.,
     *                - '.' or other characters (except space) signify floor or open space.
     *                The strings are expected to have consistent lengths and may include spaces
     *                that will be removed during processing.
     */
    private void setupMapData(String[] mapData) {
        if(mapData == null || mapData.length == 0) return;

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
                char poi = mapData[x].charAt(y);
                map[x*2+1][y*2+1] = (poi == '#' || poi == 'X') ? 1 : 0;

                // X: even, Y: even
                if(x > 0 && y > 0) {
                    String chars = new String(new char[]{
                            mapData[x-1].charAt(y-1), // Top left
                            mapData[x].charAt(y-1),  // Top right
                            mapData[x-1].charAt(y), // Bottom left
                            poi                    // Bottom right
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
                            mapData[x-1].charAt(y), // Left
                            poi                    // Right
                    });
                    if(chars.chars().anyMatch(c -> c == 'X' || c == '#')) {
                        map[x*2][y*2+1] = 1;
                    }
                }

                // X: odd, Y: even
                if(y > 0) {
                    String chars = new String(new char[]{
                            mapData[x].charAt(y-1), // Top
                            poi                    // Bottom
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
    public Location buildMap() {
        Location mapLoc = getNewMapLocation();

        // Copy all Block Displays
        BoundingBox box = BoundingBox.of(posStart, posEnd);
        Collection<Entity> bds = Iter.defaultWorld.getNearbyEntities(box, e -> e.getType() == EntityType.BLOCK_DISPLAY);
        bds.forEach(entity -> {
            Location loc = entity.getLocation();
            loc.subtract(posStart);
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
    public void removeMap(Location mapLocation) {
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

    public String displayName() {
        return mapName;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int[][] getMap() {
        return map;
    }

    public int getTeamsAmount() {
        return playersInTeams.length;
    }
    public int[] getPlayersAmountInTeams() {
        return playersInTeams;
    }
}