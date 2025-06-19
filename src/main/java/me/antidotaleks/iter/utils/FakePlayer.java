package me.antidotaleks.iter.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.GamePlayer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

import static me.antidotaleks.iter.Iter.*;

public final class FakePlayer {
    private final GamePlayer gamePlayer;
    private Location location;

    private int entityId = -1;
    private UUID uuid;

    // Common

    public FakePlayer(GamePlayer player) {
        this.gamePlayer = player;
        spawnFakePlayer();
    }

    public void spawnFakePlayer() {
        tryCatch(() -> {
            spawnFakePlayer(gamePlayer.game.getAllBukkitPlayers());
            glow(gamePlayer.bukkitPlayer);
        }, "Error spawning fake player");
    }

    private void spawnFakePlayer(List<Player> playersToSpawnTo) {
        if(playersToSpawnTo == null) return;

        // Filter list of players

        playersToSpawnTo = playersToSpawnTo.stream().filter(Objects::nonNull).toList();
        if(playersToSpawnTo.isEmpty()) return;

        // Create packets

        ProtocolManager pm = Iter.protocolManager;
        PacketContainer infoPacket = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
        PacketContainer spawnPacket = pm.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        // Generate info

        if (entityId != -1)
            remove();
        entityId = (int) (Math.random() * Integer.MAX_VALUE);
        uuid = UUID.randomUUID();
        location = gamePlayer.getWorldPosition();

        Iter.logger.info("Spawning fake player for " + gamePlayer.bukkitPlayer.getName());
        WrappedGameProfile.fromPlayer(gamePlayer.bukkitPlayer).getProperties().forEach((name, property)
                -> Iter.logger.info(name+": "+property));

        WrappedGameProfile profile = new WrappedGameProfile(uuid, gamePlayer.bukkitPlayer.getName());
        profile.getProperties().put("textures", getPlayerSkin().orElse(
                new WrappedSignedProperty("textures", DEFAULT_SKIN, DEFAULT_SIGNATURE)
        ));
        WrappedChatComponent displayName = WrappedChatComponent.fromText(gamePlayer.bukkitPlayer.getName());
        PlayerInfoData playerInfoData = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.CREATIVE, displayName);

        // Info Packet

