package fi.dy.masa.minihud.data;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.QueryBlockNbtC2SPacket;
import net.minecraft.network.packet.c2s.play.QueryEntityNbtC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.network.ServuxEntitiesHandler;
import fi.dy.masa.minihud.network.ServuxEntitiesPacket;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class EntitiesDataStorage
{
    private static final EntitiesDataStorage INSTANCE = new EntitiesDataStorage();
    public static EntitiesDataStorage getInstance() { return INSTANCE; }

    private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
    private final static MinecraftClient mc = MinecraftClient.getInstance();
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;

    static private class CacheEntry<T> {
        T value;
        int ticksAdded;

        public CacheEntry(T value, int ticksAdded) {
            this.value = value;
            this.ticksAdded = ticksAdded;
        }
    }

    private Map<BlockPos, CacheEntry<BlockEntity>> blockEntityCache;
    private Map<UUID, CacheEntry<Entity>> entityCache;

    // BlockEntity.createNbtWithIdentifyingData
    @Nullable
    public BlockEntity handleBlockEntityData(World world, NbtCompound nbt) {
        if (nbt == null) return null;
        BlockPos pos = BlockEntity.posFromNbt(nbt);
        BlockEntity blockEntity = BlockEntity.createFromNbt(pos, world.getBlockState(pos), nbt, world.getRegistryManager());
        blockEntityCache.put(pos, new CacheEntry<>(blockEntity, 0));
        return blockEntity;
    }

    // Entity.saveSelfNbt
    @Nullable
    public Entity handleEntityData(World world, NbtCompound nbt) {
        if (nbt == null) return null;
        Entity entity = EntityType.getEntityFromNbt(nbt, world).orElse(null);
        entityCache.put(entity.getUuid(), new CacheEntry<>(entity, 0));
        return entity;
    }

    private EntitiesDataStorage() { }

    public Identifier getNetworkChannel() {return ServuxEntitiesHandler.CHANNEL_ID;}

    private static ClientPlayNetworkHandler getVanillaHandler()
    {
        if (mc.player != null)
        {
            return mc.player.networkHandler;
        }

        return null;
    }

    public IPluginClientPlayHandler<ServuxEntitiesPacket.Payload> getNetworkHandler() { return HANDLER; }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            MiniHUD.printDebug("EntitiesDataStorage#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());
            this.servuxServer = false;
            this.hasInValidServux = false;
        }
        else
        {
            MiniHUD.printDebug("EntitiesDataStorage#reset() - dimension change or log-in");
        }
        // Clear data
    }

    public void setIsServuxServer()
    {
        this.servuxServer = true;
        this.hasInValidServux = false;
    }

    public boolean hasServuxServer() { return this.servuxServer; }

    public void setServuxVersion(String ver)
    {
        if (ver != null && ver.isEmpty() == false)
        {
            this.servuxVersion = ver;
            MiniHUD.logger.warn("entityDataChannel: joining Servux version {}", ver);
        }
        else
        {
            this.servuxVersion = "unknown";
        }
    }

    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxEntitiesPacket.Payload.ID, ServuxEntitiesPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public void onWorldPre()
    {
        if (DataStorage.getInstance().hasIntegratedServer() == false)
        {
            HANDLER.registerPlayReceiver(ServuxEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        // NO-OP
    }

    public void requestMetadata()
    {
        if (DataStorage.getInstance().hasIntegratedServer() == false)
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(new ServuxEntitiesPacket(nbt, true));
        }
    }

    public boolean receiveServuxMetadata(NbtCompound data)
    {
        if (DataStorage.getInstance().hasIntegratedServer() == false)
        {
            MiniHUD.printDebug("EntitiesDataStorage#receiveServuxMetadata(): received METADATA from Servux");

            if (data.getInt("version") != ServuxEntitiesPacket.PROTOCOL_VERSION)
            {
                MiniHUD.logger.warn("entityDataChannel: Mis-matched protocol version!");
            }
            this.setServuxVersion(data.getString("servux"));
            this.setIsServuxServer();

            return true;
        }

        return false;
    }

    public void onPacketFailure()
    {
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    // TODO --> Add Data Handling Here
    public void requestBlockEntity(BlockPos pos)
    {
        if (this.hasServuxServer())
        {
            this.requestServuxBlockEntityData(pos);
        }
        else
        {
            this.requestQueryBlockEntity(pos);
        }
    }

    public void requestEntity(int entityId)
    {
        if (this.hasServuxServer())
        {
            this.requestServuxEntityData(entityId);
        }
        else
        {
            this.requestQueryEntityData(entityId);
        }
    }

    public void requestQueryBlockEntity(BlockPos pos)
    {
        // FIXME
        int transactionId = -1;
        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            handler.sendPacket(new QueryBlockNbtC2SPacket(transactionId, pos));
        }
    }

    public void requestQueryEntityData(int entityId)
    {
        // FIXME
        int transactionId = -1;
        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            handler.sendPacket(new QueryEntityNbtC2SPacket(transactionId, entityId));
        }
    }

    public void requestServuxBlockEntityData(BlockPos pos)
    {
        // FIXME
        int transactionId = -1;

        HANDLER.encodeClientData(new ServuxEntitiesPacket(transactionId, pos));
    }

    public void requestServuxEntityData(int entityId)
    {
        // FIXME
        int transactionId = -1;

        HANDLER.encodeClientData(new ServuxEntitiesPacket(transactionId, entityId));
    }

    public void handleEntityData(int transactionId, @Nullable NbtCompound nbt)
    {
        // Handle
        if (nbt != null && nbt.isEmpty() == false)
        {
            MiniHUD.printDebug("EntitiesDataStorage#handleEntityData(): received transactionId {} // {}", transactionId, nbt.toString());
        }
        else
        {
            MiniHUD.printDebug("EntitiesDataStorage#handleEntityData(): received transactionId {} // <EMPTY>", transactionId);
        }
    }

    public JsonObject toJson()
    {
        return new JsonObject();
    }

    public void fromJson(JsonObject obj)
    {
        // NO-OP
    }
}
