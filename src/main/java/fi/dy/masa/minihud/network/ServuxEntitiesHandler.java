package fi.dy.masa.minihud.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.EntitiesDataStorage;

@Environment(EnvType.CLIENT)
public abstract class ServuxEntitiesHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> INSTANCE = new ServuxEntitiesHandler<>()
    {
        @Override
        public void receive(ServuxEntitiesPacket.Payload payload, ClientPlayNetworking.Context context)
        {
            ServuxEntitiesHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "entity_data");

    private boolean servuxRegistered;
    private boolean payloadRegistered = false;
    private int failures = 0;
    private static final int MAX_FAILURES = 4;
    private long readingSessionKey = -1;

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

    @Override
    public boolean isPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            return this.payloadRegistered;
        }

        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            this.payloadRegistered = true;
        }
    }

    @Override
    public <P extends IClientPayloadData> void decodeClientData(Identifier channel, P data)
    {
        ServuxEntitiesPacket packet = (ServuxEntitiesPacket) data;

        if (!channel.equals(CHANNEL_ID))
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_S2C_METADATA ->
            {
                if (EntitiesDataStorage.getInstance().receiveServuxMetadata(packet.getCompound()))
                {
                    this.servuxRegistered = true;
                }
            }
            case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE -> EntitiesDataStorage.getInstance().handleBlockEntityData(packet.getPos(), packet.getCompound(), null);
            case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE -> EntitiesDataStorage.getInstance().handleEntityData(packet.getEntityId(), packet.getCompound());
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                if (this.readingSessionKey == -1)
                {
                    this.readingSessionKey = Random.create(Util.getMeasuringTimeMs()).nextLong();
                }

                //MiniHUD.printDebug("ServuxEntitiesHandler#decodeClientData(): received Entity Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), this.readingSessionKey);
                PacketByteBuf fullPacket = PacketSplitter.receive(this, this.readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    try
                    {
                        this.readingSessionKey = -1;
                        EntitiesDataStorage.getInstance().handleBulkEntityData(fullPacket.readVarInt(), (NbtCompound) fullPacket.readNbt(NbtSizeTracker.ofUnlimitedBytes()));
                    }
                    catch (Exception e)
                    {
                        MiniHUD.logger.error("ServuxEntitiesHandler#decodeClientData(): Entity Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
            default -> MiniHUD.logger.warn("ServuxEntitiesHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
        }
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
            this.failures = 0;
            this.readingSessionKey = -1;
        }
    }

    public void resetFailures(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.failures > 0)
        {
            this.failures = 0;
        }
    }

    @Override
    public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(CHANNEL_ID))
        {
            ServuxEntitiesHandler.INSTANCE.decodeClientData(CHANNEL_ID, ((ServuxEntitiesPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(PacketByteBuf buffer, ClientPlayNetworkHandler handler)
    {
        // Send each PacketSplitter buffer slice
        ServuxEntitiesHandler.INSTANCE.sendPlayPayload(new ServuxEntitiesPacket.Payload(ServuxEntitiesPacket.ResponseC2SData(buffer)));
    }

    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data)
    {
        ServuxEntitiesPacket packet = (ServuxEntitiesPacket) data;

        if (packet.getType().equals(ServuxEntitiesPacket.Type.PACKET_C2S_NBT_RESPONSE_START))
        {
            PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
            buffer.writeVarInt(packet.getTransactionId());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, MinecraftClient.getInstance().getNetworkHandler());
        }
        else if (!ServuxEntitiesHandler.INSTANCE.sendPlayPayload(new ServuxEntitiesPacket.Payload(packet)))
        {
            if (this.failures > MAX_FAILURES)
            {
                MiniHUD.printDebug("encodeClientData(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", MAX_FAILURES);
                this.servuxRegistered = false;
                ServuxEntitiesHandler.INSTANCE.unregisterPlayReceiver();
                EntitiesDataStorage.getInstance().onPacketFailure();
            }
            else
            {
                this.failures++;
            }
        }
    }
}
