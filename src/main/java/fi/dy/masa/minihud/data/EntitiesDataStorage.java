package fi.dy.masa.minihud.data;

import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.network.ServuxEntitiesHandler;
import fi.dy.masa.minihud.network.ServuxEntitiesPacket;
import fi.dy.masa.minihud.util.DataStorage;

public class EntitiesDataStorage
{
    private static final EntitiesDataStorage INSTANCE = new EntitiesDataStorage();

    public static EntitiesDataStorage getInstance() {return INSTANCE;}

    private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();

    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;
    private int servuxTimeout;
    private boolean shouldRegisterBlockEntitiesChannel;

    private boolean enabled;

    private EntitiesDataStorage()
    {
        this.enabled = false;
    }

    public Identifier getNetworkChannel() {return ServuxEntitiesHandler.CHANNEL_ID;}

    public IPluginClientPlayHandler<ServuxEntitiesPacket.Payload> getNetworkHandler() {return HANDLER;}

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
        MiniHUD.printDebug("EntitiesDataStorage#setIsServuxServer()");
        this.servuxServer = true;
        if (this.hasInValidServux)
        {
            this.hasInValidServux = false;
        }
    }

    public boolean hasServuxServer() { return this.servuxServer; }

    public void setServuxVersion(String ver)
    {
        MiniHUD.printDebug("EntitiesDataStorage#setServuxVersion() version {}", ver);

        if (ver != null && ver.isEmpty() == false)
        {
            this.servuxVersion = ver;
        } else
        {
            this.servuxVersion = "unknown";
        }
    }

    public void onGameInit()
    {
        MiniHUD.logger.warn("EntitiesDataStorage#onGameInit()");

        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxEntitiesPacket.Payload.ID, ServuxEntitiesPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public void onWorldPre()
    {
        MiniHUD.printDebug("EntitiesDataStorage#onWorldPre()");

        if (DataStorage.getInstance().hasIntegratedServer() == false)
        {
            HANDLER.registerPlayReceiver(ServuxEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        MiniHUD.printDebug("EntitiesDataStorage#onWorldJoin()");

        if (DataStorage.getInstance().hasIntegratedServer() == false)
        {
            if (this.enabled)
            {
                this.registerBlockEntitiesChannel();
            }
            else
            {
                this.unregisterBlockEntitiesChannel();
            }
        }
    }

    public void registerBlockEntitiesChannel()
    {
        MiniHUD.printDebug("EntitiesDataStorage#registerBlockEntitiesChannel()");

        this.shouldRegisterBlockEntitiesChannel = true;

        if (this.servuxServer == false && DataStorage.getInstance().hasIntegratedServer() == false && this.hasInValidServux == false)
        {
            if (HANDLER.isPlayRegistered(this.getNetworkChannel()))
            {
                MiniHUD.printDebug("EntitiesDataStorage#registerBlockEnitiesChannel(): sending BLOCK_ENTITY_REGISTER to Servux");

                NbtCompound nbt = new NbtCompound();
                nbt.putString("version", Reference.MOD_STRING);

                HANDLER.encodeClientData(new ServuxEntitiesPacket(ServuxEntitiesPacket.Type.PACKET_C2S_ENTITY_REGISTER, nbt));
            }
        }
        else
        {
            this.shouldRegisterBlockEntitiesChannel = false;
        }
    }

    public void requestMetadata()
    {
        MiniHUD.printDebug("EntitiesDataStorage#requestMetadata()");

        if (DataStorage.getInstance().hasIntegratedServer() == false && this.hasServuxServer())
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(new ServuxEntitiesPacket(ServuxEntitiesPacket.Type.PACKET_C2S_REQUEST_METADATA, nbt));
        }
    }

    public boolean receiveServuxMetadata(NbtCompound data)
    {
        MiniHUD.printDebug("EntitiesDataStorage#receiveServuxMetadata()");

        if (this.servuxServer == false && DataStorage.getInstance().hasIntegratedServer() == false &&
            this.shouldRegisterBlockEntitiesChannel)
        {
            MiniHUD.printDebug("EntitiesDataStorage#receiveServuxBlockEntitiesMetadata(): received METADATA from Servux");

            if (data.getInt("version") != ServuxEntitiesPacket.PROTOCOL_VERSION)
            {
                MiniHUD.logger.warn("blockEntitiesChannel: Mis-matched protocol version!");
            }
            this.servuxTimeout = data.getInt("timeout");
            this.setServuxVersion(data.getString("servux"));
            this.setIsServuxServer();

            // FIXME
            if (this.enabled && !this.hasServuxServer())
            {
                this.registerBlockEntitiesChannel();
                return true;
            }
            else
            {
                this.unregisterBlockEntitiesChannel();
            }
        }

        return false;
    }

    public void unregisterBlockEntitiesChannel()
    {
        MiniHUD.printDebug("EntitiesDataStorage#unregisterBlockEntitiesChannel()");

        if (this.servuxServer)
        {
            this.servuxServer = false;
            if (this.hasInValidServux == false)
            {
                MiniHUD.printDebug("EntitiesDataStorage#unregisterBlockEntitiesChannel(): for {}", this.servuxVersion != null ? this.servuxVersion : "<unknown>");

                HANDLER.encodeClientData(new ServuxEntitiesPacket(ServuxEntitiesPacket.Type.PACKET_C2S_ENTITY_UNREGISTER, new NbtCompound()));
                HANDLER.reset(this.getNetworkChannel());
            }
        }
        this.shouldRegisterBlockEntitiesChannel = false;
    }

    public void onPacketFailure()
    {
        MiniHUD.printDebug("EntitiesDataStorage#onPacketFailure()");

        // Define how to handle multiple sendPayload failures
        this.shouldRegisterBlockEntitiesChannel = false;
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    // TODO --> Add Data Handling Here

    public JsonObject toJson()
    {
        MiniHUD.printDebug("EntitiesDataStorage#toJson()");

        return new JsonObject();
    }

    public void fromJson(JsonObject obj)
    {
        MiniHUD.printDebug("EntitiesDataStorage#fromJson()");
    }
}
