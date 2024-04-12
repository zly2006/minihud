package fi.dy.masa.minihud.mixin;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class MixinServerWorld {
    @Shadow
    private int spawnChunkRadius;

    @Inject(method = "setSpawnPos", at = @At("TAIL"))
    private void minihud$checkSpawnPos(BlockPos pos, float angle, CallbackInfo ci)
    {
        // Decrement SPAWN_CHUNK_RADIUS by 1 here to get the real value.
        int radius = (this.spawnChunkRadius - 1);
        MiniHUD.printDebug("MixinServerWorld#servux_checkSpawnPos(): Spawn Position: {}, SPAWN_CHUNK_RADIUS: {}", pos.toShortString(), radius);

        if (pos != DataStorage.getInstance().getWorldSpawn())
        {
            DataStorage.getInstance().setWorldSpawn(pos);
        }
        if (radius != DataStorage.getInstance().getSpawnChunkRadius())
        {
            DataStorage.getInstance().setSpawnChunkRadius(radius);
        }
    }
}