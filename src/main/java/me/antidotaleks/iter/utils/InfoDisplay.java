package me.antidotaleks.iter.utils;

import com.comphenix.protocol.wrappers.Pair;
import com.google.common.collect.Lists;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Conditional;
import me.antidotaleks.iter.utils.items.Cooldown;
import me.antidotaleks.iter.utils.items.GameItem;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
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
import java.util.Map;

import static me.antidotaleks.iter.Iter.offset;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.BLACK;

public final class InfoDisplay {

    private final GamePlayer gamePlayer;
    private final FakePlayer fakePlayer;
    private final Player player;
    private final Audience audience;
    private final TeamStyling teamStyling;

    private final TextDisplay infoDisplay;
    private final TextDisplay fakePlayerInfoDisplay;
    private final ItemDisplay cursor;
    private BukkitRunnable cursorUpdater;

    // Common

    public InfoDisplay(GamePlayer player) {
        this.gamePlayer = player;
        this.fakePlayer = player.getFakePlayer();
        this.player = player.getPlayer();
        this.audience = Iter.audiences.player(this.player);
        this.teamStyling = gamePlayer.getTeamStyling();

        // Create displays
        infoDisplay = newAbovePlayerInfoDisplay(true);
        fakePlayerInfoDisplay = newAbovePlayerInfoDisplay(false);

        cursor = newCursor();

        updateInfoDisplays();
    }

    public void nextTurn() {
        updateInfoDisplays();
    }

    // Display creation

