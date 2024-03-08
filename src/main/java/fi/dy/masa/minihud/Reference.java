package fi.dy.masa.minihud;

import net.minecraft.MinecraftVersion;

public class Reference
{
    public static final String MOD_ID = "minihud";
    public static final String MOD_NAME = "MiniHUD";
    public static final String MOD_VERSION = MiniHUD.getModVersionString(MOD_ID);
    public static final String MC_VERSION = MinecraftVersion.CURRENT.getName();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID+"-"+MOD_TYPE+"-"+MC_VERSION+"-"+MOD_VERSION;
}
