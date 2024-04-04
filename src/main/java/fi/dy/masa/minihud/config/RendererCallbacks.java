package fi.dy.masa.minihud.config;

import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.network.PacketType;
import fi.dy.masa.minihud.network.ServuxStructuresPlayListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.renderer.OverlayRendererBeaconRange;
import fi.dy.masa.minihud.renderer.OverlayRendererBiomeBorders;
import fi.dy.masa.minihud.renderer.OverlayRendererConduitRange;
import fi.dy.masa.minihud.renderer.OverlayRendererLightLevel;
import fi.dy.masa.minihud.renderer.OverlayRendererRandomTickableChunks;
import fi.dy.masa.minihud.renderer.OverlayRendererRegion;
import fi.dy.masa.minihud.renderer.OverlayRendererSlimeChunks;
import fi.dy.masa.minihud.renderer.OverlayRendererSpawnChunks;
import fi.dy.masa.minihud.data.DataStorage;

public class RendererCallbacks
{
    public static void onBeaconRangeToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererBeaconRange.INSTANCE.setNeedsUpdate();
        }
    }

    public static void onBiomeBorderToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererBiomeBorders.INSTANCE.setNeedsUpdate();
        }
    }

    public static void onConduitRangeToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererConduitRange.INSTANCE.setNeedsUpdate();
        }
    }

    public static void onLightLevelToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererLightLevel.setNeedsUpdate();
        }
    }

    public static void onRandomTicksFixedToggled(IConfigBoolean config)
    {
        Entity entity = EntityUtils.getCameraEntity();

        if (config.getBooleanValue() && entity != null)
        {
            Vec3d pos = entity.getPos();
            OverlayRendererRandomTickableChunks.newPos = pos;
            String green = GuiBase.TXT_GREEN;
            String rst = GuiBase.TXT_RST;
            String strStatus = green + StringUtils.translate("malilib.message.value.on") + rst;
            String strPos = String.format("x: %.2f, y: %.2f, z: %.2f", pos.x, pos.y, pos.z);
            String message = StringUtils.translate("minihud.message.toggled_using_position", config.getPrettyName(), strStatus, strPos);

            InfoUtils.printActionbarMessage(message);
        }
    }

    public static void onRandomTicksPlayerToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererRandomTickableChunks.setNeedsUpdate();
        }
    }

    public static void onRegionFileToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererRegion.setNeedsUpdate();
        }
    }

    public static void onSlimeChunksToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererSlimeChunks.setNeedsUpdate();
            OverlayRendererSlimeChunks.onEnabled();
        }
    }

    public static void onSpawnChunksPlayerToggled(IConfigBoolean config)
    {
        if (config.getBooleanValue())
        {
            OverlayRendererSpawnChunks.setNeedsUpdate();
        }
    }

    public static void onSpawnChunksRealToggled(IConfigBoolean config)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc != null && mc.player != null)
        {
            if (config.getBooleanValue())
            {
                BlockPos spawn = DataStorage.getInstance().getWorldSpawn();
                int radius = DataStorage.getInstance().getSpawnChunkRadius();
                String green = GuiBase.TXT_GREEN;
                String red = GuiBase.TXT_RED;
                String rst = GuiBase.TXT_RST;
                String message;

                if (radius != 0)
                {
                    // Set Vanilla default if unknown
                    if (radius < 0)
                    {
                        DataStorage.getInstance().setSpawnChunkRadius(2);
                    }
                    String strStatus = green + StringUtils.translate("malilib.message.value.on") + rst;
                    String strPos = String.format("x: %d, y: %d, z: %d [R: %d]", spawn.getX(), spawn.getY(), spawn.getZ(), radius);
                    message = StringUtils.translate("minihud.message.toggled_using_world_spawn", config.getPrettyName(), strStatus, strPos);

                    if (!mc.isIntegratedServerRunning() && DataStorage.getInstance().hasServuxServer())
                    {
                        // Refresh Spawn Metadata
                        NbtCompound nbt = new NbtCompound();
                        nbt.putInt("packetType", PacketType.Structures.PACKET_C2S_REQUEST_SPAWN_METADATA);
                        nbt.putString("version", Reference.MOD_STRING);
                        ServuxStructuresPlayListener.INSTANCE.encodeC2SNbtCompound(PayloadType.SERVUX_STRUCTURES, nbt);
                    }
                    else
                    {
                        OverlayRendererSpawnChunks.setNeedsUpdate();
                    }
                }
                else
                {
                    OverlayRendererSpawnChunks.setNeedsUpdate();

                    String strStatus = red + StringUtils.translate("malilib.message.value.off") + rst;
                    String strPos = red + String.format("x: %d, y: %d, z: %d [R: 0]", spawn.getX(), spawn.getY(), spawn.getZ());
                    message = StringUtils.translate("minihud.message.toggled_using_world_spawn", config.getPrettyName(), strStatus, strPos);

                    RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL.setBooleanValue(false);
                }

                InfoUtils.printActionbarMessage(message);
            }
        }
    }

    public static void onStructuresToggled(IConfigBoolean config)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc != null && mc.player != null)
        {
            if (mc.isIntegratedServerRunning() == false)
            {
                if (config.getBooleanValue())
                {
                    DataStorage.getInstance().registerStructureChannel();
                }
                else
                {
                    DataStorage.getInstance().unregisterStructureChannel();
                }
            }
            else
            {
                DataStorage.getInstance().setStructuresNeedUpdating();
            }
        }
    }
}
