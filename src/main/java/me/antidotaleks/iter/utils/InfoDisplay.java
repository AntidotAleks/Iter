package me.antidotaleks.iter.utils;

import com.comphenix.protocol.wrappers.Pair;
import com.google.common.collect.Lists;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.GamePlayer;
import me.antidotaleks.iter.utils.items.Conditional;
import me.antidotaleks.iter.utils.items.Cooldown;
import me.antidotaleks.iter.utils.items.GameItem;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static me.antidotaleks.iter.Iter.*;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.BLACK;

public final class InfoDisplay {

    private final GamePlayer gamePlayer;
    private final FakePlayer fakePlayer;
    private final Player player;
    private final Audience audience;
    private final TeamStyle teamStyle;

    private final TextDisplay
            infoDisplay,
            fakePlayerInfoDisplay;
    private final ItemDisplay cursor;
    private BukkitRunnable cursorUpdater;
    private BossBar topBar;

    // Common

    public InfoDisplay(GamePlayer player) {
        if (player == null)
            throw new IllegalArgumentException("Player cannot be null");
        if (player.getFakePlayer() == null)
            throw new IllegalStateException("Player must have FakePlayer object created before InfoDisplay");

        this.gamePlayer = player;
        this.fakePlayer = player.getFakePlayer();
        this.player = player.getPlayer();
        this.audience = Iter.audiences.player(this.player);
        this.teamStyle = player.getTeam().getStyle();

        this.infoDisplay = Iter.tryCatchReturn(() -> newAbovePlayerInfoDisplay(true));
        this.fakePlayerInfoDisplay = Iter.tryCatchReturn(() -> newAbovePlayerInfoDisplay(false));
        this.cursor = Iter.tryCatchReturn(this::newCursor);

        player.getPlayer().setPlayerListName(ChatColor.of(teamStyle.color) +"["+ teamStyle +"] "+ ChatColor.of(teamStyle.lightColor) + player.getPlayer().getName());

        update();
        mount();
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

        if (isForRealPlayer) {
            infoDisplay.setTextOpacity((byte) 100); // ~half transparent
            this.player.hideEntity(Iter.plugin, infoDisplay); // hide info for themselves
        }


        return infoDisplay;
    }


