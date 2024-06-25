package fi.dy.masa.minihud.data;

import com.google.gson.JsonObject;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.network.ServuxEntitiesHandler;
import fi.dy.masa.minihud.network.ServuxEntitiesPacket;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class EntitiesDataStorage
{
    private static final EntitiesDataStorage INSTANCE = new EntitiesDataStorage();

    public static EntitiesDataStorage getInstance()
    {
        return INSTANCE;
    }

    private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
    private final static MinecraftClient mc = MinecraftClient.getInstance();
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;

    private Set<BlockPos> pendingBlockEntities = new HashSet<>();
    private Set<Integer> pendingEntities = new HashSet<>();

    @Nullable
    public World getWorld()
    {
        return mc.world;
    }

    // BlockEntity.createNbtWithIdentifyingData
    @Nullable
    public BlockEntity handleBlockEntityData(BlockPos pos, NbtCompound nbt)
    {
        if (nbt == null || this.getWorld() == null) return null;

        BlockEntity blockEntity = this.getWorld().getBlockEntity(pos);
        if (blockEntity != null)
        {
            blockEntity.read(nbt, this.getWorld().getRegistryManager());
            return blockEntity;
        }
        else
        {
            BlockEntity blockEntity2 = BlockEntity.createFromNbt(pos, this.getWorld().getBlockState(pos), nbt, mc.world.getRegistryManager());
            if (blockEntity2 != null)
            {
                this.getWorld().addBlockEntity(blockEntity2);
                return blockEntity2;
            }
        }

        return null;
    }

    // Entity.saveSelfNbt
    @Nullable
    public Entity handleEntityData(int entityId, NbtCompound nbt)
    {
        if (nbt == null || this.getWorld() == null) return null;
        Entity entity = this.getWorld().getEntityById(entityId);
        if (entity != null)
        {
            entity.readNbt(nbt);
        }
        return entity;
    }

    private EntitiesDataStorage()
    {
    }

    public Identifier getNetworkChannel()
    {
        return ServuxEntitiesHandler.CHANNEL_ID;
    }

    private static ClientPlayNetworkHandler getVanillaHandler()
    {
        if (mc.player != null)
        {
            return mc.player.networkHandler;
        }

        return null;
    }

    public IPluginClientPlayHandler<ServuxEntitiesPacket.Payload> getNetworkHandler()
    {
        return HANDLER;
    }

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

    public boolean hasServuxServer()
    {
        return this.servuxServer;
    }

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

            HANDLER.encodeClientData(ServuxEntitiesPacket.MetadataRequest(nbt));
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

    public void requestBlockEntity(World world, BlockPos pos)
    {
        if (world.getBlockState(pos).getBlock() instanceof BlockEntityProvider)
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
        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            pendingBlockEntities.add(pos);
            handler.getDataQueryHandler().queryBlockNbt(pos, nbtCompound ->
            {
                handleBlockEntityData(pos, nbtCompound);
            });
        }
    }

    public void requestQueryEntityData(int entityId)
    {
        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            pendingEntities.add(entityId);
            handler.getDataQueryHandler().queryEntityNbt(entityId, nbtCompound ->
            {
                handleEntityData(entityId, nbtCompound);
            });
        }
    }

    public void requestServuxBlockEntityData(BlockPos pos)
    {
        pendingBlockEntities.add(pos);
        HANDLER.encodeClientData(ServuxEntitiesPacket.BlockEntityRequest(pos));
    }

    public void requestServuxEntityData(int entityId)
    {
        pendingEntities.add(entityId);
        HANDLER.encodeClientData(ServuxEntitiesPacket.EntityRequest(entityId));
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
