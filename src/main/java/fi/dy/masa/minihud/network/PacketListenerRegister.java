package fi.dy.masa.minihud.network;


import fi.dy.masa.malilib.network.handler.client.ClientPlayHandler;
import fi.dy.masa.malilib.network.payload.channel.ServuxStructuresPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PacketListenerRegister
{
    static ServuxStructuresPlayListener<ServuxStructuresPayload> minihud_servuxStructuresListener = ServuxStructuresPlayListener.INSTANCE;
    private static boolean payloadsRegistered = false;

    public static void registerListeners()
    {
        if (payloadsRegistered)
            return;

        // Register Client Payload Listeners
        ClientPlayHandler.getInstance().registerClientPlayHandler(minihud_servuxStructuresListener);

        payloadsRegistered = true;
    }


    public static void unregisterListeners()
    {
        ClientPlayHandler.getInstance().unregisterClientPlayHandler(minihud_servuxStructuresListener);

        payloadsRegistered = false;
    }
}