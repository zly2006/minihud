package fi.dy.masa.minihud.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.util.DataStorage;

@Environment(EnvType.CLIENT)
public abstract class ServuxStructuresHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private final static ServuxStructuresHandler<ServuxStructuresPayload> INSTANCE = new ServuxStructuresHandler<>()
    {
        @Override
        public void receive(ServuxStructuresPayload payload, ClientPlayNetworking.Context context)
        {
            ServuxStructuresHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPayload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = new Identifier("servux", "structures");
    public static final int PROTOCOL_VERSION = 2;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;
    public static final int PACKET_C2S_STRUCTURES_REGISTER = 3;
    public static final int PACKET_C2S_STRUCTURES_UNREGISTER = 4;
    public static final int PACKET_S2C_SPAWN_METADATA = 10;
    public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;
    private boolean servuxRegistered;
    private boolean payloadRegistered = false;

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

    @Override
    public boolean isPlayRegistered(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()))
        {
            return this.payloadRegistered;
        }

        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()))
        {
            this.payloadRegistered = true;
        }
    }

    @Override
    public void decodeNbtCompound(Identifier channel, NbtCompound data)
    {
        int packetType = data.getInt("packetType");

        if (packetType == PACKET_S2C_METADATA)
        {
            if (DataStorage.getInstance().receiveServuxMetadata(data))
            {
                this.servuxRegistered = true;
            }
        }
        else if (packetType == PACKET_S2C_SPAWN_METADATA)
        {
            DataStorage.getInstance().receiveSpawnMetadata(data);
        }
        else if (packetType == PACKET_S2C_STRUCTURE_DATA)
        {
            NbtList structures = data.getList("Structures", Constants.NBT.TAG_COMPOUND);
            DataStorage.getInstance().addOrUpdateStructuresFromServer(structures, this.servuxRegistered);
        }
        else
        {
            MiniHUD.logger.warn("ServuxStructuresHandler#decodeS2CNbtCompound(): received unhandled packetType {} of size {} bytes.", packetType, data.getSizeInBytes());
        }
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(this.getPayloadChannel()) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
        }
    }

    @Override
    public void registerPlayPayload(Identifier channel)
    {
        MiniHUD.logger.error("registerPlayPayload() called for {}", channel.toString());

        if (this.servuxRegistered == false && this.payloadRegistered == false &&
                ClientPlayHandler.getInstance().isClientPlayChannelRegistered(this) == false)
        {
            MiniHUD.logger.error("registerPlayPayload() registering for {}", channel.toString());

            PayloadTypeRegistry.playC2S().register(ServuxStructuresPayload.TYPE, ServuxStructuresPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(ServuxStructuresPayload.TYPE, ServuxStructuresPayload.CODEC);
        }

        this.payloadRegistered = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(Identifier channel)
    {
        MiniHUD.logger.error("registerPlayHandler() called for {}", channel.toString());

        if (channel.equals(this.getPayloadChannel()) && this.payloadRegistered)
        {
            ClientPlayNetworking.registerGlobalReceiver((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
            this.servuxRegistered = true;
        }
    }

    @Override
    public void unregisterPlayHandler(Identifier channel)
    {
        MiniHUD.logger.error("unregisterPlayHandler() called for {}", channel.toString());

        if (channel.equals(this.getPayloadChannel()) && this.payloadRegistered)
        {
            reset(channel);

            ClientPlayNetworking.unregisterGlobalReceiver(ServuxStructuresPayload.TYPE.id());
        }
    }

    @Override
    public <P extends CustomPayload> void receivePlayPayload(P payload, ClientPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()))
        {
            ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeNbtCompound(CHANNEL_ID, ((ServuxStructuresPayload) payload).data());
        }
    }

    @Override
    public void encodeNbtCompound(NbtCompound data)
    {
        ServuxStructuresHandler.INSTANCE.sendPlayPayload(new ServuxStructuresPayload(data));
    }

    @Override
    public <P extends CustomPayload> void sendPlayPayload(P payload)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()) && this.payloadRegistered &&
            ClientPlayNetworking.canSend(payload.getId()))
        {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public <P extends CustomPayload> void sendPlayPayload(P payload, ClientPlayNetworkHandler handler)
    {
        if (payload.getId().id().equals(this.getPayloadChannel()) && this.payloadRegistered)
        {
            Packet<?> packet = new CustomPayloadS2CPacket(payload);

            if (handler != null && handler.accepts(packet))
            {
                handler.sendPacket(packet);
            }
        }
    }
}
