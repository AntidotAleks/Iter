package me.antidotaleks.iter.maps;

import com.google.common.base.Preconditions;
import me.antidotaleks.iter.Iter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class Map {
    private final String[] mapData;
    private final String mapName;
    private final Location posStart, posEnd;
    private final int sizeX, sizeY;
    private boolean[][] map;
    private final int[] playersInTeams;
    private final ArrayList<Point>[] spawnPoints;
    private final ConfigurationSection[] modifiers;

    public Map(File mapFile) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(mapFile);

        List<Integer> rawPos = yaml.getIntegerList("copyPosStart");
        posStart = new Location(Iter.overworld, rawPos.get(0), rawPos.get(1), rawPos.get(2));
        rawPos = yaml.getIntegerList("copyPosEnd");
        posEnd = new Location(Iter.overworld, rawPos.get(0), rawPos.get(1), rawPos.get(2));

        mapName = mapFile.getName().replace(".yaml", "");
        String mapDataStr = yaml.getString("map").replace(" ", "");
        mapData = mapDataStr.split("\n");
        int[] uniqueTeams = mapDataStr.chars().filter(Character::isDigit).distinct().toArray();

        Preconditions.checkArgument(!mapDataStr.isEmpty(), "Map data is empty");
        Preconditions.checkArgument(uniqueTeams.length >= 2, "Not enough teams in map");
        Preconditions.checkArgument(mapData.length > 0, "Map data is empty");
        Preconditions.checkArgument(Arrays.stream(mapData).allMatch(s -> s.length() == mapData[0].length()), "Map data lines are not equal in length");

        sizeX = mapData[0].length();
        sizeY = mapData.length;

        // Count amount of spawn points for each team, up to 10 teams and their spawn points
        //noinspection unchecked
        spawnPoints = new ArrayList[uniqueTeams.length];
        playersInTeams = new int[uniqueTeams.length];

        // Maps team number to index in the arrays
        HashMap<Integer, Integer> teamToIndex = new HashMap<>();
        for (int i = 0; i < uniqueTeams.length; i++) {
            teamToIndex.put(uniqueTeams[i], i);
            spawnPoints[i] = new ArrayList<>();
        }

        // Saves spawn points and counts the amount of spawn points for each team (as max players in team)
        for (int x = 0; x < sizeX; x++)
        for (int y = 0; y < sizeY; y++) {
            char c = getChar(x, y);
            if (!Character.isDigit(c))
                continue;

            spawnPoints[teamToIndex.get((int)c)].add(new Point(x, y));
            playersInTeams[teamToIndex.get((int)c)]++;
        }

        setupMapData();

        // Load modifiers
        modifiers = new YamlConfiguration[uniqueTeams.length];
        for (int i = 0; i < uniqueTeams.length; i++) {
            modifiers[i] = yaml.getConfigurationSection("modifiers."+uniqueTeams[i]);
        }
    }

    /**
     * Initializes and sets up the internal map/walls representation based on the given map data.
     * This method processes the input strings representing the map, removes spaces, calculates
     * the size of the map, and populates a 2D array with wall and floor information. <br>
     * Each string represents a row of the map where:
     * - 'X' or '#' indicates a wall or an obstacle, where X blocks vision around corners.,
     * - '.' or other characters (except space) signify floor or open space.
     * The strings are expected to have consistent lengths and may include spaces
     * that will be removed during processing.
     */
    private void setupMapData() {
        if(mapData == null || mapData.length == 0) return;

        map = new boolean[sizeX*2 + 1][sizeY*2 + 1]; // Grid of walls and floors, size is doubled for between-grid objects

        // Replace first and last row with walls
        for (int x = 0; x < sizeX*2+1; x++) {
            map[x][0] = true;       // First row
            map[x][sizeY*2] = true; // Last roww
        }

        // Replace first and last column (excluding corners, already covered)
        for (int y = 1; y < sizeY*2; y++) {
            map[0][y] = true;       // First column
            map[sizeX*2][y] = true; // Last column
        }

        for (int x = 0; x < sizeX; x++)
            for (int y = 0; y < sizeY; y++) {

                // X: odd, Y: odd
                char poi = getChar(x, y);
                map[x*2+1][y*2+1] = (poi == '#' || poi == 'X');

                // X: even, Y: even
                if(x > 0 && y > 0) {
                    String chars = new String(new char[]{
                            getChar(x-1, y-1), // Top left
                            getChar(x, y-1),     // Top right
                            getChar(x-1, y),    // Bottom left
                            poi                   // Bottom right
                    });
                    if(chars.chars().anyMatch(c -> c == 'X')) {
                        map[x*2][y*2] = true;
                    }
                    if(chars.chars().filter(c -> c == '#').count() >= 2) {
                        map[x*2][y*2] = true;
                    }
                }

                // X: even, Y: odd
                if(x > 0) {
                    String chars = new String(new char[]{
                            getChar(x-1, y), // Left
                            poi                // Right
                    });
                    if(chars.chars().anyMatch(c -> c == 'X' || c == '#')) {
                        map[x*2][y*2+1] = true;
                    }
                }

                // X: odd, Y: even
                if(y > 0) {
                    String chars = new String(new char[]{
                            getChar(x, y-1), // Top
                            poi                // Bottom
                    });
                    if(chars.chars().anyMatch(c -> c == 'X' || c == '#')) {
                        map[x*2+1][y*2] = true;
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
        Collection<Entity> bds = Iter.overworld.getNearbyEntities(box, e -> e.getType() == EntityType.BLOCK_DISPLAY);
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
            BlockData block = Iter.overworld.getBlockData(x+(int) box.getMinX(),y+(int) box.getMinY(),z+(int) box.getMinZ());
            if (block.getMaterial() != Material.AIR)
                Iter.overworld.setBlockData(mapLoc.clone().add(x,y,z), block);
        }

        return mapLoc;
    }

    /**
     * Removes map (removes all blocks and displayBlock entities)
     */
    public void removeMap(Location mapLocation) {
        // Get useful cords
        Location start = mapLocation.clone();
        Location end = mapLocation.clone().subtract(posStart).add(posEnd);


        Iter.logger.info("[Iter] Removing map at "+mapLocation);

        // Kill all Block Displays
        Iter.logger.info("       - Removing BlockDisplays");
        BoundingBox box = BoundingBox.of(start, end);
        Collection<Entity> bds = Iter.overworld.getNearbyEntities(box, e -> e.getType() == EntityType.BLOCK_DISPLAY);
        bds.forEach(Entity::remove);

        //Removes all blocks
        Iter.logger.info("       - Removing blocks");
        for (int x = start.getBlockX(); x <= end.getBlockX(); x++)
        for (int y = start.getBlockY(); y <= end.getBlockY(); y++)
        for (int z = start.getBlockZ(); z <= end.getBlockZ(); z++) {
            if (Iter.overworld.getBlockData(x,y,z).getMaterial() != Material.AIR)
                Iter.overworld.setBlockData(x,y,z, air);
        }
    }

    private char getChar(int x, int y) {
        return mapData[y].charAt(x);
    }

    private static final BlockData air = Bukkit.createBlockData(Material.AIR);

    private static int i = 0;
    private static Location getNewMapLocation() {
        //noinspection IntegerDivisionInFloatingPointContext
        Location loc = new Location(Iter.overworld, (i%8+1)<<8, 0, (i/8+1)<<8);
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

    public boolean[][] getMap() {
        return map;
    }

    public int getTeamsAmount() {
        return playersInTeams.length;
    }
    public int[] getPlayersAmountInTeams() {
        return playersInTeams;
    }

    public ArrayList<Point> getSpawnPoints(int team) {
        return spawnPoints[team];
    }

    public ConfigurationSection getModifiers(int teamIndex) {
        return modifiers[teamIndex];
    }
}