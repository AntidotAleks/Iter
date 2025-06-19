package me.antidotaleks.iter;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class Iter extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register custom events

        plugin = this;
        logger = getLogger();
        overworld = Bukkit.getWorlds().getFirst();
        pluginFolder = getDataFolder();
        protocolManager = ProtocolLibrary.getProtocolManager();

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(plugin);
        } catch (NoPacketAdapterAvailableException e) {
            scoreboardLibrary = new NoopScoreboardLibrary();
            logger.warning("No scoreboard packet adapter available!");
        }
        audiences = BukkitAudiences.create(plugin);

        Iter.logger.info("[Iter] Registering event listeners");
        SetupManager.registerListeners();

        Iter.logger.info("[Iter] Registering commands");
        SetupManager.registerCommands();

        Iter.logger.info("[Iter] Loading maps");
        SetupManager.loadFolders(); // Load maps data

        Iter.logger.info("[Iter] Starting team service");
        Teaming.onEnable(); // Remove invites older than 30 seconds and return players to the team lists
        SetupManager.setupMinecraftTeams();

    }

    @Override
    public void onDisable() {
        while (!GameManager.games.isEmpty())
            GameManager.stopGame(GameManager.games.getLast());

        tryIgnored(scoreboardLibrary::close);
    }



    public static Iter plugin;
    public static Logger logger;
    public static World overworld;
    public static File pluginFolder;
    public static ProtocolManager protocolManager;
    public static ScoreboardLibrary scoreboardLibrary;
    public static BukkitAudiences audiences;

    // Utils

    public static void tryIgnored(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {}
    }

    public static void tryCatch(Runnable runnable) {
        tryCatch(runnable, (String) null);
    }

    public static void tryCatch(Runnable runnable, String errorMessage) {
        try {
            runnable.run();
        } catch (Exception e) {

            if (errorMessage != null)
                logger.warning(errorMessage+": " + e.getMessage());
            else
                logger.warning("An error occurred: " + e.getMessage());

            for (StackTraceElement element : e.getStackTrace())
                logger.warning("\t" + element);
        }
    }

    public static void tryCatch(Runnable runnable, Consumer<Throwable> onException) {
        try {
            runnable.run();
        } catch (Throwable e) {
            if (onException != null)
                onException.accept(e);
            else
                logger.warning("An error occurred: " + e.getMessage());
        }
    }

    public static <T> T tryCatchReturn(Callable<T> callable) {
        return tryCatchReturn(callable, null);
    }

    public static <T> T tryCatchReturn(Callable<T> callable, T defaultValue) {
        try {
            return callable.call();
        } catch (Exception e) {
            logger.warning("An error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace())
                logger.warning("\t" + element);
            return defaultValue;
        }
    }


    // Utils vars

    public static final String
            HEALTH_COLOR_HEX = "#ff5252",
            ENERGY_COLOR_HEX = "#5297ff";

    public static final float HEALTH_BAR_FRACTION = 96/182f;

    public static final BlockData AIR_DATA = Bukkit.createBlockData(Material.AIR);
    public static final ItemStack CURSOR_IS = new ItemStack(Material.GLASS_PANE, 1);

    public static final String CARD_BLOCK = "\uEFFE";
    public static final String CARD_BACKSIDE = "\uEFFF";
    public static final String[] CARD_HISTORY_STATES = new String[]{"\uEFE0", "\uEFE1", "\uEFE2"};
    public static final String[] SIDEBAR_CARD_OFFSETS = new String[]{"\uDB00\uDC30", "\uDAFF\uDF89"};
    public static final String
            COOLDOWN_CARD_ICON = "\uEFF0",
            CONDITIONAL_CARD_ICON = "\uEFF1",
            ENERGY_USE_CARD_ICON = "\uEFF2";

    public static final Style[]
            CARD_FONT = new Style[]{
            Style.empty().font(Key.key("cards")),
            Style.empty().font(Key.key("cards_low"))
            },
            MONO_OFFSET_FONTS = new Style[]{
                    Style.empty().font(Key.key("mono")),
                    Style.empty().font(Key.key("mono_low1")),
                    Style.empty().font(Key.key("mono_low2")),
                    Style.empty().font(Key.key("mono_low3")),
                    Style.empty().font(Key.key("mono_low4")),
                    Style.empty().font(Key.key("mono_low5"))
            };

    public static TranslatableComponent offset(int offset) {
        if (offset < -8192 || offset > 8192) throw new IllegalArgumentException("Offset out of bounds, must be in [-8192, 8192]");
        return Component.translatable("space."+offset).style(Style.style().font(Key.key("default")));
    }

    public static String DEFAULT_SKIN = "ewogICJ0aW1lc3RhbXAiIDogMTY1MDgzMDI5NzE5MCwKICAicHJvZmlsZUlkIiA6ICIxZDJkNWRkZjk2NDc0M2QwYjExYWExZjkxMDA2N2U2MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJzbG93diIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83ZmEyOGNkZjhjMWQ1NzE3NTFiM2FhZWNkM2IwYmU2MGMzMTkyNmQ1YWY4NjVmMjc5YzNlZmU1MWI5Nzc2N2ExIgogICAgfQogIH0KfQ==";
    public static String DEFAULT_SIGNATURE = "TuvAw9N7T+rASaVzjTRhK/iSAxV5chXuO2QeGjp3792qVzQQvido71fHGEDNWynIuh/WEmBrhqsojoD+uitoldvDGIkMYdswlHO7daRzb+vyE6EEIV70OKKAYW6K7PamAQgW0oQOIlhOdH4qD/Uuf64hy3gupi/KW8HBdBU3qdCNJzcEoFw78REZEwumAvke6ToYEhjpyNTgsiDLZbNgM9Zwax2XPOp7KptU0CLLaP/Q0KhNsJbd/jI8CKZznpiaYSgY6K86/V6dTz55wjXe89+TDwJ54wtT/ap6OdMIZS9SptYC/f98P71dXnqnSDMsgz5AlZNgveSSSLKPxIFUO+bHUaNDz/ruSIiQ1LVesNkysFQPlxAND84D/DEAMUxfjjxODcVnRlxSUvHX6Fjx4gQT//6saWr0EArkkNQvwY3ni2KOuEElHZdZ8t7eEXA8VQYS6UKhAXKtq3LkO0tibVnpLw0sd0nQIAZgz7R2j+EubgBHg5sg3UwiMd/VOHEyvMIpMI6841H2c2GfejxtwAoSBm7tjUlnD5Dwf+znqkz7ExRdA+1IbOOOvObsnm4tc+jF+j8K0IlJuPOtsFCViu+d7qKj14hGFWElauzz3LKYL1F5Uf1YxSMuV/nUKhMd72RgZunuF/H2GBbcfyX4BgJRbiJmfXYLzNc1U3HHAhM=";
}
