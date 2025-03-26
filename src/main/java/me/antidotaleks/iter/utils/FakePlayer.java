package me.antidotaleks.iter.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.google.common.collect.Multimap;
import me.antidotaleks.iter.Iter;
import me.antidotaleks.iter.elements.GamePlayer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public final class FakePlayer {
    private final GamePlayer playerBase;
    private final List<Player> allPlayersInGame;
    private Location location;

    private int entityId = -1;
    private UUID uuid;

    // Common

    public FakePlayer(GamePlayer player) {
        this.playerBase = player;
        this.allPlayersInGame = player.getGame().getAllPlayers();

        spawnFakePlayer();
    }

    public void spawnFakePlayer() {
        try {
            spawnFakePlayer(allPlayersInGame);
            glow(playerBase.getPlayer());
        } catch (Exception e) {
            Iter.logger.warning("Error spawning fake player:");
            e.printStackTrace();
        }
    }

    private void spawnFakePlayer(List<Player> playersToSpawnTo) {

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
        location = playerBase.getWorldPosition();

        WrappedGameProfile profile = new WrappedGameProfile(uuid, playerBase.getPlayer().getName());
        Multimap<String, WrappedSignedProperty> properties = WrappedGameProfile.fromPlayer(playerBase.getPlayer()).getProperties();
        // System.out.println("Properties for "+playerBase.getPlayer().getName()+":");
        // properties.forEach((x, y)-> System.out.println(x+": "+y.toString())); TODO: test in online, then remove
        profile.getProperties().putAll(properties);
        WrappedChatComponent displayName = WrappedChatComponent.fromText(playerBase.getPlayer().getName());
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

        Iter.logger.info("Spawning fake player of " + playerBase.getPlayer().getName());

        pm.broadcastServerPacket(infoPacket, playersToSpawnTo);
        pm.broadcastServerPacket(spawnPacket, playersToSpawnTo);

        teleport(playerBase.getWorldPosition());
    }

    public void teleport(Location newLocation) {
        try {
            moveEntity(entityId, newLocation, allPlayersInGame);
            newLocation = newLocation.clone().add(0, 1.8, 0);
            for (int passengerId : passengerIds) {
                removePassenger(passengerId, allPlayersInGame);
                Iter.protocolManager.getEntityFromID(Iter.overworld, passengerId).teleport(newLocation);
                moveEntity(passengerId, newLocation, allPlayersInGame);
                addPassenger(passengerId, allPlayersInGame);
            }
        } catch (Exception e) {
            Iter.logger.warning("Error teleporting fake player:");
            e.printStackTrace();
        }
    }

    private void moveEntity(int entityId, Location newLocation, List<Player> playersToShowTeleport) {

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
        try {
            remove(allPlayersInGame);
        } catch (Exception e) {
            Iter.logger.warning("Error removing fake player:");
            e.printStackTrace();
        }
    }

    private void remove(List<Player> playersToRemoveFrom) {

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

        Iter.logger.info("Removing fake player of " + playerBase.getPlayer().getName());

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
            final WrappedDataWatcher.WrappedDataWatcherObject object = entry.getWatcherObject();
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
        try {
            addPassenger(entity.getEntityId(), allPlayersInGame);
        } catch (Exception e) {
            Iter.logger.warning("Error adding passenger to fake player:");
            e.printStackTrace();
        }
    }

    private void addPassenger(int passengerId, List<Player> playersToAddPassengerTo) {

        if(entityId == -1) {
            Iter.logger.warning("Attempted to add passenger to non-existent fake player");
            return;
        }

        // Filter list of players

        playersToAddPassengerTo = playersToAddPassengerTo.stream().filter(Objects::nonNull).toList();
        if(playersToAddPassengerTo.isEmpty()) return;
        passengerIds.add(passengerId);

        // Create packets

        updatePassengers(playersToAddPassengerTo);

    }

    public void removePassenger(Entity passenger) {
        try {
            removePassenger(passenger.getEntityId(), allPlayersInGame);
        } catch (Exception e) {
            Iter.logger.warning("Error removing passenger from fake player:");
            e.printStackTrace();
        }
    }

    private void removePassenger(int passengerId, List<Player> playersToRemovePassengerFrom) {

        // Filter list of players

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

}