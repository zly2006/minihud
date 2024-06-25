package fi.dy.masa.minihud.util;

import fi.dy.masa.minihud.mixin.IMixinEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;

public class EntityUtils
{
    // entity.readNbt(nbt);
    public static void loadNbtIntoEntity(Entity entity, NbtCompound nbt)
    {
        if (nbt.contains("CustomName", NbtElement.STRING_TYPE)) {
            String string = nbt.getString("CustomName");
            entity.setCustomName(Text.Serialization.fromJson(string, entity.getRegistryManager()));
        }

        entity.setCustomNameVisible(nbt.getBoolean("CustomNameVisible"));
        entity.setSilent(nbt.getBoolean("Silent"));
        entity.setNoGravity(nbt.getBoolean("NoGravity"));
        entity.setGlowing(nbt.getBoolean("Glowing"));
        entity.setFrozenTicks(nbt.getInt("TicksFrozen"));
        if (nbt.contains("Tags", NbtElement.LIST_TYPE)) {
            entity.getCommandTags().clear();
            NbtList nbtList4 = nbt.getList("Tags", NbtElement.STRING_TYPE);
            int i = Math.min(nbtList4.size(), 1024);

            for(int j = 0; j < i; ++j) {
                entity.getCommandTags().add(nbtList4.getString(j));
            }
        }

        ((IMixinEntity) entity).readCustomDataFromNbt(nbt);
    }

    public static void loadNbtIntoEntity0(Entity entity, NbtCompound nbt)
    {
        double prevX = entity.prevX;
        double prevY = entity.prevY;
        double prevZ = entity.prevZ;
        double lastRenderX = entity.lastRenderX;
        double lastRenderY = entity.lastRenderY;
        double lastRenderZ = entity.lastRenderZ;
        float prevYaw = entity.prevYaw;
        float prevPitch = entity.prevPitch;
        entity.readNbt(nbt);
        entity.prevX = prevX;
        entity.prevY = prevY;
        entity.prevZ = prevZ;
        entity.lastRenderX = lastRenderX;
        entity.lastRenderY = lastRenderY;
        entity.lastRenderZ = lastRenderZ;
        entity.prevYaw = prevYaw;
        entity.prevPitch = prevPitch;
    }
}
