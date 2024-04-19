package fi.dy.masa.minihud.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import fi.dy.masa.malilib.network.client.ClientPlayHandler;
import fi.dy.masa.malilib.network.client.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadManager;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
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
            ServuxStructuresHandler.INSTANCE.receiveS2CPlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandler<ServuxStructuresPayload> getInstance() { return INSTANCE; }
    private boolean servuxRegistered;

    @Override
    public PayloadType getPayloadType() {
        return PayloadType.SERVUX_STRUCTURES;
    }

    @Override
    public void decodeS2CNbtCompound(PayloadType type, NbtCompound data)
    {
        int packetType = data.getInt("packetType");

        if (packetType == PacketType.Structures.PACKET_S2C_METADATA)
        {
            if (DataStorage.getInstance().receiveServuxMetadata(data))
            {
                this.servuxRegistered = true;
            }
        }
        else if (packetType == PacketType.Structures.PACKET_S2C_SPAWN_METADATA)
        {
            DataStorage.getInstance().receiveSpawnMetadata(data);
        }
        else if (packetType == PacketType.Structures.PACKET_S2C_STRUCTURE_DATA)
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
    public void reset(PayloadType type)
    {
        if (type.equals(getPayloadType()) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
        }
    }

    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered() == false)
        {
            PayloadManager.getInstance().registerPlayChannel(type, ServuxStructuresPayload.TYPE, ServuxStructuresPayload.CODEC);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            PayloadManager.getInstance().registerPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE, this);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            reset(type);

            PayloadManager.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) ServuxStructuresPayload.TYPE);
        }
    }

    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(P payload, ClientPlayNetworking.Context ctx)
    {
        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.SERVUX_STRUCTURES, ((ServuxStructuresPayload) payload).data());
    }

    @Override
    public void encodeC2SNbtCompound(NbtCompound data)
    {
        ServuxStructuresHandler.INSTANCE.sendC2SPlayPayload(new ServuxStructuresPayload(data));
    }

    @Override
    public <P extends CustomPayload> void sendC2SPlayPayload(P payload)
    {
        if (ClientPlayNetworking.canSend(payload.getId()))
        {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public <P extends CustomPayload> void sendC2SPlayPayload(P payload, ClientPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadS2CPacket(payload);

        if (handler != null && handler.accepts(packet))
        {
            handler.sendPacket(packet);
        }
    }
}
