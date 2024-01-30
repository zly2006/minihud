package fi.dy.masa.minihud.mixin;

import fi.dy.masa.malilib.network.ClientNetworkPlayInitHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.minihud.data.DataStorage;
import fi.dy.masa.minihud.util.NotificationUtils;

@Mixin(net.minecraft.client.network.ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{
    @Inject(method = "onBlockUpdate", at = @At("RETURN"))
    private void minihud_markChunkChangedBlockChange(net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket packet, CallbackInfo ci)
    {
        NotificationUtils.onBlockChange(packet.getPos(), packet.getState());
    }

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void minihud_markChunkChangedFullChunk(net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket packet, CallbackInfo ci)
    {
        NotificationUtils.onChunkData(packet.getChunkX(), packet.getChunkZ(), packet.getChunkData());
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void minihud_markChunkChangedMultiBlockChange(net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci)
    {
        net.minecraft.util.math.ChunkSectionPos pos = ((IMixinChunkDeltaUpdateS2CPacket) packet).minihud_getChunkSectionPos();
        NotificationUtils.onMultiBlockChange(pos, packet);
    }

    @Inject(method = "onGameMessage", at = @At("RETURN"))
    private void minihud_onGameMessage(net.minecraft.network.packet.s2c.play.GameMessageS2CPacket packet, CallbackInfo ci)
    {
        DataStorage.getInstance().onChatMessage(packet.content());
    }

    @Inject(method = "onPlayerListHeader", at = @At("RETURN"))
    private void minihud_onHandlePlayerListHeaderFooter(net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket packetIn, CallbackInfo ci)
    {
        DataStorage.getInstance().handleCarpetServerTPSData(packetIn.getFooter());
        DataStorage.getInstance().getMobCapData().parsePlayerListFooterMobCapData(packetIn.getFooter());
    }

    @Inject(method = "onWorldTimeUpdate", at = @At("RETURN"))
    private void minihud_onTimeUpdate(net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket packetIn, CallbackInfo ci)
    {
        DataStorage.getInstance().onServerTimeUpdate(packetIn.getTime());
    }

    @Inject(method = "onPlayerSpawnPosition", at = @At("RETURN"))
    private void minihud_onSetSpawn(net.minecraft.network.packet.s2c.play.PlayerSpawnPositionS2CPacket packet, CallbackInfo ci)
    {
        DataStorage.getInstance().setWorldSpawnIfUnknown(packet.getPos());
    }
    @Inject(method = "onGameJoin", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;joinWorld(" +
                    "Lnet/minecraft/client/world/ClientWorld;)V"))
    private void minihud_onPreGameJoin(GameJoinS2CPacket packet, CallbackInfo ci)
    {
        ClientNetworkPlayInitHandler.registerPlayChannels();
    }

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void minihud_onPostGameJoin(GameJoinS2CPacket packet, CallbackInfo ci)
    {
        ClientNetworkPlayInitHandler.registerReceivers();
    }
}
