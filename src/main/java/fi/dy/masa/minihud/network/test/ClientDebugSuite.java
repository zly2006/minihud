package fi.dy.masa.minihud.network.test;

import fi.dy.masa.minihud.MiniHUD;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

import java.util.Iterator;
import java.util.Set;

public class ClientDebugSuite {
    public static void checkGlobalChannels() {
        MiniHUD.printDebug("ClientDebugSuite#checkGlobalChannels(): Start.");
        Set<Identifier> channels = ClientPlayNetworking.getGlobalReceivers();
        Iterator<Identifier> iterator = channels.iterator();
        int i = 0;
        while (iterator.hasNext())
        {
            Identifier id = iterator.next();
            i++;
            MiniHUD.printDebug("ClientDebugSuite#checkGlobalChannels(): id("+i+") hash: "+id.hashCode()+" //name: "+id.getNamespace()+" path: "+id.getPath());
        }
        MiniHUD.printDebug("ClientDebugSuite#checkGlobalChannels(): END. Total Channels: "+i);
    }
}
