package fi.dy.masa.minihud.data;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class EntityData
{
    // EntitySpawnS2CPacket
    private UUID uuid;
    private EntityType<?> entityType;
    private int entityId;
    BlockPos pos;
    private int entityData;
    // EntityEquipmentUpdateS2CPacket
    private List<Pair<EquipmentSlot, ItemStack>> equipmentList;
    // EntityAttributesS2CPacket
    private List<Entry> entries;
    // NbtQueryResponseS2CPacket
    private int transactionId;
    private NbtCompound nbt = new NbtCompound();
    private String customName = "";

    // EntitySpawnS2CPacket
    public EntityData(Entity entity, int entityData, BlockPos pos)
    {
        this.entityId = entity.getId();
        this.uuid = entity.getUuid();
        this.pos = pos;
        this.entityType = entity.getType();
        this.entityData = entityData;
    }

    public EntityData(int entityId, UUID uuid, double x, double y, double z, EntityType<?> entityType, int entityData)
    {
        this.entityId = entityId;
        this.uuid = uuid;
        this.pos = new BlockPos((int) x, (int) y, (int) z);
        this.entityType = entityType;
        this.entityData = entityData;
    }

    public int getId() { return this.entityId; }

    public UUID getUuid() { return this.uuid; }

    public EntityType<?> getEntityType() { return this.entityType; }

    public BlockPos getPos() { return this.pos; }

    public int getEntityData() { return this.entityData; }

    public void updatePos(BlockPos pos) { this.pos = pos; }

    public void updateEntityData(int data) { this.entityData = data; }

    // EntityEquipmentUpdateS2CPacket
    public EntityData(int entityId, List<Pair<EquipmentSlot, ItemStack>> equipmentList, boolean eq)
    {
        this.entityId = entityId;
        if (this.equipmentList.isEmpty() == false)
        {
            this.equipmentList.clear();
        }
        this.equipmentList.addAll(equipmentList);
    }

    public List<Pair<EquipmentSlot, ItemStack>> getEquipmentList() { return this.equipmentList; }

    public void updateEquipmentList(List<Pair<EquipmentSlot, ItemStack>> equipmentList)
    {
        if (this.equipmentList.isEmpty() == false)
        {
            this.equipmentList.clear();
        }
        this.equipmentList.addAll(equipmentList);
    }

    // EntityAttributesS2CPacket
    public EntityData(int entityId, List<Entry> entries)
    {
        this.entityId = entityId;
        if (this.entries.isEmpty() == false)
        {
            this.entries.clear();
        }
        this.entries.addAll(entries);
    }

    public List<Entry> getEntries() { return this.entries; }

    public void updateEntires(List<Entry> entries)
    {
        if (this.entries.isEmpty() == false)
        {
            this.entries.clear();
        }
        this.entries.addAll(entries);
    }

    // QueryEntityNbtC2SPacket
    public EntityData(int transactionId, int entityId, @Nullable NbtCompound nbt)
    {
        this.transactionId = transactionId;
        this.entityId = entityId;

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

    public int getTransactionId() { return this.transactionId; }

    public void updateTransactionId(int id) { this.transactionId = id; }

    public NbtCompound getNbt() { return this.nbt; }

    public void updateNbt(NbtCompound nbt)
    {
        this.nbt.copyFrom(nbt);
    }

    public boolean readFromNbt()
    {
        if (this.nbt.isEmpty())
        {
            return false;
        }

        NbtCompound nbtCopy = new NbtCompound();
        NbtList posList;
        UUID uuid;
        nbtCopy.copyFrom(this.nbt);

        if (nbtCopy.contains("Pos"))
        {
            posList = nbtCopy.getList("Pos", 6);
            double x = MathHelper.clamp(posList.getDouble(0), -3.0000512E7, 3.0000512E7);
            double y = MathHelper.clamp(posList.getDouble(1), -2.0E7, 2.0E7);
            double z = MathHelper.clamp(posList.getDouble(2), -3.0000512E7, 3.0000512E7);
            int a = MathHelper.floor(x);
            int b = MathHelper.floor(y);
            int c = MathHelper.floor(z);
            this.pos = new BlockPos(a, b, c);
        }
        if (nbtCopy.containsUuid("UUID"))
        {
            uuid = nbtCopy.getUuid("UUID");
        }
        if (nbtCopy.contains("CustomName", 8))
        {
            this.customName = nbtCopy.getString("CustomName");
        }

        if (nbtCopy.contains("attributes"))
        {
            // Do Attributes
        }

        return false;
    }

    public String getCustomName() { return this.customName; }

    public record Entry(RegistryEntry<EntityAttribute> attribute, double base, Collection<EntityAttributeModifier> modifiers)
    {
        public Entry(RegistryEntry<EntityAttribute> attribute, double base, Collection<EntityAttributeModifier> modifiers)
        {
            this.attribute = attribute;
            this.base = base;
            this.modifiers = modifiers;
        }

        public RegistryEntry<EntityAttribute> getAttribute() {
            return this.attribute;
        }

        public double getBase() {
            return this.base;
        }

        public Collection<EntityAttributeModifier> getModifiers() {
            return this.modifiers;
        }
    }
}