        infoPacket.getPlayerInfoActions()
                .write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER)); // Action
        infoPacket.getPlayerInfoDataLists()
                .write(1, List.of(playerInfoData)); // Player Info Data

        // Spawn Packet

        spawnPacket.getIntegers()
                .write(0, entityId); // Entity ID
        spawnPacket.getUUIDs()
                .write(0, uuid); // UUID
        spawnPacket.getEntityTypeModifier()
                .write(0, EntityType.PLAYER); // Entity Type
        spawnPacket.getDoubles()
                .write(0, location.getX()) // X
                .write(1, location.getY()) // Y
                .write(2, location.getZ()); // Z
        spawnPacket.getIntegers()
                .write(2, 0) // Velocity X
                .write(3, 0) // Velocity Y
                .write(4, 0); // Velocity Z
        spawnPacket.getBytes()
                .write(0, (byte) 0) // Pitch
                .write(1, (byte) 0) // Yaw
                .write(2, (byte) 0); // Head Yaw

        // Send Packets

        Iter.logger.info("Spawning fake player of " + gamePlayer.bukkitPlayer.getName());

        pm.broadcastServerPacket(infoPacket, playersToSpawnTo);
        pm.broadcastServerPacket(spawnPacket, playersToSpawnTo);

        teleport(gamePlayer.getWorldPosition());
    }

    public void teleport(Location newLocation) {
        var allPlayers = allPlayers();
        tryCatch(() -> {
            moveEntity(entityId, newLocation, allPlayers);
            Location passengerLocation = newLocation.clone().add(0, 1.8, 0);
            for (int passengerId : passengerIds) {
                removePassenger(passengerId, allPlayers);
                Iter.protocolManager.getEntityFromID(Iter.overworld, passengerId).teleport(passengerLocation);
                moveEntity(passengerId, passengerLocation, allPlayers);
                addPassenger(passengerId, allPlayers);
            }
        }, "Error teleporting fake player");
    }

    private void moveEntity(int entityId, Location newLocation, List<Player> playersToShowTeleport) {
        if(playersToShowTeleport == null) return;

        playersToShowTeleport = playersToShowTeleport.stream().filter(Objects::nonNull).toList();
        if(playersToShowTeleport.isEmpty()) return;

        // Create packets

        ProtocolManager pm = Iter.protocolManager;
        PacketContainer teleportPacket = pm.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

        // Teleport Packet

        teleportPacket.getIntegers()
                .write(0, entityId); // Entity ID
        teleportPacket.getDoubles()
                .write(0, newLocation.getX()) // X
                .write(1, newLocation.getY()) // Y
                .write(2, newLocation.getZ());// Z
        teleportPacket.getBytes()
                .write(0, (byte) 0) // Pitch
                .write(1, (byte) 0); // Yaw
        teleportPacket.getBooleans()
                .write(0, true); // On Ground

        // Send Packets

        pm.broadcastServerPacket(teleportPacket, playersToShowTeleport);

        this.location = newLocation;
    }

    public void remove() {
        tryCatch(() -> remove(gamePlayer.game.getAllBukkitPlayers()), "Error removing fake player");
    }

    private void remove(List<Player> playersToRemoveFrom) {
        if(playersToRemoveFrom == null) return;

        if(entityId == -1) return;

        // Filter list of players

        playersToRemoveFrom = playersToRemoveFrom.stream().filter(Objects::nonNull).toList();
        if(playersToRemoveFrom.isEmpty()) return;

        // Create packets

        ProtocolManager pm = Iter.protocolManager;
        PacketContainer destroyPacket = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        PacketContainer infoPacket = pm.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);

        // Destroy Packet

        destroyPacket.getIntLists()
                .write(0, List.of(entityId)); // Entity IDs

        // Remove Info Packet

        infoPacket.getUUIDLists()
                .write(0, List.of(uuid)); // UUIDs

        // Send Packets

        Iter.logger.info("Removing fake player of " + gamePlayer.bukkitPlayer.getName());

        pm.broadcastServerPacket(destroyPacket, playersToRemoveFrom);
        pm.broadcastServerPacket(infoPacket, playersToRemoveFrom);

        entityId = -1;
    }

    private void glow(Player playerToShowGlowTo) {

        if(playerToShowGlowTo == null) return;

        // Create packets

        ProtocolManager pm = Iter.protocolManager;
        PacketContainer glowPacket = pm.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        WrappedDataWatcher watcher = new WrappedDataWatcher();

        // Glow Packet

        WrappedDataWatcher.Serializer byteSerializer = WrappedDataWatcher.Registry.get(Byte.class);
        byte glowingMask = 0x40;
        watcher.setEntity(playerToShowGlowTo); // Player to show glow to for some unimaginable reason, like, why? we already know the player we are sending the packet to
        watcher.setObject(0, byteSerializer, glowingMask);

        final List<WrappedDataValue> wrappedDataValueList = watcher.getWatchableObjects().stream().filter(Objects::nonNull).map( entry -> {
            final var object = entry.getWatcherObject();
            return new WrappedDataValue(object.getIndex(), object.getSerializer(), entry.getRawValue());
        }).toList();

        glowPacket.getIntegers()
                .write(0, entityId); // Entity ID
        glowPacket.getDataValueCollectionModifier()
                .write(0, wrappedDataValueList);

        // Send Packets

        pm.sendServerPacket(playerToShowGlowTo, glowPacket);
    }

    // Passengers

    private final ArrayList<Integer> passengerIds = new ArrayList<>();

    public void addPassenger(Entity entity) {
        tryCatch(() -> addPassenger(entity.getEntityId(), allPlayers()), "Error adding passenger to fake player");
    }

    private void addPassenger(int passengerId, List<Player> playersToAddPassengerTo) {

        if(entityId == -1) {
            Iter.logger.warning("Attempted to add passenger to non-existent fake player");
            return;
        }

        // Filter list of players

        if(playersToAddPassengerTo == null) return;
        playersToAddPassengerTo = playersToAddPassengerTo.stream().filter(Objects::nonNull).toList();
        if(playersToAddPassengerTo.isEmpty()) return;
        passengerIds.add(passengerId);

        // Create packets

        updatePassengers(playersToAddPassengerTo);

    }

    public void removePassenger(Entity passenger) {
        tryCatch(() -> removePassenger(passenger.getEntityId(), allPlayers()), "Error removing passenger from fake player");
    }

    private void removePassenger(int passengerId, List<Player> playersToRemovePassengerFrom) {

        if(entityId == -1) {
            Iter.logger.warning("Attempted to remove passenger from non-existent fake player");
            return;
        }

        // Filter list of players
        if(playersToRemovePassengerFrom == null) return;
        playersToRemovePassengerFrom = playersToRemovePassengerFrom.stream().filter(Objects::nonNull).toList();
        if(playersToRemovePassengerFrom.isEmpty()) return;
        passengerIds.remove((Integer) passengerId);

        updatePassengers(playersToRemovePassengerFrom);
    }

    private void updatePassengers(List<Player> playersToUpdatePassengersFor) {

        // Create packets

        ProtocolManager pm = Iter.protocolManager;
        PacketContainer mountPacket = pm.createPacket(PacketType.Play.Server.MOUNT);

        // Mount Packet

        mountPacket.getIntegers()
                .write(0, entityId); // Vehicle ID
        mountPacket.getIntegerArrays()
                .write(0, passengerIds.stream().mapToInt(i->i).toArray()); // Passenger IDs

        // Send Packets

        pm.broadcastServerPacket(mountPacket, playersToUpdatePassengersFor);
    }

    // Player skin

    private Optional<WrappedSignedProperty> getPlayerSkin() {
        var properties = WrappedGameProfile.fromPlayer(gamePlayer.bukkitPlayer).getProperties();
        return properties.get("textures").stream().findFirst();
    }

    // Utils

    private List<Player> allPlayers() {
        return gamePlayer.game.getAllBukkitPlayers();
    }
}