    private ItemDisplay newCursor() {
        ItemDisplay cursor = this.player.getWorld().spawn(gamePlayer.getGame().getMapLocation(), ItemDisplay.class);

        cursor.setItemStack(CURSOR_IS);
        cursor.setBrightness(new Display.Brightness(15, 15));
        cursor.setTeleportDuration(1);
        cursor.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0),
                new Vector3f(3f, 3, 0.1f),
                new AxisAngle4f()
        ));

        gamePlayer.getGame().getAllBukkitPlayers().forEach(p -> p.hideEntity(Iter.plugin, cursor));
        player.showEntity(Iter.plugin, cursor);

        return cursor;
    }

    // Display

    public void update() {
        tryCatch(this::updateInfoDisplays);
        tryCatch(this::updateTopBar);
        tryCatch(this::updateSelection);
    }

    public void updateInfoDisplays() {
        int health = gamePlayer.getHealth();
        int maxHealth = gamePlayer.getMaxHealth();
        int energy = gamePlayer.getEnergy();
        int maxEnergy = gamePlayer.getMaxEnergy();
        String teamName = "Team " + teamStyle.toString();

        // Update scoreboard
        String infoString = String.format("%s%s: %s%s\n"+ ChatColor.of(HEALTH_COLOR_HEX) +"❤%d/%d "+ChatColor.RESET+"| "+ChatColor.of(ENERGY_COLOR_HEX)+"♦ %d/%d\n",
                        ChatColor.of(teamStyle.color), teamName, ChatColor.of(teamStyle.lightColor), this.player.getName(), health, maxHealth, energy, maxEnergy);

        infoDisplay.setText(infoString);
        fakePlayerInfoDisplay.setText(infoString);
    }

    public void updateTopBar() {
        if (topBar == null)
            return;

        final TextColor
                healthColor = TextColor.fromHexString(HEALTH_COLOR_HEX),
                energyColor = TextColor.fromHexString(ENERGY_COLOR_HEX);

        int health = gamePlayer.getHealth();
        int maxHealth = gamePlayer.getMaxHealth();
        int energy = gamePlayer.getEnergy();
        int maxEnergy = gamePlayer.getMaxEnergy();

        Component tempText = text("❤ "+health+"/"+maxHealth).style(MONO_OFFSET_FONTS[1])
                .append(offset(60))
                .append(text("♦ "+energy+"/"+maxEnergy).style(MONO_OFFSET_FONTS[1].color(energyColor)));
        topBar.name(tempText);

        topBar.progress((float) health / maxHealth * HEALTH_BAR_FRACTION);
    }

    // Card UI

    Sidebar sidebar;
    private Component bottomBarText;
    BukkitRunnable bottomBarUpdater;

    public void updateSelection() {
        if (cards.isEmpty())
            return;

        // First time - create sidebar and actionbar runnable (actionbar disappears shortly without update)
        if (sidebar == null) {
            ScoreboardLibrary sl = Iter.scoreboardLibrary;
            sidebar = sl.createSidebar(15);
            sidebar.addPlayer(player);

            bottomBarUpdater = new BukkitRunnable() {@Override public void run() {
                audience.sendActionBar(bottomBarText);
            }};
            bottomBarUpdater.runTaskTimer(Iter.plugin, 0, 15);
        }

        int index = gamePlayer.getCurrentItemIndex();
        var cardTextBase = cards.get(index);

        // Bottom Bar card to show
        bottomBarText = addCardBlocksToBase(cardTextBase.getKey(), index);
        audience.sendActionBar(bottomBarText);

        // Sidebar card list to show
        Component[] sidebarCard = cardTextBase.getValue();
        for (int i = 0; i < 10; i++)
            sidebar.line(i+5, sidebarCard[i]);
    }

    public void updateCardUseHistory() {
        if (sidebar == null)
            return;

        var usedItems = gamePlayer.getItemsUsed();
        int usedAmount = usedItems.size();
        logger.info(usedAmount+ " left");

        // Clear previous cards
        for (int i = 0; i < 5-usedAmount; i++)
            sidebar.line(i, empty());

        if (usedAmount <= 5) {
            // show all used cards
            for (int i = 0; i < usedAmount; i++)
                sidebar.line(5-usedAmount+i, text(SIDEBAR_CARD_OFFSETS[0])
                        .append(cardBase(usedItems.get(i).getKey(), false)));
        }
        else {
            // show the number of cards used except last 4
            sidebar.line(0, text("\uEFFE",CARD_FONT[0])
                    .append(offset(-50))
                    .append(text("+"+(usedAmount-4), MONO_OFFSET_FONTS[0])));
            // and 4 latest used cards
            for (int i = 1; i < 5; i++)
                sidebar.line(i, text(SIDEBAR_CARD_OFFSETS[0])
                        .append(cardBase(usedItems.get(usedAmount-5+i).getKey(), false)));
        }
    }

    private Component addCardBlocksToBase(Component cardList, int index) {
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
                    .append(text(cooldown+"", MONO_OFFSET_FONTS[4 + (index == i ? 0 : 1)]))
                    .append(offset( 31 - (cooldown+"").length()*3));
        }

        return cardList;
    }

    ArrayList<Map.Entry<Component, Component[]>> cards = new ArrayList<>();
    public void updateInventoryItemData() {
        for (int i = 0; i < gamePlayer.getItems().size(); i++) {
            GameItem item = gamePlayer.getItems().get(i).getFirst();

            cards.add(Map.entry(bottomBarCardList(i), sidebarCard(item)));
        }
        updateSelection();
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
        tryIgnored(() -> cursorUpdater.cancel());
    }

    public Component bottomBarCardList(int activeIndex) {
        final int[] i = {0};
        return gamePlayer.getItems().stream()
                .map(pair -> bottomBarCard(pair.getFirst(), i[0]++ == activeIndex)).reduce(Component.empty(), Component::append).compact();
    }

    public void showTopBars() {
        Audience playerAsAudience = audiences.player(player);
        topBar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        topBar.addViewer(playerAsAudience);

        updateTopBar();
    }

    // Utils

    public void remove() {
        // info displays
        dismount();
        infoDisplay.remove();
        fakePlayerInfoDisplay.remove();

        // Fake player
        fakePlayer.removeFakePlayer();

        // Sidebar
        if (sidebar != null)
            sidebar.close();

        // Bottom bar
        if (bottomBarUpdater != null)
            bottomBarUpdater.cancel();
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent()); // Clear player's actionbar / bottom bar

        // Top bar
        if (topBar != null)
            topBar.removeViewer(audience);

        // Cursor
        tryIgnored(cursorUpdater::cancel);
        if (cursor != null)
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

    private Component bottomBarCard(GameItem item, boolean raised) {
        String[] title = cardTitle(item.getName());
        int offset = raised?0:1;

        Component card = cardBase(item, raised);

        Component[] offsets = new Component[]{
                offset(-3*title[0].length() - 30),
                offset(-3*(title[0].length() + title[1].length())),
                offset(-3*(title[1].length() + title[2].length())),
                offset(-3*title[2].length() + 30)
        };
        for (int i = 0; i < 3; i++) {
            Component line = text(title[i])
                    .style(MONO_OFFSET_FONTS[offset+i].color(BLACK));

            card = card.append(offsets[i]);
            card = card.append(line);
        }
        card = card.append(offsets[offsets.length-1]);

        return card;
    }

    private static Component[] sidebarCard(GameItem item) {
        Component[] card = new Component[10];

        card[0] = text(SIDEBAR_CARD_OFFSETS[0])
                .append(cardBase(item, false))
                .append(text(SIDEBAR_CARD_OFFSETS[1]))
                .append(text(CARD_BACKSIDE, CARD_FONT[0]));
        card[1] = Component.empty();
        for (int i = 1; i <= 8; i++) {
            card[i+1] = translatable("card."+item.getName().replace(" ", "")+"."+i, MONO_OFFSET_FONTS[0].color(BLACK)).append(offset(-8192));
        }

        return card;
    }

    private static Component cardBase(GameItem item, boolean is_raised) {
        final int  // Some statics
            OFFSET_TO_LEFT_SIDE = -57, OFFSET_TO_RIGHT_SIDE = 4, // -OFFSET_TO_LEFT_SIDE = OFFSET_TO_RIGHT_SIDE + (2 x ICON_WIDTH + CD_ICON_WIDTH) + (2 x SPACE_WIDTH)
            ICON_WIDTH = 8, CD_ICON_WIDTH = 9, DIGIT_WIDTH = 5, SPACE_WIDTH = 14; //Cooldown icon is 1 pixel wider

        final Style card_font = CARD_FONT[is_raised?0:1];
        Component card = text(item.getCardSymbol(), card_font)
                .append(offset(OFFSET_TO_LEFT_SIDE)); // Move to ~card's left side (from where symbols are added)

        final Style digitsStyle = card_font.color(BLACK);

        // Item with rounds Cooldown

        if (item instanceof Cooldown itemCooldown)
            card = card
                    .append(text(COOLDOWN_CARD_ICON, card_font)) // Cooldown icon
                    .append(text(itemCooldown.getMaxCooldown(), digitsStyle)) // Cooldown number
                    .append(offset((SPACE_WIDTH - String.valueOf(itemCooldown.getMaxCooldown()).length() * DIGIT_WIDTH))); // offset from right side of number
        else
            card = card.append(offset(CD_ICON_WIDTH + SPACE_WIDTH)); // If item doesn't have cooldown

        // Item with Conditional usage

        if (item instanceof Conditional)
            card = card.append(text(CONDITIONAL_CARD_ICON, card_font)); // Conditional icon
        else
            card = card.append(offset(ICON_WIDTH)); // If item doesn't have conditional

        // Item with Energy usage

        int energy = item.getEnergyUsage();
        if (energy != 0)
            card = card
                    .append(offset((SPACE_WIDTH - String.valueOf(energy).length() * DIGIT_WIDTH))) // Offset to left side of number
                    .append(text(energy, digitsStyle)) // Energy usage number
                    .append(text(ENERGY_USE_CARD_ICON, card_font)) // Energy usage icon
                    .append(offset(OFFSET_TO_RIGHT_SIDE)); // Move to card's right side
        else
            card = card.append(offset(ICON_WIDTH + SPACE_WIDTH + OFFSET_TO_RIGHT_SIDE)); // If item doesn't use energy + move to card's right side

        return card;
    }

    private static String[] cardTitle(String title) {
        List<String> split = cardTitleSplit(title);
        return switch (split.size()) {
            case 1 -> new String[]{"", split.get(0), ""};
            case 2 -> new String[]{split.get(0), split.get(1), ""};
            default -> new String[]{split.get(0), split.get(1), split.get(2)};
        };
    }

    private final static int MAX_TITLE_WIDTH = 9;
    private static List<String> cardTitleSplit(String title) {
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

        List<String> split = cardTitleSplit(title.substring(spaceIndex+(spaceFound?1:0)));
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