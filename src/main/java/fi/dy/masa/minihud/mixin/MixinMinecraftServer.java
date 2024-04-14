package fi.dy.masa.minihud.mixin;

import java.util.function.BooleanSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.DebugInfoUtils;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Shadow
    public abstract GameRules getGameRules();
    @Shadow public abstract ServerWorld getOverworld();

    @Inject(method = "tick", at = @At("TAIL"))
    public void onServerTickPost(BooleanSupplier supplier, CallbackInfo ci)
    {
        DebugInfoUtils.onServerTickEnd((MinecraftServer) (Object) this);
    }

    @Inject(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setSpawnPos(Lnet/minecraft/util/math/BlockPos;F)V", shift = At.Shift.AFTER))
    private void checkSpawnChunkRadius(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci)
    {
        if (this.getOverworld().getSpawnPos() != DataStorage.getInstance().getWorldSpawn())
        {
            DataStorage.getInstance().setWorldSpawn(this.getOverworld().getSpawnPos());
        }
        if (this.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS) != DataStorage.getInstance().getSpawnChunkRadius())
        {
            DataStorage.getInstance().setSpawnChunkRadius(this.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS));
            }
    }
}
