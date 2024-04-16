package fi.dy.masa.minihud.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.malilib.network.handler.CommonHandlerRegister;
import fi.dy.masa.malilib.network.handler.client.ClientPlayHandler;
import fi.dy.masa.malilib.network.handler.client.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadManager;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.DataStorage;

@Environment(EnvType.CLIENT)
public abstract class ServuxStructuresHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private final static ServuxStructuresHandler<ServuxStructuresPayload> INSTANCE = new ServuxStructuresHandler<>()
    {
        @Override
        public void receive(ServuxStructuresPayload payload, ClientPlayNetworking.Context context)
        {
            ServuxStructuresHandler.INSTANCE.receiveS2CPlayPayload(PayloadType.SERVUX_STRUCTURES, payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPayload> getInstance() { return INSTANCE; }

    private boolean servuxRegistered;
    private boolean register;
    private int timeout;

    @Override
    public PayloadType getPayloadType() {
        return PayloadType.SERVUX_STRUCTURES;
    }

    public void setRegister(boolean toggle)
    {
        this.register = toggle;
    }

    @Override
    public void reset(PayloadType type)
    {
        if (type.equals(getPayloadType()) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
            this.timeout = -1;
        }
        this.register = false;
    }

    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworking.Context ctx)
    {
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;

        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data());
    }

    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworkHandler handler, CallbackInfo ci)
    {
        ServuxStructuresPayload packet = (ServuxStructuresPayload) payload;

        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, packet.data());

        if (ci.isCancellable())
        {
            ci.cancel();
        }
    }

    @Override
    public void decodeS2CNbtCompound(PayloadType type, NbtCompound data)
    {
        int packetType = data.getInt("packetType");

        if (packetType == PacketType.Structures.PACKET_S2C_METADATA)
        {
            int version = data.getInt("version");

            if (version == PacketType.Structures.PROTOCOL_VERSION)
            {
                MiniHUD.printDebug("ServuxStructuresHandler#decodeS2CNbtCompound(): received \"{}\" METADATA packet Version {}, of size in bytes: {}.", data.getString("name"), version, data.getSizeInBytes());

                this.timeout = data.getInt("timeout");
                DataStorage.getInstance().setServerVersion(data.getString("servux"));
                DataStorage.getInstance().setWorldSpawn(new BlockPos(data.getInt("spawnPosX"), data.getInt("spawnPosY"), data.getInt("spawnPosZ")));
                DataStorage.getInstance().setSpawnChunkRadius(data.getInt("spawnChunkRadius"));

                if (RendererToggle.OVERLAY_STRUCTURE_MAIN_TOGGLE.getBooleanValue() && this.register)
                {
                    this.servuxRegistered = true;
                    DataStorage.getInstance().setIsServuxServer();

                    MiniHUD.logger.info("ServuxStructuresHandler: accepting structures from server version {}", data.getString("servux"));
                    /*
                    NbtCompound nbt = new NbtCompound();
                    nbt.putInt("packetType", PacketType.Structures.PACKET_C2S_STRUCTURES_ACCEPT);
                    encodeC2SNbtCompound(type, nbt);
                     */
                }
            }
            else
            {
                MiniHUD.logger.warn("ServuxStructuresHandler#decodeS2CNbtCompound(): Received invalid Structures Metadata (version: {})", version);
            }
        }
        else if (packetType == PacketType.Structures.PACKET_S2C_SPAWN_METADATA)
        {
            MiniHUD.printDebug("ServuxStructuresHandler#decodeS2CNbtCompound(): received SPAWN_METADATA packet, of size in bytes: {}.", data.getSizeInBytes());

            DataStorage.getInstance().setServerVersion(data.getString("servux"));
            DataStorage.getInstance().setWorldSpawn(new BlockPos(data.getInt("spawnPosX"), data.getInt("spawnPosY"), data.getInt("spawnPosZ")));
            DataStorage.getInstance().setSpawnChunkRadius(data.getInt("spawnChunkRadius"));
        }
        else if (packetType == PacketType.Structures.PACKET_S2C_STRUCTURE_DATA)
        {
            MiniHUD.printDebug("ServuxStructuresHandler#decodeS2CNbtCompound(): received STRUCTURE_DATA packet, of size in bytes: {}.", data.getSizeInBytes());

            NbtList structures = data.getList("Structures", Constants.NBT.TAG_COMPOUND);
            DataStorage.getInstance().addOrUpdateStructuresFromServer(structures, this.timeout, true);
        }
        else
        {
            MiniHUD.logger.warn("ServuxStructuresHandler#decodeS2CNbtCompound(): received unhandled packetType {} of size {} bytes.", packetType, data.getSizeInBytes());
        }
    }

    @Override
    public void encodeC2SNbtCompound(PayloadType type, NbtCompound data)
    {
        ServuxStructuresPayload payload = new ServuxStructuresPayload(data);

        ServuxStructuresHandler.INSTANCE.sendC2SPlayPayload(type, payload);        // Fabric API method
    }

    public void sendC2SPlayPayload(PayloadType type, ServuxStructuresPayload payload)
    {
        if (ClientPlayNetworking.canSend(payload.getId()))
        {
            ClientPlayNetworking.send(payload);
        }
        else
        {
            MiniHUD.logger.error("ServuxStructuresHandler#sendS2CPlayPayload(): [API].canSend() is FALSE type: {}", type.toString());
        }
    }

    public void sendC2SPlayPayload(PayloadType type, ServuxStructuresPayload payload, ClientPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadS2CPacket(payload);

        if (handler != null && handler.accepts(packet))
        {
            handler.sendPacket(packet);
        }
        else
        {
            MiniHUD.logger.error("ServuxStructuresHandler#sendS2CPlayPayload(): [Handler].accepts() is FALSE type: {}", type.toString());
        }
    }

    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered() == false)
        {
            PayloadManager.getInstance().registerPlayChannel(type, CommonHandlerRegister.getInstance().getPayloadType(type), CommonHandlerRegister.getInstance().getPacketCodec(type));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            CommonHandlerRegister.getInstance().registerPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
            this.servuxRegistered = true;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            reset(getPayloadType());

            CommonHandlerRegister.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE);
        }
    }
}
