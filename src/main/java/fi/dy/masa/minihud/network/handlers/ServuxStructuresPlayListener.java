package fi.dy.masa.minihud.network.handlers;

import fi.dy.masa.malilib.network.handler.ClientCommonHandlerRegister;
import fi.dy.masa.malilib.network.handler.play.ClientPlayHandler;
import fi.dy.masa.malilib.network.handler.play.IPluginPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.PayloadTypeRegister;
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.DataStorage;
import fi.dy.masa.minihud.network.PacketType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

public abstract class ServuxStructuresPlayListener<T extends CustomPayload> implements IPluginPlayHandler<T>
{
    public final static ServuxStructuresPlayListener<ServuxStructuresPayload> INSTANCE = new ServuxStructuresPlayListener<>() {
        @Override
        public void receive(ServuxStructuresPayload payload, ClientPlayNetworking.Context context)
        {
            //ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
            CallbackInfo ci = new CallbackInfo("ServuxStructuresPlayListener",false);

            //if (handler != null)
            //{
                //ServuxStructuresPlayListener.INSTANCE.receiveS2CPlayPayload(PayloadType.SERVUX_STRUCTURES, payload, handler, ci);
                // Servux doesn't need to use the networkHandler interface.
            //}
            //else
                ServuxStructuresPlayListener.INSTANCE.receiveS2CPlayPayload(PayloadType.SERVUX_STRUCTURES, payload, context);
        }
    };
    private final Map<PayloadType, Boolean> registered = new HashMap<>();
    private int timeout;
    private boolean register;
    @Override
    public PayloadType getPayloadType() {
        return PayloadType.SERVUX_STRUCTURES;
    }
    @Override
    public void reset(PayloadType type)
    {
        // Don't unregister
        this.register = false;
        //ServuxStructuresPlayListener.INSTANCE.unregisterPlayHandler(type);
        if (this.registered.containsKey(type))
            this.registered.replace(type, false);
        else
            this.registered.put(type, false);
    }
    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworking.Context ctx)
    {
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
        MiniHUD.printDebug("ServuxStructuresPlayListener#receiveS2CPlayPayload(): handling packet via Fabric Network API.");

        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data());
    }
    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworkHandler handler, CallbackInfo ci)
    {
        // Can store the network handler here if wanted
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
        MiniHUD.printDebug("ServuxStructuresPlayListener#receiveS2CPlayPayload(): handling packet via network handler.");

        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data());

        if (ci.isCancellable())
            ci.cancel();
    }


    @Override
    public void decodeS2CNbtCompound(PayloadType type, NbtCompound data)
    {
        // Handle packet.
        int packetType = data.getInt("packetType");
        MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): received packetType: {}, of size in bytes: {}.", packetType, data.getSizeInBytes());

        if (packetType == PacketType.Structures.PACKET_S2C_METADATA)
        {
            MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): received METADATA packet, of size in bytes: {}.", data.getSizeInBytes());
            int version = data.getInt("version");
            //String identifier = data.getString("id");
            if (version == PacketType.Structures.PROTOCOL_VERSION)
            {
                this.timeout = data.getInt("timeout");
                this.register = true;
                DataStorage.getInstance().setIsServuxServer();
                if (data.contains("servux"))
                {
                    String servux = data.getString("servux");
                    DataStorage.getInstance().setServerVersion(servux);
                }
                int x = data.getInt("spawnPosX");
                int y = data.getInt("spawnPosY");
                int z = data.getInt("spawnPosZ");
                BlockPos spawnPos = new BlockPos(x, y, z);
                MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): SpawnPos: {} versus {}", DataStorage.getInstance().getWorldSpawn().toShortString(), spawnPos.toShortString());
                DataStorage.getInstance().setWorldSpawn(spawnPos);
                int radius = data.getInt("spawnChunkRadius");
                MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): SpawnChunkRadius: {} versus {}", DataStorage.getInstance().getSpawnChunkRadius(), radius);
                DataStorage.getInstance().setSpawnChunkRadius(radius);
                MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): register; timeout: {}", this.timeout);

                // Accept / Decline Structure Data based on Render toggle.
                if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue())
                {
                    // Send a list of toggled structures
                    DataStorage.getInstance().updateStructureToggles();
                    // TODO implement this in a usable way under Servux,
                    //  but this is just informing them of our enabled structures
                    //  that we'd like to receive.

                    NbtCompound nbt = new NbtCompound();
                    nbt.putInt("packetType", PacketType.Structures.PACKET_C2S_STRUCTURES_ACCEPT);
                    MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): sending STRUCTURES_ACCEPT packet");
                    encodeC2SNbtCompound(type, nbt);
                }
                // TODO unnecessary logging
                //else
                //{
                    //MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): not sending STRUCTURES_ACCEPT packet");
                //}
            }
            else
            {
                MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): Received invalid Metadata (version: {})", version);
            }
        }
        else if (packetType == PacketType.Structures.PACKET_S2C_SPAWN_METADATA)
        {
            MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): received SPAWN_METADATA packet, of size in bytes: {}.", data.getSizeInBytes());
            if (data.contains("servux"))
            {
                String servux = data.getString("servux");
                DataStorage.getInstance().setServerVersion(servux);
            }
            int x = data.getInt("spawnPosX");
            int y = data.getInt("spawnPosY");
            int z = data.getInt("spawnPosZ");
            BlockPos spawnPos = new BlockPos(x, y, z);
            MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): SpawnPos: {} versus {}", DataStorage.getInstance().getWorldSpawn().toShortString(), spawnPos.toShortString());
            DataStorage.getInstance().setWorldSpawn(spawnPos);
            int radius = data.getInt("spawnChunkRadius");
            MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): SpawnChunkRadius: {} versus {}", DataStorage.getInstance().getSpawnChunkRadius(), radius);
            DataStorage.getInstance().setSpawnChunkRadius(radius);
        }
        else if (packetType == PacketType.Structures.PACKET_S2C_STRUCTURE_DATA)
        {
            // It's safe to accept the data, even if our Structure Renderer is off.
            MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): received STRUCTURE_DATA packet, of size in bytes: {}.", data.getSizeInBytes());
            NbtList structures = data.getList("Structures", Constants.NBT.TAG_COMPOUND);
            MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): structures; list size: {}", structures.size());
            DataStorage.getInstance().addOrUpdateStructuresFromServer(structures, this.timeout, true);
        }
        else
        {
            MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): received unhandled packetType of size {} bytes.", data.getSizeInBytes());
        }
    }

    @Override
    public void encodeC2SNbtCompound(PayloadType type, NbtCompound data)
    {
        // Encode Payload
        ServuxStructuresPayload payload = new ServuxStructuresPayload(data);

        // TODO -- In case you want to use the networkHandler interface to send packets,
        //  instead of the Fabric Networking API, this is how you can do it using this API.
        //ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        //if (handler != null)
        //{
            //ServuxStructuresPlayListener.INSTANCE.sendC2SPlayPayload(type, payload, handler);
        //}
        //else
            ServuxStructuresPlayListener.INSTANCE.sendC2SPlayPayload(type, payload);
    }
    //@Override
    public void sendC2SPlayPayload(PayloadType type, ServuxStructuresPayload payload)
    {
        if (ClientPlayNetworking.canSend(payload.getId()))
        {
            ClientPlayNetworking.send(payload);
        }
        else
            MiniHUD.printDebug("ServuxStructuresPlayListener#sendC2SPlayPayload(): [ERROR] CanSend() is false");
    }
    //@Override
    public void sendC2SPlayPayload(PayloadType type, ServuxStructuresPayload payload, ClientPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadS2CPacket(payload);

        if (handler == null)
        {
            MiniHUD.printDebug("ServuxStructuresPlayListener#sendC2SPlayPayload(): [ERROR] networkHandler = null");
            return;
        }

        if (handler.accepts(packet))
        {
            handler.sendPacket(packet);
        }
        else
            MiniHUD.printDebug("ServuxStructuresPlayListener#sendC2SPlayPayload(): [ERROR] accepts() is false");
    }
    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadTypeRegister.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (!codec.isPlayRegistered())
        {
            MiniHUD.printDebug("ServuxStructuresPlayListener#registerPlayPayload(): received for type {}", type.toString());
            PayloadTypeRegister.getInstance().registerPlayChannel(type, ClientCommonHandlerRegister.getInstance().getPayloadType(type), ClientCommonHandlerRegister.getInstance().getPacketCodec(type));
        }
    }
    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadTypeRegister.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (codec.isPlayRegistered())
        {
            MiniHUD.printDebug("ServuxStructuresPlayListener#registerPlayHandler(): received for type {}", type.toString());

            ClientCommonHandlerRegister.getInstance().registerPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
            if (this.registered.containsKey(type))
                this.registered.replace(type, true);
            else
                this.registered.put(type, true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadTypeRegister.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (codec.isPlayRegistered())
        {
            MiniHUD.printDebug("ServuxStructuresPlayListener#unregisterPlayHandler(): received for type {}", type.toString());

            ClientCommonHandlerRegister.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE);
            if (this.registered.containsKey(type))
                this.registered.replace(type, false);
            else
                this.registered.put(type, false);
        }
    }
}
