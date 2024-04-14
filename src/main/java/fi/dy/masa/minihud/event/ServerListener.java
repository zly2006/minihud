package fi.dy.masa.minihud.event;

import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarting(MinecraftServer server)
    {
        if (server.isSingleplayer())
        {
            DataStorage.getInstance().setIntegratedServer(true);
            DataStorage.getInstance().setOpenToLan(false);
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
        DataStorage.getInstance().setIntegratedServer(true);
        DataStorage.getInstance().setOpenToLan(false);
    }

    @Override
    public void onServerOpenToLan(IntegratedServer server)
    {
        DataStorage.getInstance().setIntegratedServer(true);
        DataStorage.getInstance().setOpenToLan(true);
    }

    @Override
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        // NO-OP
    }

    @Override
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        DataStorage.getInstance().setIntegratedServer(false);
        DataStorage.getInstance().setOpenToLan(false);
    }
}
