package fi.dy.masa.minihud.network.handlers;

import fi.dy.masa.malilib.network.handler.ClientCommonHandlerRegister;
import fi.dy.masa.malilib.network.handler.ClientPlayHandler;
import fi.dy.masa.malilib.network.handler.IPluginPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.PayloadTypeRegister;
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
import fi.dy.masa.malilib.network.test.ClientDebugSuite;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.DataStorage;
import fi.dy.masa.minihud.network.PacketType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public abstract class ServuxStructuresPlayListener<T extends CustomPayload> implements IPluginPlayHandler<T>
{
    public final static ServuxStructuresPlayListener<ServuxStructuresPayload> INSTANCE = new ServuxStructuresPlayListener<>() {
        @Override
        public void receive(ServuxStructuresPayload payload, ClientPlayNetworking.Context context) {
            //MiniHUD.printDebug("ServuxStructuresPlayListener#receive(): received Servux Structures payload.");
            //((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).receiveS2CPlayPayload(PayloadType.SERVUX_STRUCTURES, payload, context);
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
        unregisterPlayHandler(type);
        if (this.registered.containsKey(type))
            this.registered.replace(type, false);
        else
            this.registered.put(type, false);
    }
    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworking.Context ctx)
    {
        //IPluginPlayHandler.super.receiveS2CPlayPayload(type, payload, ctx);
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;
        MiniHUD.printDebug("ServuxStructuresPlayListener#receiveS2CPlayPayload(): received a Servux Structures payload.");
        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data());
    }

    @Override
    public void decodeS2CNbtCompound(PayloadType type, NbtCompound data)
    {
        //IPluginPlayHandler.super.decodeS2CNbtCompound(type, data);

        MiniHUD.printDebug("ServuxStructuresPlayListener#decodeS2CNbtCompound(): decoding Servux Structures packet...");

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
                    NbtCompound nbt = new NbtCompound();
                    nbt.putInt("packetType", PacketType.Structures.PACKET_C2S_STRUCTURES_ACCEPT);
                    MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): sending STRUCTURES_ACCEPT packet");
                    encodeC2SNbtCompound(type, nbt);
                }
                else
                {
                    MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): not sending STRUCTURES_ACCEPT packet");
                }
            }
            else
            {
                MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): Received invalid Metadata (version: {})", version);
            }
        }
        else if (packetType == PacketType.Structures.PACKET_S2C_SPAWN_METADATA)
        {
            MiniHUD.printDebug("ServuxStructuresListener#decodeServuxStructures(): received SPAWN_METADATA packet, of size in bytes: {}.", data.getSizeInBytes());
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
        //IPluginPlayHandler.super.encodeC2SNbtCompound(type, data);

        MiniHUD.printDebug("ServuxStructuresPlayListener#encodeC2SNbtCompound(): data.putByteArray() size in bytes: {}", data.getSizeInBytes());

        // Encode Payload
        ServuxStructuresPayload payload = new ServuxStructuresPayload(data);
        sendC2SPlayPayload(type, payload);
    }
    //@Override
    public void sendC2SPlayPayload(PayloadType type, ServuxStructuresPayload payload)
    {
        //IPluginPlayHandler.super.sendC2SPlayPayload(type, payload);
        MiniHUD.printDebug("ServuxStructuresPlayListener#sendC2SPlayPayload(): sending Servux Structures packet.");

        if (ClientPlayNetworking.canSend(payload.getId()))
        {
            MiniHUD.printDebug("ServuxStructuresPlayListener#sendC2SPlayPayload(): canSend = true;");
            ClientPlayNetworking.send(payload);
        }
        else
            MiniHUD.printDebug("ServuxStructuresPlayListener#sendC2SPlayPayload(): canSend = false;");
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
            PayloadTypeRegister.getInstance().registerPlayChannel(type, ClientCommonHandlerRegister.getInstance().getPayloadType(type), ClientCommonHandlerRegister.getInstance().getPacketCodec(type));
        }
        ClientDebugSuite.checkGlobalPlayChannels();
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
        ClientDebugSuite.checkGlobalPlayChannels();
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
            //PayloadTypeRegister.getInstance().registerPlayChannel(type, ClientCommonHandlerRegister.getInstance().getPayload(type), ClientCommonHandlerRegister.getInstance().getPacketCodec(type));
            ClientCommonHandlerRegister.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE);
            if (this.registered.containsKey(type))
                this.registered.replace(type, false);
            else
                this.registered.put(type, false);
        }
    }
}
