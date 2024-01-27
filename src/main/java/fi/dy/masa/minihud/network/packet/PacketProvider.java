package fi.dy.masa.minihud.network.packet;

import fi.dy.masa.malilib.event.ServuxPayloadHandler;

public class PacketProvider
{
    static ServuxPayloadListener servuxListener = new ServuxPayloadListener();
    public static void registerPayloads()
    {
        // Register Client Payload Listeners
        ServuxPayloadHandler.getInstance().registerServuxHandler(servuxListener);
    }

    public static void unregisterPayloads()
    {
        ServuxPayloadHandler.getInstance().unregisterServuxHandler(servuxListener);
    }
}