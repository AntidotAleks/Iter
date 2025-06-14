package me.antidotaleks.iter;

import me.antidotaleks.iter.commands.QueueCommand;
import me.antidotaleks.iter.commands.TeamCommand;
import me.antidotaleks.iter.commands.TestCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class SetupManager {

    // Event Listeners

    private static final ArrayList<Listener> listeners = new ArrayList<>(){{
        add(new GameManager());
        add(new Teaming());
    }};

    public static void registerListeners() {
        for (Listener listener : listeners)
        Bukkit.getPluginManager().registerEvents(listener, Iter.plugin);
    }

    // Commands

    private static final HashMap<String, TabExecutor> commands = new HashMap<>(){{
        put("iqueue", new QueueCommand());
        put("iteam", new TeamCommand());
        put("itest", new TestCommand());
    }};

    public static void registerCommands() {
        for (Map.Entry<String, TabExecutor> pair : commands.entrySet()) {
            Iter.logger.info("       - "+pair.getKey());
            PluginCommand command = Iter.plugin.getCommand(pair.getKey());

            assert command != null;
            command.setExecutor(pair.getValue());
            command.setTabCompleter(pair.getValue());
        }
    }

    // Maps

    public static File mapsFolder;
    public static void loadFolders() {
        Iter.pluginFolder.mkdirs();
        String path = Iter.pluginFolder.getPath();
        char separator = File.separatorChar;

        // Open maps folder
        mapsFolder = new File(path+separator+"maps"); // Open maps folder
        mapsFolder.mkdirs();

        // Read maps data files
        File[] mapFiles = mapsFolder.listFiles((dir, name) -> name.endsWith(".yaml"));
        assert mapFiles != null;
        for (File mapFile : mapFiles) {
            String mapName = mapFile.getName().replace(".yaml", "");
            try {
                Iter.logger.info("       - "+mapName);
                me.antidotaleks.iter.maps.Map current = new me.antidotaleks.iter.maps.Map(mapFile);
                GameManager.MAPS.add(current);
                GameManager.MAP_NAMES.add(mapName);
            } catch (Exception e) {
                Iter.logger.warning("Failed to load map "+mapName+": ");
                e.printStackTrace();
            }
        }
    }

    // Languages

    public static File langsFolder;
    public static void loadLanguages() {
        Iter.pluginFolder.mkdirs();
        String path = Iter.pluginFolder.getPath();
        char separator = File.separatorChar;


        // Open langs folder
        mapsFolder = new File(path+separator+"langs"); // Open maps folder
        mapsFolder.mkdirs();

        File[] mapFiles = mapsFolder.listFiles((dir, name) -> name.endsWith(".yaml"));
        assert mapFiles != null;
        for (File langFile : mapFiles) {

            String mapName = langFile.getName().replace(".yaml", "");
        }
    }

    // Teams

    public static Team hideNameTeam;
    public static void setupMinecraftTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        hideNameTeam = scoreboard.getTeam("hide_name");
        if (hideNameTeam == null)
            hideNameTeam = scoreboard.registerNewTeam("hide_name");

        hideNameTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        hideNameTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        hideNameTeam.setCanSeeFriendlyInvisibles(true);
    }
}
