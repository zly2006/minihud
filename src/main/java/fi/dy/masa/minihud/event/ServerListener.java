package fi.dy.masa.minihud.event;

import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.malilib.network.ClientNetworkPlayInitHandler;
import fi.dy.masa.malilib.network.ClientNetworkPlayRegister;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.network.test.ClientDebugSuite;
import net.minecraft.server.MinecraftServer;

public class ServerListener implements IServerListener
{
    /**
     * This interface for IntegratedServers() works much more reliably than invoking a WorldLoadHandler
     * @param minecraftServer
     */

    public void onServerStarting(MinecraftServer minecraftServer)
    {
        ClientNetworkPlayInitHandler.registerPlayChannels();
        ClientDebugSuite.checkGlobalChannels();
        MiniHUD.printDebug("MinecraftServerEvents#onServerStarting(): invoked.");
    }
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        ClientNetworkPlayRegister.registerDefaultReceivers();
        ClientDebugSuite.checkGlobalChannels();
        MiniHUD.printDebug("MinecraftServerEvents#onServerStarted(): invoked.");
    }
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        ClientDebugSuite.checkGlobalChannels();
        MiniHUD.printDebug("MinecraftServerEvents#onServerStopping(): invoked.");
    }
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        ClientNetworkPlayRegister.unregisterDefaultReceivers();
        ClientDebugSuite.checkGlobalChannels();
        MiniHUD.printDebug("MinecraftServerEvents#onServerStopped(): invoked.");
    }
}
