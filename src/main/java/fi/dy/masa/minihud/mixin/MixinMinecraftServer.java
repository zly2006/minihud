package fi.dy.masa.minihud.mixin;

import java.util.function.BooleanSupplier;

import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.data.DataStorage;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.minihud.util.DebugInfoUtils;
import net.minecraft.server.MinecraftServer;

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
    private void minihud_checkSpawnChunkRadius(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci)
    {
        BlockPos spawnPos = this.getOverworld().getSpawnPos();
        int radius = this.getGameRules().getInt(GameRules.SPAWN_CHUNK_RADIUS);
        MiniHUD.printDebug("MixinMinecraftServer#minihud_checkSpawnChunkRadius(): Spawn Position: {}, SPAWN_CHUNK_RADIUS: {}", spawnPos.toShortString(), radius);
        if (spawnPos != DataStorage.getInstance().getWorldSpawn())
            DataStorage.getInstance().setWorldSpawn(spawnPos);
        if (radius != DataStorage.getInstance().getSpawnChunkRadius())
            DataStorage.getInstance().setSpawnChunkRadius(radius);
    }
}
