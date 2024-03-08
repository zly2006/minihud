package fi.dy.masa.minihud.event;

import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.data.DataStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;

public class ServerListener implements IServerListener
{
    /**
     * This interface for IntegratedServers() works much more reliably than invoking a WorldLoadHandler
     * -- I've tried it first! --
     * The WorldLoadHandler calls Connect/Disconnect multiple times, breaking the networking API.
     * So using the IServerListener is best because it only gets invoked ONCE per a server start / stop
     * to get handled correctly.
     */

    @Override
    public void onServerStarting(MinecraftServer server)
    {
        if (server.isSingleplayer())
        {
            DataStorage.getInstance().setHasIntegratedServer(true);
            DataStorage.getInstance().setHasOpenToLan(false);
            MiniHUD.printDebug("[{}] Single Player Mode detected", Reference.MOD_ID);
        }
    }

    @Override
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        // NO-OP
    }

    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        MiniHUD.printDebug("[{}] Integrated Server Mode detected", Reference.MOD_ID);
        DataStorage.getInstance().setHasIntegratedServer(true);
        DataStorage.getInstance().setHasOpenToLan(false);
    }

    @Override
    public void onServerOpenToLan(IntegratedServer server)
    {
        MiniHUD.printDebug("[{}] OpenToLan Mode detected [Serving on localhost:{}]", Reference.MOD_ID, server.getServerPort());
        DataStorage.getInstance().setHasIntegratedServer(true);
        DataStorage.getInstance().setHasOpenToLan(true);
    }

    @Override
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        MiniHUD.printDebug("[{}] server is stopping", Reference.MOD_ID);
    }

    @Override
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        MiniHUD.printDebug("[{}] server has stopped", Reference.MOD_ID);
        DataStorage.getInstance().setHasIntegratedServer(false);
        DataStorage.getInstance().setHasOpenToLan(false);
    }
}
