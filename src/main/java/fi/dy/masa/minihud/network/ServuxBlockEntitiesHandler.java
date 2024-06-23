package fi.dy.masa.minihud.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.BlockEntitiesData;

@Environment(EnvType.CLIENT)
public abstract class ServuxBlockEntitiesHandler<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private final static ServuxBlockEntitiesHandler<ServuxBlockEntitiesPacket.Payload> INSTANCE = new ServuxBlockEntitiesHandler<>()
    {
        @Override
        public void receive(ServuxBlockEntitiesPacket.Payload payload, ClientPlayNetworking.Context context)
        {
            ServuxBlockEntitiesHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxBlockEntitiesHandler<ServuxBlockEntitiesPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "block_entities");

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
        ServuxBlockEntitiesPacket packet = (ServuxBlockEntitiesPacket) data;

        if (channel.equals(CHANNEL_ID) == false)
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_S2C_BLOCK_ENTITY_DATA ->
            {
                if (this.readingSessionKey == -1)
                {
                    this.readingSessionKey = Random.create(Util.getMeasuringTimeMs()).nextLong();
                }

                PacketByteBuf fullPacket = PacketSplitter.receive(this, this.readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    try
                    {
                        NbtCompound nbt = fullPacket.readNbt();
                        this.readingSessionKey = -1;

                        if (nbt != null)
                        {
                            //NbtList structures = nbt.getList("Structures", Constants.NBT.TAG_COMPOUND);
                            MiniHUD.printDebug("ServuxBlockEntitiesHandler#decodeClientData(): received Block Entity Data of size {} (in bytes)", nbt.getSizeInBytes());

                            //BlockEntitiesData.getInstance().addOrUpdateStructuresFromServer(structures, this.servuxRegistered);
                        }
                        else
                        {
                            MiniHUD.logger.warn("ServuxBlockEntitiesHandler#decodeClientData(): Block Entity Data: error reading fullBuffer NBT is NULL");
                        }
                    }
                    catch (Exception e)
                    {
                        MiniHUD.logger.error("ServuxBlockEntitiesHandler#decodeClientData(): Block Entity Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
            case PACKET_S2C_METADATA ->
            {
                if (BlockEntitiesData.getInstance().receiveServuxMetadata(packet.getCompound()))
                {
                    this.servuxRegistered = true;
                }
            }
            case PACKET_S2C_BLOCK_ENTITY_REQUEST_DENIED ->
            {
                /*
                if (BlockEntitiesData.getInstance().receiveServuxMetadata(packet.getCompound()))
                {
                    this.servuxRegistered = true;
                }
                 */
            }
            default -> MiniHUD.logger.warn("ServuxBlockEntitiesHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
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
            //ServuxBlockEntitiesHandler.INSTANCE.decodeClientData(CHANNEL_ID, ((ServuxBlockEntitiesPacket.Payload) payload).data());
            ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeClientData(CHANNEL_ID, ((ServuxBlockEntitiesPacket.Payload) payload).data());
            // This allows the data to be "shared" among multiple mods
        }
    }

    @Override
    public void encodeWithSplitter(PacketByteBuf buffer, ClientPlayNetworkHandler handler)
    {
        // NO-OP
    }

    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data)
    {
        ServuxBlockEntitiesPacket packet = (ServuxBlockEntitiesPacket) data;

        if (ServuxBlockEntitiesHandler.INSTANCE.sendPlayPayload(new ServuxBlockEntitiesPacket.Payload(packet)) == false)
        {
            if (this.failures > MAX_FAILURES)
            {
                MiniHUD.logger.warn("encodeClientData(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", MAX_FAILURES);
                this.servuxRegistered = false;
                ServuxBlockEntitiesHandler.INSTANCE.unregisterPlayReceiver();
                BlockEntitiesData.getInstance().onPacketFailure();
            }
            else
            {
                this.failures++;
            }
        }
    }
}
