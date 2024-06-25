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
        entity.fallDistance = nbt.getFloat("FallDistance");
        entity.setFireTicks(nbt.getShort("Fire"));
        if (nbt.contains("Air")) {
            entity.setAir(nbt.getShort("Air"));
        }

        entity.setOnGround(nbt.getBoolean("OnGround"));
        entity.setInvulnerable(nbt.getBoolean("Invulnerable"));
        entity.setPortalCooldown(nbt.getInt("PortalCooldown"));
        if (nbt.containsUuid("UUID")) {
            entity.setUuid(nbt.getUuid("UUID"));
        }

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
            int max = Math.min(nbtList4.size(), 1024);

            for(int i = 0; i < max; ++i) {
                entity.getCommandTags().add(nbtList4.getString(i));
            }
        }

        ((IMixinEntity) entity).readCustomDataFromNbt(nbt);
    }
}
