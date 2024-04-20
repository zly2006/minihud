package fi.dy.masa.minihud.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.minihud.util.DataStorage;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarting(MinecraftServer server)
    {
        // NO-OP
    }

    @Override
    public void onServerStarted(MinecraftServer server)
    {
        DataStorage.getInstance().checkWorldSeed(server);
    }

    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        DataStorage.getInstance().setHasIntegratedServer(true);
    }

    @Override
    public void onServerOpenToLan(IntegratedServer server)
    {
        // NO-OP
    }

    @Override
    public void onServerStopping(MinecraftServer server)
    {
        // NO-OP
    }

    @Override
    public void onServerStopped(MinecraftServer server)
    {
        DataStorage.getInstance().reset(true);
    }
}
