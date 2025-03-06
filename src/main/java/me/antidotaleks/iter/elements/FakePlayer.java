package me.antidotaleks.iter.elements;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import me.antidotaleks.iter.Iter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FakePlayer {
    private final GamePlayer playerBase;
    private final List<Player> allPlayersInGame;

    public FakePlayer(GamePlayer player) {
        this.playerBase = player;
        this.allPlayersInGame = player.getGame().getAllPlayers();

        spawnFakePlayer(allPlayersInGame);
    }

    private void spawnFakePlayer(List<Player> playersToSpawnTo) {
        // Filter list of players
        playersToSpawnTo = playersToSpawnTo.stream().filter(Objects::nonNull).toList();
        if(playersToSpawnTo.isEmpty()) return;

        // Create packets

        ProtocolManager pm = Iter.protocolManager;

        PacketContainer playerInfoPacket = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
        PacketContainer spawnPacket = pm.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        // Generate info

        int entityId = (int) (Math.random() * Integer.MAX_VALUE);
        int entityType = 147; // 147 is for player
        UUID uuid = UUID.randomUUID();
        Location spawnLocation = playerBase.getWorldPosition();
        WrappedGameProfile profile = new WrappedGameProfile(uuid, playerBase.getPlayer().getName());
        PlayerInfoData playerInfoData = new PlayerInfoData(
                profile, 0, EnumWrappers.NativeGameMode.CREATIVE, null
        );

        // Player Info Packet

        playerInfoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        playerInfoPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));

        // Spawn Packet

        spawnPacket.getIntegers()
                .write(0, entityId) // Entity ID
                .write(1, entityType) // Type
                .write(2, 0); // Data
        spawnPacket.getUUIDs()
                .write(0, uuid); // UUID
        spawnPacket.getDoubles()
                .write(0, spawnLocation.getX()) // X
                .write(1, spawnLocation.getY()) // Y
                .write(2, spawnLocation.getZ()); // Z
        spawnPacket.getBytes()
                .write(0, (byte) 0) // Pitch
                .write(1, (byte) 0) // Yaw
                .write(2, (byte) 0); // Head Yaw
        spawnPacket.getShorts()
                .write(0, (short) 0) // Velocity X
                .write(1, (short) 0) // Velocity Y
                .write(2, (short) 0); // Velocity Z

        // Send Packets

        pm.broadcastServerPacket(playerInfoPacket, playersToSpawnTo);
        pm.broadcastServerPacket(spawnPacket, playersToSpawnTo);
    }
}
