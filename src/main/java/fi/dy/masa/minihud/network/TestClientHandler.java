package fi.dy.masa.minihud.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import fi.dy.masa.malilib.network.handler.client.ClientPlayHandler;
import fi.dy.masa.malilib.network.handler.client.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadManager;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.channel.MaLiLibTestPayload;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;

public abstract class TestClientHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private final static TestClientHandler<MaLiLibTestPayload> INSTANCE = new TestClientHandler<>()
    {
        @Override
        public void receive(MaLiLibTestPayload payload, ClientPlayNetworking.Context context)
        {
            TestClientHandler.INSTANCE.receiveS2CPlayPayload(PayloadType.MALILIB_TEST, payload, context);
        }
    };
    public static TestClientHandler<MaLiLibTestPayload> getInstance() { return INSTANCE; }
    private boolean testRegistered;

    @Override
    public PayloadType getPayloadType() { return PayloadType.MALILIB_TEST; }

    @Override
    public void decodeS2CNbtCompound(PayloadType type, NbtCompound data)
    {
        MiniHUD.printDebug("TestClientHandler#decodeS2CNbtCompound(): received data of size {} bytes", data.getSizeInBytes());
        String test = data.getString("message");

        MiniHUD.logger.error("test message: {}", test);

        NbtCompound nbt = new NbtCompound();
        nbt.putString("message", "packet reply from "+ Reference.MOD_STRING);
        this.encodeC2SNbtCompound(type, nbt);
    }

    @Override
    public void reset(PayloadType type)
    {
        if (type.equals(getPayloadType()) && this.testRegistered)
        {
            this.testRegistered = false;
        }
    }

    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered() == false)
        {
            PayloadManager.getInstance().registerPlayChannel(getPayloadType(), MaLiLibTestPayload.TYPE, MaLiLibTestPayload.CODEC);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            PayloadManager.getInstance().registerPlayHandler((CustomPayload.Id<T>) MaLiLibTestPayload.TYPE, this);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadManager.getInstance().getPayloadCodec(type);

        if (codec != null && codec.isPlayRegistered())
        {
            PayloadManager.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) MaLiLibTestPayload.TYPE);
        }
    }

    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworking.Context ctx)
    {
        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(this.getPayloadType(), ((MaLiLibTestPayload) payload).data());
    }

    @Override
    public void encodeC2SNbtCompound(PayloadType type, NbtCompound data)
    {
        MaLiLibTestPayload payload = new MaLiLibTestPayload(data);

        this.sendC2SPlayPayload(this.getPayloadType(), payload);
    }

    @Override
    public <P extends CustomPayload> void sendC2SPlayPayload(PayloadType type, P payload)
    {
        if (ClientPlayNetworking.canSend(payload.getId()))
        {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public <P extends CustomPayload> void sendC2SPlayPayload(PayloadType type, P payload, ClientPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadC2SPacket(payload);

        if (handler != null && handler.accepts(packet))
        {
            handler.sendPacket(packet);
        }
    }
}
