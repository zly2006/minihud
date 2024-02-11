package fi.dy.masa.minihud.event;

import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.network.PacketUtils;
import net.minecraft.server.MinecraftServer;

public class ServerListener implements IServerListener
{
    /**
     * This interface for IntegratedServers() works much more reliably than invoking a WorldLoadHandler
     */

    public void onServerStarting(MinecraftServer minecraftServer)
    {
        PacketUtils.registerPayloads();

        //ClientNetworkPlayInitHandler.registerPlayChannels();
        //ClientDebugSuite.checkGlobalChannels();
        MiniHUD.printDebug("MinecraftServerEvents#onServerStarting(): invoked.");
    }
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        //PayloadTypeRegister.getInstance().registerAllHandlers();

        //ClientNetworkPlayRegister.registerReceivers();
        //ClientDebugSuite.checkGlobalChannels();
        MiniHUD.printDebug("MinecraftServerEvents#onServerStarted(): invoked.");
    }
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        //PayloadTypeRegister.getInstance().resetPayloads();

        //ClientDebugSuite.checkGlobalChannels();
        MiniHUD.printDebug("MinecraftServerEvents#onServerStopping(): invoked.");
    }
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        //ClientNetworkPlayRegister.unregisterReceivers();
        //ClientDebugSuite.checkGlobalChannels();
        MiniHUD.printDebug("MinecraftServerEvents#onServerStopped(): invoked.");
    }
}
