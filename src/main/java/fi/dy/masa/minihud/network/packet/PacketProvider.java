package fi.dy.masa.minihud.network.packet;

import fi.dy.masa.malilib.event.ServuxMetadataHandler;
import fi.dy.masa.malilib.event.ServuxStructuresHandler;

public class PacketProvider
{
    static ServuxStructuresListener minihud_servuxStructuresListener = new ServuxStructuresListener();
    static ServuxMetadataListener minihud_servuxMetadataListener = new ServuxMetadataListener();
    public static void registerPayloads()
    {
        // Register Client Payload Listeners
        ServuxStructuresHandler.getInstance().registerServuxStructuresHandler(minihud_servuxStructuresListener);
        ServuxMetadataHandler.getInstance().registerServuxMetadataHandler(minihud_servuxMetadataListener);
    }

    public static void unregisterPayloads()
    {
        ServuxStructuresHandler.getInstance().unregisterServuxStructuresHandler(minihud_servuxStructuresListener);
        ServuxMetadataHandler.getInstance().unregisterServuxMetadataHandler(minihud_servuxMetadataListener);
    }
}