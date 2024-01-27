package fi.dy.masa.minihud.network.packet;

import fi.dy.masa.malilib.interfaces.IServuxPayloadListener;
import fi.dy.masa.malilib.network.ClientNetworkPlayHandler;
import fi.dy.masa.malilib.network.payload.ServuxPayload;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.util.DataStorage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;

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
        MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): received packet of size in bytes: {}.", data.getSizeInBytes());
        int packetType = data.getInt("packetType");
        if (packetType == ServuxPacketType.PACKET_S2C_STRUCTURE_DATA)
        {
            NbtList structures = data.getList("Structures", Constants.NBT.TAG_COMPOUND);
            MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): structures; list size: {}", structures.size());
            DataStorage.getInstance().addOrUpdateStructuresFromServer(structures, this.timeout, true);
        }
        else if (packetType == ServuxPacketType.PACKET_S2C_METADATA)
        {
            int version = data.getInt("version");
            String identifier = data.getString("id");
            if (version == ServuxPacketType.PROTOCOL_VERSION && identifier.equals(id.toString()))
            {
                this.timeout = data.getInt("timeout");
                this.registered = true;
                int radius = data.getInt("spawnChunkRadius");
                MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): SpawnChunkRadius: {} versus {}", DataStorage.getInstance().getSpawnChunkRadius(), radius);
                DataStorage.getInstance().setIsServuxServer();
                DataStorage.getInstance().setSpawnChunkRadius(radius);
                MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): register; timeout: {}", this.timeout);
            }
            else
            {
                MiniHUD.printDebug("ServuxPayloadListener#decodeServuxPayload(): Received invalid Metadata (version: {}, id: {})", version, identifier);
            }
        }
    }
}
