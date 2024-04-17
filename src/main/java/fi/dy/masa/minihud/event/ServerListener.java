package fi.dy.masa.minihud.event;

import net.minecraft.server.MinecraftServer;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.minihud.util.DataStorage;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerStarted(MinecraftServer server)
    {
        DataStorage.getInstance().checkWorldSeed(server);
    }
}
