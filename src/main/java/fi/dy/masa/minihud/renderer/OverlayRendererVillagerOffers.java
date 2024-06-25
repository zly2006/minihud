package fi.dy.masa.minihud.renderer;

import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.IMixinMerchantEntity;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class OverlayRendererVillagerOffers extends OverlayRendererBase
{
    public static final OverlayRendererVillagerOffers INSTANCE = new OverlayRendererVillagerOffers();
    @Override
    public String getName()
    {
        return "Villager Offers Overlay";
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return RendererToggle.OVERLAY_VILLAGER_OFFERS.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        return true;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        Box box = entity.getBoundingBox().expand(10, 10, 10);
        World world = WorldUtils.getBestWorld(mc);
        List<VillagerEntity> librarians = world.getEntitiesByClass(VillagerEntity.class, box, villager -> villager.getVillagerData().getProfession() == VillagerProfession.LIBRARIAN);
        for (VillagerEntity librarian : librarians)
        {
            List<String> overlay = new ArrayList<>();
            TradeOfferList offers = ((IMixinMerchantEntity) librarian).offers();
            if (offers == null)
            {
                continue;
            }

            for (TradeOffer tradeOffer : offers)
            {
                if (tradeOffer.getSellItem().getItem() == Items.ENCHANTED_BOOK)
                {
                    for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : tradeOffer.getSellItem().get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantmentEntries())
                    {
                        StringBuilder sb = new StringBuilder();
                        if (entry.getKey().value().getMaxLevel() == entry.getIntValue())
                        {
                            sb.append("§6");
                        }
                        else if (Configs.Generic.VILLAGER_OFFER_HIGHEST_LEVEL_ONLY.getBooleanValue())
                        {
                            continue;
                        }
                        sb.append(Enchantment.getName(entry.getKey(), entry.getIntValue()).getString());
                        sb.append("§r");
                        if (tradeOffer.getFirstBuyItem().item().value() == Items.EMERALD)
                        {
                            sb.append(" ");
                            sb.append(tradeOffer.getFirstBuyItem().count());

                            int lowest = 2 + 3 * entry.getIntValue();
                            int highest = 6 + 13 * entry.getIntValue();
                            if (entry.getKey().isIn(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
                                lowest *= 2;
                                highest *= 2;
                            }

                            if (highest > 64) {
                                highest = 64;
                            }

                            if (tradeOffer.getFirstBuyItem().count() > MathHelper.lerp(Configs.Generic.VILLAGER_OFFER_PRICE_THRESHOLD.getDoubleValue(), lowest, highest)) {
                                continue;
                            }
                        }
                        overlay.add(sb.toString());
                    }
                }
            }

            double hypot = MathHelper.hypot(entity.getX() - librarian.getX(), entity.getZ() - librarian.getZ());
            double x = librarian.getX() + (entity.getX() - librarian.getX()) / hypot / 2;
            double z = librarian.getZ() + (entity.getZ() - librarian.getZ()) / hypot / 2;
            double y = librarian.getY() + 1.5 + 0.1 * overlay.size();

            for (String line : overlay)
            {
                RenderUtils.drawTextPlate(List.of(line), x, y, z, 0.02f);
                y -= 0.2;
            }
        }
    }
}
