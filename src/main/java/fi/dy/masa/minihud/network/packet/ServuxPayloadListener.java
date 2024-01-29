package fi.dy.masa.minihud.network.packet;

import fi.dy.masa.malilib.interfaces.IServuxPayloadListener;
import fi.dy.masa.malilib.network.ClientNetworkPlayHandler;
import fi.dy.masa.malilib.network.payload.ServuxPayload;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.DataStorage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ServuxPayloadListener implements IServuxPayloadListener
{
    /**
     * StructureDataPacketHandler (Replaced), etc. using new networking API, and greatly simplifies the work flow.
     * --------------------------------------------------------------------------------------------------------------------
     * In here, We don't need to encapsulate Identifiers either, but I'm only passing it along for the DataProvider Code.
     * It's the same channel ID, and the same HashCode() for all players being utilized by ServuxPayload.  The Server
     * only cares what player you want to send a packet to, with what Payload type.  Also, unless there is a magical ton
     * of structure data, PacketSplitter will never be used.  Also, Carpet no longer provides a "Structures" packet...
     * So, let's get this done.
     */
    private int timeout;
    private boolean registered;
    @Override
    public void reset() { this.registered = false; }
    @Override
    public void sendServuxPayload(NbtCompound data)
    {
        ServuxPayload payload = new ServuxPayload(data);
        MiniHUD.printDebug("ServuxPayloadListener#sendServuxPayload(): sending payload of size {} bytes.", data.getSizeInBytes());
        ClientNetworkPlayHandler.sendServUX(payload);
    }
    @Override
    public void receiveServuxPayload(NbtCompound data, ClientPlayNetworking.Context ctx, Identifier id)
    {
        decodeServuxPayload(data, id);
    }
    @Override
    public void encodeServuxPayload(NbtCompound data, Identifier id)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(data);
        nbt.putString("id", id.toString());
        MiniHUD.printDebug("ServuxPayloadListener#encodeServuxPayload(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxPayload(nbt);
    }
    @Override
    public void decodeServuxPayload(NbtCompound data, Identifier id)
    {
        int packetType = data.getInt("packetType");
        MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): received packetType: {}, of size in bytes: {}.", packetType, data.getSizeInBytes());

        if (packetType == ServuxPacketType.PACKET_S2C_METADATA)
        {
            MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): received METADATA packet, of size in bytes: {}.", data.getSizeInBytes());
            int version = data.getInt("version");
            String identifier = data.getString("id");
            if (version == ServuxPacketType.PROTOCOL_VERSION && identifier.equals(id.toString()))
            {
                this.timeout = data.getInt("timeout");
                this.registered = true;
                DataStorage.getInstance().setIsServuxServer();
                int x = data.getInt("spawnPosX");
                int y = data.getInt("spawnPosY");
                int z = data.getInt("spawnPosZ");
                BlockPos spawnPos = new BlockPos(x, y, z);
                MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): SpawnPos: {} versus {}", DataStorage.getInstance().getWorldSpawn().toShortString(), spawnPos.toShortString());
                DataStorage.getInstance().setWorldSpawn(spawnPos);
                int radius = data.getInt("spawnChunkRadius");
                MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): SpawnChunkRadius: {} versus {}", DataStorage.getInstance().getSpawnChunkRadius(), radius);
                DataStorage.getInstance().setSpawnChunkRadius(radius);
                MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): register; timeout: {}", this.timeout);

                // Accept / Decline Structure Data based on Render toggle.
                if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
                {
                    NbtCompound nbt = new NbtCompound();
                    nbt.putInt("packetType", ServuxPacketType.PACKET_C2S_STRUCTURES_ACCEPT);
                    MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): sending STRUCTURES_ACCEPT packet");
                    sendServuxPayload(nbt);
                }
                else
                {
                    MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): not sending STRUCTURES_ACCEPT packet");
                }
            }
            else
            {
                MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): Received invalid Metadata (version: {}, id: {})", version, identifier);
            }
        }
        else if (packetType == ServuxPacketType.PACKET_S2C_SPAWN_METADATA)
        {
            MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): received SPAWN_METADATA packet, of size in bytes: {}.", data.getSizeInBytes());
            int x = data.getInt("spawnPosX");
            int y = data.getInt("spawnPosY");
            int z = data.getInt("spawnPosZ");
            BlockPos spawnPos = new BlockPos(x, y, z);
            MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): SpawnPos: {} versus {}", DataStorage.getInstance().getWorldSpawn().toShortString(), spawnPos.toShortString());
            DataStorage.getInstance().setWorldSpawn(spawnPos);
            int radius = data.getInt("spawnChunkRadius");
            MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): SpawnChunkRadius: {} versus {}", DataStorage.getInstance().getSpawnChunkRadius(), radius);
            DataStorage.getInstance().setSpawnChunkRadius(radius);
        }
        else if (packetType == ServuxPacketType.PACKET_S2C_STRUCTURE_DATA)
        {
            MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): received STRUCTURE_DATA packet, of size in bytes: {}.", data.getSizeInBytes());
            NbtList structures = data.getList("Structures", Constants.NBT.TAG_COMPOUND);
            MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): structures; list size: {}", structures.size());
            DataStorage.getInstance().addOrUpdateStructuresFromServer(structures, this.timeout, true);
        }
        else
        {
            MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): received unhandled packetType of size {} bytes.", data.getSizeInBytes());
        }
    }
}
