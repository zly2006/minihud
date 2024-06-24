package fi.dy.masa.minihud.data;

import javax.annotation.Nullable;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class BlockEntitiyData
{
    //BlockEntityUpdateS2CPacket
    private BlockPos pos;
    private BlockEntityType<?> blockType;
    private NbtCompound nbt;

    public BlockEntitiyData(BlockPos pos, BlockEntityType<?> type, @Nullable NbtCompound nbt)
    {
        this.pos = pos;
        this.blockType = type;
        if (nbt == null)
        {
            this.nbt = new NbtCompound();
        }
        else
        {
            this.nbt = new NbtCompound();
            this.nbt.copyFrom(nbt);
        }
    }

    public BlockPos getPos() { return this.pos; }

    public BlockEntityType<?> getType() { return this.blockType; }

    @Nullable
    public NbtCompound getNbt() { return this.nbt; }

    public void updatePos(BlockPos pos) { this.pos = pos; }

    public void updateNbt(NbtCompound nbt)
    {
        if (nbt.isEmpty() == false)
        {
            this.nbt.copyFrom(nbt);
        }
        // Don't clear old data
    }
}
