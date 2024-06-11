package fi.dy.masa.minihud.network.test;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.ApiStatus;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PayloadSplitter;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.util.DataStorage;

@Environment(EnvType.CLIENT)
@ApiStatus.Experimental
public abstract class ServuxStructuresHandlerTest<T extends CustomPayload> implements IPluginClientPlayHandler<T>
{
    private final static ServuxStructuresHandlerTest<ServuxStructuresPayloadTest> INSTANCE = new ServuxStructuresHandlerTest<>()
    {
        @Override
        public void receive(ServuxStructuresPayloadTest payload, ClientPlayNetworking.Context context)
        {
            ServuxStructuresHandlerTest.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxStructuresHandlerTest<ServuxStructuresPayloadTest> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.of("servux", "structures-test");

    private boolean servuxRegistered;
    private boolean payloadRegistered = false;
    private int failures = 0;
    private static final int MAX_FAILURES = 4;

    public static final int PROTOCOL_VERSION = 2;
    public static final int PACKET_S2C_METADATA = 1;
    public static final int PACKET_S2C_STRUCTURE_DATA = 2;
    public static final int PACKET_C2S_STRUCTURES_REGISTER = 3;
    public static final int PACKET_C2S_STRUCTURES_UNREGISTER = 4;
    public static final int PACKET_S2C_STRUCTURE_DATA_START = 5;
    public static final int PACKET_S2C_STRUCTURE_DATA_END = 6;
    public static final int PACKET_S2C_SPAWN_METADATA = 10;
    public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;

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

    public void decodeStructuresData(Identifier channel, ServuxStructuresDataTest data, ClientPlayNetworkHandler networkHandler)
    {
        //MiniHUD.logger.warn("decodeStructuresData():[TEST] received packet of packetType {} // size in bytes [{}]", data.getPacketType(), data.getTotalSize());

        switch (data.getPacketType())
        {
            case PACKET_S2C_STRUCTURE_DATA ->
            {
                MiniHUD.logger.warn("decodeStructuresData():[TEST] received structures packet of size [{}]", data.getTotalSize());

                PacketByteBuf fullPacket = PayloadSplitter.receive(this, data.getPacket(), networkHandler);

                if (fullPacket != null)
                {
                    //MiniHUD.logger.error("decodeStructuresData():[TEST] fullPacket Received");
                    try
                    {
                        //int varInt = fullPacket.readVarInt();
                        NbtCompound nbt = fullPacket.readNbt();

                        if (nbt != null)
                        {
                            NbtList structures = nbt.getList("Structures", Constants.NBT.TAG_COMPOUND);
                            MiniHUD.printDebug("decodeStructuresData():[TEST] received Structures Data payload of size {} (in bytes)", nbt.getSizeInBytes());
                            MiniHUD.logger.error("decodeStructuresData():[TEST] total list size {}", structures.size());
                        }
                        else
                        {
                            MiniHUD.logger.error("decodeStructuresData():[TEST] error reading fullBuffer NBT is NULL");
                        }
                    }
                    catch (Exception e)
                    {
                        MiniHUD.logger.error("decodeStructuresData():[TEST] error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }

                    //DataStorage.getInstance().addOrUpdateStructuresFromServer(structures, this.servuxRegistered);
                }
            }
            case PACKET_S2C_STRUCTURE_DATA_END ->
            {
                MiniHUD.logger.warn("decodeStructuresData():[TEST] Structures End Received");
            }
            case PACKET_S2C_METADATA ->
            {
                MiniHUD.logger.warn("decodeStructuresData():[TEST] Metadata Received");
                /*
                if (DataStorage.getInstance().receiveServuxMetadata(data.getCompound()))
                {
                    this.servuxRegistered = true;
                }
                 */
            }
            case PACKET_S2C_SPAWN_METADATA ->
            {
                MiniHUD.logger.warn("decodeStructuresData():[TEST] Spawn Metadata Received");

                //DataStorage.getInstance().receiveSpawnMetadata(data.getCompound());
            }
            default ->
            {
                MiniHUD.logger.warn("decodeStructuresData():[TEST] received unhandled packetType {} of size {} bytes.", data.getPacketType(), data.getTotalSize());
            }
        }
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.servuxRegistered)
        {
            MiniHUD.printDebug("[TEST] reset() called for {}", channel.toString());

            this.servuxRegistered = false;
            this.failures = 0;
        }
    }

    public void resetFailures(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.failures > 0)
        {
            MiniHUD.printDebug("[TEST] resetFailures() called for {}", channel.toString());
            this.failures = 0;
        }
    }

    @Override
    public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx)
    {
        if (payload.getId().id().equals(CHANNEL_ID))
        {
            ServuxStructuresHandlerTest.INSTANCE.decodeStructuresData(CHANNEL_ID, ((ServuxStructuresPayloadTest) payload).data(), ctx.player().networkHandler);
        }
    }

    @Override
    public void encodeWithSplitter(PacketByteBuf buf, ClientPlayNetworkHandler handler)
    {
        ServuxStructuresHandlerTest.INSTANCE.encodeStructuresData(new ServuxStructuresDataTest(PACKET_S2C_STRUCTURE_DATA, buf));
    }

    public void encodeStructuresData(ServuxStructuresDataTest data)
    {
        /*
        if (data.getPacketType() == PACKET_S2C_STRUCTURE_DATA_START)
        {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeNbt(data.getCompound());

            if (PayloadSplitter.send(this, this.getPayloadChannel(), buf))
            {
                MiniHUD.logger.error("encodeStructuresData(): Finished splitting structures packet");
                ServuxStructuresHandlerTest.INSTANCE.encodeStructuresData(new ServuxStructuresDataTest(PACKET_S2C_STRUCTURE_DATA_END, new NbtCompound()));
            }
        }
         */
        if (ServuxStructuresHandlerTest.INSTANCE.sendPlayPayload(new ServuxStructuresPayloadTest(data)) == false)
        {
            if (this.failures > MAX_FAILURES)
            {
                MiniHUD.logger.warn("encodeStructuresData:[TEST] encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", MAX_FAILURES);
                this.servuxRegistered = false;
                ServuxStructuresHandlerTest.INSTANCE.unregisterPlayReceiver();
                DataStorage.getInstance().onPacketFailure();
            }
            else
            {
                this.failures++;
            }
        }
    }
}
