package me.antidotaleks.iter.utils;

import com.google.common.collect.Lists;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.FakePlayer;
import me.antidotaleks.iter.elements.GameItem;
import me.antidotaleks.iter.elements.GamePlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class InfoDisplay {

    private final GamePlayer gamePlayer;
    private final FakePlayer fakePlayer;
    private final Player player;
    private final TeamDetails teamDetails;

    private final TextDisplay infoDisplay;
    private final TextDisplay fakePlayerInfoDisplay;
    private final ItemDisplay cursor;
    private BukkitRunnable cursorUpdater;

    // Common

    public InfoDisplay(GamePlayer player) {
        this.gamePlayer = player;
        this.fakePlayer = player.getFakePlayer();
        this.player = player.getPlayer();
        teamDetails = gamePlayer.getTeamDetails();

        // Create displays
        infoDisplay = newNicknameInfo(true);
        fakePlayerInfoDisplay = newNicknameInfo(false);

        cursor = newCursor();
        newCursorUpdater();

        updateData();
    }

    private TextDisplay newNicknameInfo(boolean isForRealPlayer) {
        TextDisplay infoDisplay = Iter.overworld.spawn(gamePlayer.getGame().getMapLocation(), TextDisplay.class);

        infoDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        infoDisplay.setSeeThrough(true);
        infoDisplay.setShadowed(true);
        infoDisplay.setBrightness(new Display.Brightness(15, 15));
        infoDisplay.setBillboard(Display.Billboard.CENTER);
        infoDisplay.setBackgroundColor(Color.fromARGB(0,0,0,0));
        infoDisplay.setTeleportDuration(3);

        if (isForRealPlayer)
            infoDisplay.setTextOpacity((byte) 100);

        // hide info for themselves
        if (isForRealPlayer)
            this.player.hideEntity(Iter.plugin, infoDisplay);

        return infoDisplay;
    }

    private static final ItemStack CURSOR_DATA = new ItemStack(Material.GLASS_PANE, 1);

    private ItemDisplay newCursor() {
        ItemDisplay cursor = this.player.getWorld().spawn(gamePlayer.getGame().getMapLocation(), ItemDisplay.class);

        cursor.setItemStack(CURSOR_DATA);
        cursor.setBrightness(new Display.Brightness(15, 15));
        cursor.setTeleportDuration(1);
        cursor.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0),
                new Vector3f(3f, 3, 0.1f),
                new AxisAngle4f()
        ));

        gamePlayer.getGame().getAllPlayers().forEach(p -> p.hideEntity(Iter.plugin, cursor));
        player.showEntity(Iter.plugin, cursor);

        return cursor;
    }

    private void newCursorUpdater() {

        cursorUpdater = new BukkitRunnable() {
                    Location lastLocation = player.getLocation();
                    @Override
                    public void run() {
                        Location newLocation = player.getLocation();
                        if (newLocation.equals(lastLocation))
                            return;

                        lastLocation = newLocation;

                        Location lookAt = gamePlayer.getLookTileWorldPosition();
                        if (lookAt != null) {
                            cursor.teleport(lookAt);
                            return;
                        }
                        Location underMap = cursor.getLocation();
                        underMap.setY(0);
                        cursor.teleport(underMap);
                    }
                };
    }

    // Display

    public void updateData() {
        int health = gamePlayer.getHealth();
        int maxHealth = gamePlayer.getMaxHealth();
        int energy = gamePlayer.getEnergy();
        int maxEnergy = gamePlayer.getMaxEnergy();
        String teamName = "Team " + teamDetails.toString();

        // Update scoreboard
        String infoString = String.format("%s%s: %s%s\n"+ ChatColor.of("#ff5252") +"❤%d/%d "+ChatColor.RESET+"| "+ChatColor.of("#5297ff")+"♦ %d/%d\n",
                        ChatColor.of(teamDetails.color), teamName, ChatColor.of(teamDetails.lightColor), this.player.getName(), health, maxHealth, energy, maxEnergy);

        infoDisplay.setText(infoString);
        fakePlayerInfoDisplay.setText(infoString);
    }

    public void showCursor() {
        player.showEntity(Iter.plugin, cursor);
        cursorUpdater.runTaskTimerAsynchronously(Iter.plugin, 0, 1);
    }

    public void hideCursor() {
        player.hideEntity(Iter.plugin, cursor);
        try {
            cursorUpdater.cancel();
        } catch (Exception ignored) {}
        newCursorUpdater();
    }

    // UI

    public void cardList() {
        BaseComponent[] itemsAsText = gamePlayer.getItems().stream()
                .flatMap(item -> actionbarCard(item, false).stream())
                .toArray(BaseComponent[]::new);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, itemsAsText);
            }
        }.runTaskTimer(Iter.plugin, 0, 10);

    }

    // Utils

    public void remove() {
        dismount();
        infoDisplay.remove();
        fakePlayerInfoDisplay.remove();

        try {
            cursorUpdater.cancel();
        } catch (Exception ignored) {}
        cursor.remove();
    }

    public void mount() {
        player.addPassenger(infoDisplay);
        fakePlayer.addPassenger(fakePlayerInfoDisplay);
    }

    public void dismount() {
        this.player.removePassenger(infoDisplay);
        this.fakePlayer.removePassenger(fakePlayerInfoDisplay);
    }

    private static final String[] CARD_OFFSET_FONT = new String[]{"cards", "cards_low"};
    private static final String[] TEXT_OFFSET_FONT = new String[]{"mono", "mono_low1", "mono_low2", "mono_low3"};

    private List<BaseComponent> actionbarCard(GameItem item, boolean expanded) {
        String[] title = cardTitle(item.getName());
        int offset = expanded?0:1;

        TextComponent cardSymbol = new TextComponent(String.valueOf(item.getCardSymbol()));
        cardSymbol.setFont(CARD_OFFSET_FONT[offset]);
        TextComponent title1 = new TextComponent(title[0]);
        title1.setFont(TEXT_OFFSET_FONT[offset]); title1.setColor(ChatColor.BLACK);
        TextComponent title2 = new TextComponent(title[1]);
        title2.setFont(TEXT_OFFSET_FONT[offset+1]); title2.setColor(ChatColor.BLACK);
        TextComponent title3 = new TextComponent(title[2]);
        title3.setFont(TEXT_OFFSET_FONT[offset+2]); title3.setColor(ChatColor.BLACK);

        List<BaseComponent> card = new ArrayList<>();

        card.add(cardSymbol);
        card.add(Iter.offset(-3*title[0].length() - 30));
        card.add(title1);
        card.add(Iter.offset(-3*(title[0].length() + title[1].length())));
        card.add(title2);
        card.add(Iter.offset(-3*(title[1].length() + title[2].length())));
        card.add(title3);
        card.add(Iter.offset(-3*title[2].length() + 30));

        return card;
    }

    private String[] cardTitle(String title) {
        List<String> split = titleSplit(title);
        return switch (split.size()) {
            case 1 -> new String[]{"", split.get(0), ""};
            case 2 -> new String[]{split.get(0), split.get(1), ""};
            default -> new String[]{split.get(0), split.get(1), split.get(2)};
        };
    }

    private final static int MAX_TITLE_WIDTH = 9;
    private List<String> titleSplit(String title) {
        if (title.length() <= MAX_TITLE_WIDTH)
            return Lists.newArrayList(title);

        boolean spaceFound = false;
        int spaceIndex = MAX_TITLE_WIDTH - 1;
        for (int i = MAX_TITLE_WIDTH; i >= 0; i--)
            if (title.charAt(i) == ' ') {
                spaceIndex = i;
                spaceFound = true;
                break;
            }

        List<String> split = titleSplit(title.substring(spaceIndex+(spaceFound?1:0)));
        split.addFirst(title.substring(0, spaceIndex) + (spaceFound?"":"-"));
        return split;
    }

    // Getters

    public Player getPlayer() {
        return player;
    }
}