package fi.dy.masa.minihud.mixin;

import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.DataStorage;
import net.minecraft.server.world.ChunkTicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkTicketManager.class)
public class MixinChunkTicketManager
{
    @Inject(method = "setSimulationDistance", at = @At("TAIL"))
    private void minihud_getSimulationDistance(int distance, CallbackInfo ci)
    {
        if (distance > 0)
        {
            final int simul = DataStorage.getInstance().getSimulationDistance();
            if (simul != distance)
            {
                MiniHUD.printDebug("MixinChunkTicketManager#minihud_getSimulationDistance() Simulation Distance change detected {} -> {}", simul, distance);
                DataStorage.getInstance().setSimulationDistance(distance);
            }
        }
    }
}
