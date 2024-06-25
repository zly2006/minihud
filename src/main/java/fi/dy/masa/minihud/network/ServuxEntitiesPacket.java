package fi.dy.masa.minihud.network;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.minihud.MiniHUD;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ServuxEntitiesPacket implements IClientPayloadData
{
    private Type packetType;
    private int transactionId;
    private int entityId;
    private BlockPos pos;
    private NbtCompound nbt;
    private PacketByteBuf buffer;
    public static final int PROTOCOL_VERSION = 1;

    private ServuxEntitiesPacket(Type type)
    {
        this.packetType = type;
        this.transactionId = -1;
        this.entityId = -1;
        this.pos = BlockPos.ORIGIN;
        this.nbt = new NbtCompound();
        this.clearPacket();
    }

    public static ServuxEntitiesPacket MetadataRequest(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_METADATA_REQUEST);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    public static ServuxEntitiesPacket MetadataResponse(@Nullable NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_METADATA);
        if (nbt != null)
        {
            packet.nbt.copyFrom(nbt);
        }
        return packet;
    }

    // Entity simple response
    public static ServuxEntitiesPacket SimpleEntityResponse(int entityId, NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE);
        packet.nbt = nbt.copy();
        packet.entityId = entityId;
        return packet;
    }

    public static ServuxEntitiesPacket SimpleBlockResponse(BlockPos pos, NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE);
        packet.nbt = nbt.copy();
        packet.pos = pos.toImmutable();
        return packet;
    }

    public static ServuxEntitiesPacket BlockEntityRequest(BlockPos pos)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_BLOCK_ENTITY_REQUEST);
        packet.pos = pos.toImmutable();
        return packet;
    }

    public static ServuxEntitiesPacket EntityRequest(int entityId)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_C2S_ENTITY_REQUEST);
        packet.entityId = entityId;
        return packet;
    }

    // Response Nbt Packet, using Packet Splitter
    public static ServuxEntitiesPacket ResponseStart(@Nonnull NbtCompound nbt)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_NBT_RESPONSE_START);
        packet.nbt.copyFrom(nbt);
        return packet;
    }

    // Response Packet Slice (Packet Splitter)
    public static ServuxEntitiesPacket ResponseData(@Nonnull PacketByteBuf buffer)
    {
        var packet = new ServuxEntitiesPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA);
        packet.buffer = buffer;
        packet.nbt = new NbtCompound();
        return packet;
    }

    private void clearPacket()
    {
        if (this.buffer != null)
        {
            this.buffer.clear();
            this.buffer = new PacketByteBuf(Unpooled.buffer());
        }
    }

    @Override
    public int getVersion()
    {
        return PROTOCOL_VERSION;
    }

    @Override
    public int getPacketType()
    {
        return this.packetType.get();
    }

    @Override
    public int getTotalSize()
    {
        int total = 2;

        if (this.nbt != null && this.nbt.isEmpty() == false)
        {
            total += this.nbt.getSizeInBytes();
        }
        if (this.buffer != null)
        {
            total += this.buffer.readableBytes();
        }

        return total;
    }

    public Type getType()
    {
        return this.packetType;
    }

    public int getTransactionId() { return this.transactionId; }

    public int getEntityId() { return this.entityId; }

    public BlockPos getPos() { return this.pos; }

    public NbtCompound getCompound()
    {
        return this.nbt;
    }

    public PacketByteBuf getBuffer()
    {
        return this.buffer;
    }

    public boolean hasBuffer() { return this.buffer != null && this.buffer.isReadable(); }

    public boolean hasNbt() { return this.nbt != null && !this.nbt.isEmpty(); }

    @Override
    public boolean isEmpty()
    {
        return !this.hasBuffer() && !this.hasNbt();
    }

    @Override
    public void toPacket(PacketByteBuf output)
    {
        output.writeVarInt(this.packetType.get());

        switch (this.packetType)
        {
            case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
            {
                // Write BE Request
                try
                {
                    output.writeVarInt(this.transactionId);
                    output.writeBlockPos(this.pos);
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#toPacket: error writing Block Entity Request to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_ENTITY_REQUEST ->
            {
                // Write Entity Request
                try
                {
                    output.writeVarInt(this.transactionId);
                    output.writeVarInt(this.entityId);
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#toPacket: error writing Entity Request to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
            {
                try
                {
                    output.writeBlockPos(this.pos);
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#toPacket: error writing Block Entity Response to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
            {
                try
                {
                    output.writeVarInt(this.entityId);
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#toPacket: error writing Entity Response to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Write Packet Buffer (Slice)
                try
                {
                    output.writeBytes(this.buffer.readBytes(this.buffer.readableBytes()));
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST, PACKET_S2C_METADATA ->
            {
                // Write NBT
                try
                {
                    output.writeNbt(this.nbt);
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
                }
            }
            default ->
            {
                MiniHUD.logger.error("ServuxEntitiesPacket#toPacket: Unknown packet type!");
            }
        }
    }

    @Nullable
    public static ServuxEntitiesPacket fromPacket(PacketByteBuf input)
    {
        int i = input.readVarInt();
        Type type = getType(i);

        if (type == null)
        {
            // Invalid Type
            MiniHUD.logger.warn("ServuxEntitiesPacket#fromPacket: invalid packet type received");
            return null;
        }
        switch (type)
        {
            case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
            {
                // Read Packet Buffer
                try
                {
                    input.readVarInt(); // todo: old code compat
                    return ServuxEntitiesPacket.BlockEntityRequest(input.readBlockPos());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#fromPacket: error reading Block Entity Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_ENTITY_REQUEST ->
            {
                // Read Packet Buffer
                try
                {
                    input.readVarInt(); // todo: old code compat
                    return ServuxEntitiesPacket.EntityRequest(input.readVarInt());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#fromPacket: error reading Entity Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_NBT_RESPONSE_DATA ->
            {
                // Read Packet Buffer Slice
                try
                {
                    return ServuxEntitiesPacket.ResponseData(new PacketByteBuf(input.readBytes(input.readableBytes())));
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#fromPacket: error reading Block Response Buffer from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_C2S_METADATA_REQUEST ->
            {
                // Read Nbt
                try
                {
                    return ServuxEntitiesPacket.MetadataRequest(input.readNbt());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#fromPacket: error reading Metadata Request from packet: [{}]", e.getLocalizedMessage());
                }
            }
            case PACKET_S2C_METADATA ->
            {
                // Read Nbt
                try
                {
                    return ServuxEntitiesPacket.MetadataResponse(input.readNbt());
                }
                catch (Exception e)
                {
                    MiniHUD.logger.error("ServuxEntitiesPacket#fromPacket: error reading Metadata Response from packet: [{}]", e.getLocalizedMessage());
                }
            }
            default ->
            {
                MiniHUD.logger.error("ServuxEntitiesPacket#fromPacket: Unknown packet type!");
            }
        }

        return null;
    }

    @Override
    public void clear()
    {
        if (this.nbt != null && this.nbt.isEmpty() == false)
        {
            this.nbt = new NbtCompound();
        }
        this.clearPacket();
        this.transactionId = -1;
        this.entityId = -1;
        this.pos = BlockPos.ORIGIN;
        this.packetType = null;
    }

    @Nullable
    public static Type getType(int input)
    {
        for (Type type : Type.values())
        {
            if (type.get() == input)
            {
                return type;
            }
        }

        return null;
    }

    public enum Type
    {
        PACKET_S2C_METADATA(1),
        PACKET_C2S_METADATA_REQUEST(2),
        PACKET_C2S_BLOCK_ENTITY_REQUEST(3),
        PACKET_C2S_ENTITY_REQUEST(4),
        PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE(5),
        PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE(6),
        // For Packet Splitter (Oversize Packets)
        PACKET_S2C_NBT_RESPONSE_START(10),
        PACKET_S2C_NBT_RESPONSE_DATA(11);

        private final int type;

        Type(int type)
        {
            this.type = type;
        }

        int get() { return this.type; }
    }

    public record Payload(ServuxEntitiesPacket data) implements CustomPayload
    {
        public static final Id<ServuxEntitiesPacket.Payload> ID = new Id<>(ServuxEntitiesHandler.CHANNEL_ID);
        public static final PacketCodec<PacketByteBuf, ServuxEntitiesPacket.Payload> CODEC = CustomPayload.codecOf(ServuxEntitiesPacket.Payload::write, ServuxEntitiesPacket.Payload::new);

        public Payload(PacketByteBuf input)
        {
            this(fromPacket(input));
        }

        private void write(PacketByteBuf output)
        {
            data.toPacket(output);
        }

        @Override
        public Id<? extends CustomPayload> getId()
        {
            return ID;
        }
    }
}
