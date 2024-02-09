package fi.dy.masa.minihud.network.packet;

import fi.dy.masa.malilib.interfaces.IServuxMetadataListener;
import fi.dy.masa.malilib.network.ClientNetworkPlayHandler;
import fi.dy.masa.malilib.network.payload.channel.ServuxMetadataPayload;
import fi.dy.masa.minihud.MiniHUD;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class ServuxMetadataListener implements IServuxMetadataListener
{
    //private int timeout = -1;
    private boolean registered = false;
    @Override
    public void reset() { this.registered = false; }
    @Override
    public void sendServuxMetadata(NbtCompound data)
    {
        ServuxMetadataPayload payload = new ServuxMetadataPayload(data);
        MiniHUD.printDebug("ServuxMetadataListener#sendServuxMetadata(): sending payload of size {} bytes.", data.getSizeInBytes());
        ClientNetworkPlayHandler.sendServuxMetadata(payload);
    }
    @Override
    public void receiveServuxMetadata(NbtCompound data, ClientPlayNetworking.Context ctx, Identifier id)
    {
        decodeServuxMetadata(data, id);
    }
    @Override
    public void encodeServuxMetadata(NbtCompound data, Identifier id)
    {
        // Encode packet.
        NbtCompound nbt = new NbtCompound();
        nbt.copyFrom(data);
        nbt.putString("id", id.toString());
        MiniHUD.printDebug("ServuxMetadataListener#encodeServuxMetadata(): nbt.putByteArray() size in bytes: {}", nbt.getSizeInBytes());
        sendServuxMetadata(nbt);
    }
    @Override
    public void decodeServuxMetadata(NbtCompound data, Identifier id)
    {
        int packetType = data.getInt("packetType");
        MiniHUD.printDebug("ServuxMetadataListener#decodeServuxMetadata(): received packetType: {}, of size in bytes: {}.", packetType, data.getSizeInBytes());

        //this.timeout = 0;
        //this.registered = false;
    }
}
