package fi.dy.masa.minihud.network.packet;

import fi.dy.masa.malilib.event.ServuxPayloadHandler;

public class PacketProvider
{
    static ServuxPayloadListener minihud_servuxListener = new ServuxPayloadListener();
    public static void registerPayloads()
    {
        // Register Client Payload Listeners
        ServuxPayloadHandler.getInstance().registerServuxHandler(minihud_servuxListener);
    }

    public static void unregisterPayloads()
    {
        ServuxPayloadHandler.getInstance().unregisterServuxHandler(minihud_servuxListener);
    }
}