    private TextDisplay newAbovePlayerInfoDisplay(boolean isForRealPlayer) {
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

    // Display

    public void updateInfoDisplays() {
        int health = gamePlayer.getHealth();
        int maxHealth = gamePlayer.getMaxHealth();
        int energy = gamePlayer.getEnergy();
        int maxEnergy = gamePlayer.getMaxEnergy();
        String teamName = "Team " + teamStyling.toString();

        // Update scoreboard
        String infoString = String.format("%s%s: %s%s\n"+ ChatColor.of("#ff5252") +"❤%d/%d "+ChatColor.RESET+"| "+ChatColor.of("#5297ff")+"♦ %d/%d\n",
                        ChatColor.of(teamStyling.color), teamName, ChatColor.of(teamStyling.lightColor), this.player.getName(), health, maxHealth, energy, maxEnergy);

        infoDisplay.setText(infoString);
        fakePlayerInfoDisplay.setText(infoString);
    }

    // Card UI

    Sidebar sidebar = null;
    private Component cardListToShow = null;
    BukkitRunnable cardUpdater = null;

    public void changeSelectedCard() {
        int index = gamePlayer.getCurrentItemIndex();
        if (cards.isEmpty())
            updateInventory();
        cardListToShow = cards.get(index).getKey();
        cardListToShow = addCardBlocks(cardListToShow, index);
        audience.sendActionBar(cardListToShow);

        if (sidebar == null) {
            ScoreboardLibrary sl = Iter.scoreboardLibrary;
            sidebar = sl.createSidebar(15);
            sidebar.addPlayer(player);

            cardUpdater = new BukkitRunnable() {@Override public void run() {
                audience.sendActionBar(cardListToShow);
            }};
            cardUpdater.runTaskTimer(Iter.plugin, 0, 15);
        }

        Component[] sidebarCard = cards.get(index).getValue();
        for (int i = 0; i < sidebarCard.length; i++)
            sidebar.line(i, sidebarCard[i]);
    }

    private static final String CARD_BLOCK = "\uEFFE";
    private Component addCardBlocks(Component cardList, int index) {
        List<Pair<GameItem, Boolean>> itemBlockedPairList = gamePlayer.getItems();
        int itemAmount = itemBlockedPairList.size();


        cardList = cardList.append(offset( -itemAmount*63 ));
        for (int i = 0; i < itemBlockedPairList.size(); i++) {
            Pair<GameItem, Boolean> pair = itemBlockedPairList.get(i);

            if (!pair.getSecond()) {
                cardList = cardList.append(offset( 63 ));
                continue;
            }

            cardList = cardList.append(text(CARD_BLOCK, CARD_FONT[index == i ? 0 : 1]));

            GameItem item = pair.getFirst();
            if (!(item instanceof Cooldown itemCooldown) || itemCooldown.getCooldown() == 0)
                continue;

            int cooldown = itemCooldown.getCooldown();
            cardList = cardList
                    .append(offset(-31 - (cooldown+"").length()*3))
                    .append(text(cooldown+"", TEXT_OFFSET_FONT[4 + (index == i ? 0 : 1)]))
                    .append(offset( 31 - (cooldown+"").length()*3));
        }

        return cardList;
    }

    ArrayList<Map.Entry<Component, Component[]>> cards = new ArrayList<>();
    public void updateInventory() {
        for (int i = 0; i < gamePlayer.getItems().size(); i++) {
            GameItem item = gamePlayer.getItems().get(i).getFirst();

            cards.add(Map.entry(cardList(i), sidebarCard(item)));
        }
        changeSelectedCard();
    }

    public void showCursor() {
        player.showEntity(Iter.plugin, cursor);
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
        cursorUpdater.runTaskTimerAsynchronously(Iter.plugin, 0, 1);
    }

    public void hideCursor() {
        player.hideEntity(Iter.plugin, cursor);
        try {
            cursorUpdater.cancel();
        } catch (Exception ignored) {}
        ;
    }

    public Component cardList(int activeIndex) {
        final int[] i = {0};
        return gamePlayer.getItems().stream()
                .map(pair -> actionbarCard(pair.getFirst(), i[0]++ == activeIndex)).reduce(Component.empty(), Component::append).compact();
    }

    // Utils

    public void remove() {
        dismount();
        infoDisplay.remove();
        fakePlayerInfoDisplay.remove();
        if (sidebar != null)
            sidebar.close();
        if (cardUpdater != null)
            cardUpdater.cancel();
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent());

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

    // Adventure API components generators for cards

    private static final Style[]
            CARD_FONT = new Style[]{
                    Style.empty().font(Key.key("cards")),
                    Style.empty().font(Key.key("cards_low"))
            },
            TEXT_OFFSET_FONT = new Style[]{
                    Style.empty().font(Key.key("mono")),
                    Style.empty().font(Key.key("mono_low1")),
                    Style.empty().font(Key.key("mono_low2")),
                    Style.empty().font(Key.key("mono_low3")),
                    Style.empty().font(Key.key("mono_low4")),
                    Style.empty().font(Key.key("mono_low5"))
            };
    private Component actionbarCard(GameItem item, boolean raised) {
        String[] title = cardTitle(item.getName());
        int offset = raised?0:1;

        Component card = cardInfoBase(item, raised);

        Component[] offsets = new Component[]{
                offset(-3*title[0].length() - 30),
                offset(-3*(title[0].length() + title[1].length())),
                offset(-3*(title[1].length() + title[2].length())),
                offset(-3*title[2].length() + 30)
        };
        for (int i = 0; i < 3; i++) {
            Component line = text(title[i])
                    .style(TEXT_OFFSET_FONT[offset+i].color(BLACK));

            card = card.append(offsets[i]);
            card = card.append(line);
        }
        card = card.append(offsets[offsets.length-1]);

        return card;
    }

    private static final String[] SIDEBAR_CARD_OFFSETS = new String[]{"\uDB00\uDC30", "\uDAFF\uDF89"};
    private static final String CARD_BACKSIDE = "\uEFFF";
    private static Component[] sidebarCard(GameItem item) {
        Component[] card = new Component[10];

        card[0] = text(SIDEBAR_CARD_OFFSETS[0])
                .append(cardInfoBase(item, false))
                .append(text(SIDEBAR_CARD_OFFSETS[1]))
                .append(text(CARD_BACKSIDE, CARD_FONT[0]));
        card[1] = Component.empty();
        for (int i = 1; i <= 8; i++) {
            card[i+1] = translatable("card."+item.getName().replace(" ", "")+"."+i, TEXT_OFFSET_FONT[0].color(BLACK));
        }

        return card;
    }

    private static final int
            CHAR_WIDTH = 8,
            DIGIT_WIDTH = 5,
            SPACE_WIDTH = 15;
    private static Component cardInfoBase(GameItem item, boolean raised) {
        int offset = raised?0:1;
        Component card = text(item.getCardSymbol(), CARD_FONT[offset])
                .append(offset(-57));

        final Style digitsStyle = CARD_FONT[offset].color(BLACK);

        // Item with rounds Cooldown

        if (item instanceof Cooldown itemCooldown)
            card = card
                    .append(text("\uEFF0", CARD_FONT[offset]))
                    .append(text(itemCooldown.getMaxCooldown(), digitsStyle))
                    .append(offset((SPACE_WIDTH-2 - String.valueOf(itemCooldown.getMaxCooldown()).length() * DIGIT_WIDTH))); // why tf is this -2? Why not -1? Why it works?
        else
            card = card.append(offset(CHAR_WIDTH-1 + SPACE_WIDTH));

        // Item with Conditional usage

        if (item instanceof Conditional)
            card = card
                    .append(text("\uEFF1", CARD_FONT[offset]));
        else
            card = card.append(offset(CHAR_WIDTH));

        // Item with Energy usage

        int energy = item.getEnergyUsage();
        if (energy != 0)
            card = card
                    .append(offset((SPACE_WIDTH - String.valueOf(energy).length() * DIGIT_WIDTH)))
                    .append(text(energy, digitsStyle))
                    .append(text("\uEFF2", CARD_FONT[offset]))
                    .append(offset(4));
        else
            card = card.append(offset(CHAR_WIDTH + SPACE_WIDTH + 4));

        return card;
    }

    private static String[] cardTitle(String title) {
        List<String> split = titleSplit(title);
        return switch (split.size()) {
            case 1 -> new String[]{"", split.get(0), ""};
            case 2 -> new String[]{split.get(0), split.get(1), ""};
            default -> new String[]{split.get(0), split.get(1), split.get(2)};
        };
    }

    private final static int MAX_TITLE_WIDTH = 9;
    private static List<String> titleSplit(String title) {
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

    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public Player getPlayer() {
        return player;
    }
}