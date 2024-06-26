package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.math.*;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.EntitiesDataStorage;
import fi.dy.masa.minihud.mixin.IMixinMerchantEntity;

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
        List<VillagerEntity> librarians = world != null ? world.getEntitiesByClass(VillagerEntity.class, box, villager -> villager.getVillagerData().getProfession() == VillagerProfession.LIBRARIAN) : List.of();

        for (VillagerEntity librarian : librarians)
        {
            if (librarian.isClient())
            {
                EntitiesDataStorage.getInstance().requestEntity(librarian.getId());
            }
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
                    for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : tradeOffer.getSellItem().getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, null).getEnchantmentEntries())
                    {
                        if (entry == null)
                        {
                            continue;
                        }
                        StringBuilder sb = new StringBuilder();

                        if (entry.getKey().value().getMaxLevel() == entry.getIntValue())
                        {
                            sb.append(GuiBase.TXT_GOLD);
                        }
                        else if (Configs.Generic.VILLAGER_OFFER_HIGHEST_LEVEL_ONLY.getBooleanValue())
                        {
                            continue;
                        }
                        sb.append(Enchantment.getName(entry.getKey(), entry.getIntValue()).getString());
                        sb.append(GuiBase.TXT_RST);

                        if (tradeOffer.getFirstBuyItem().item().value() == Items.EMERALD)
                        {
                            int emeraldCost = tradeOffer.getFirstBuyItem().count();
                            int lowest = 2 + 3 * entry.getIntValue();
                            int highest = 6 + 13 * entry.getIntValue();
                            int maxCost = lowest + 4 + entry.getIntValue() * 10;

                            if (entry.getKey().isIn(EnchantmentTags.DOUBLE_TRADE_PRICE))
                            {
                                lowest *= 2;
                                highest *= 2;
                                maxCost *= 2;
                            }

                            sb.append(" ");
                            if (emeraldCost <= (maxCost - lowest) / 3 + lowest)
                            {
                                sb.append(GuiBase.TXT_GREEN);
                            }
                            else if (emeraldCost <= (maxCost - lowest) / 3 * 2 + lowest)
                            {
                                sb.append(GuiBase.TXT_WHITE);
                            }
                            else
                            {
                                sb.append(GuiBase.TXT_RED);
                            }

                            // Can add additional formatting if you like, but this works as is
                            sb.append(emeraldCost);
                            sb.append(GuiBase.TXT_RST);

                            if (tradeOffer.getFirstBuyItem().count() > MathHelper.lerp(Configs.Generic.VILLAGER_OFFER_PRICE_THRESHOLD.getDoubleValue(), lowest, highest))
                            {
                                continue;
                            }
                        }
                        overlay.add(sb.toString());
                    }
                }
            }

            double hypot = MathHelper.hypot(entity.getX() - librarian.getX(), entity.getZ() - librarian.getZ());
            double distance = 0.8;
            double x = librarian.getX() + (entity.getX() - librarian.getX()) / hypot * distance;
            double z = librarian.getZ() + (entity.getZ() - librarian.getZ()) / hypot * distance;
            double y = librarian.getY() + (1.5 + 0.1) * overlay.size();
            // TODO This y calculation needs checking when they have more than 1 trade, in particular

            // Render the overlay at its job site, this is useful in trading halls
            Optional<GlobalPos> jobSite = librarian.getBrain().getOptionalMemory(MemoryModuleType.JOB_SITE);
            if (jobSite != null && jobSite.isPresent())
            {
                BlockPos pos = jobSite.get().pos();
                if (librarian.getPos().distanceTo(pos.toCenterPos()) < 1.7)
                {
                    x = pos.getX() + 0.5;
                    z = pos.getZ() + 0.5;
                }
            }

            for (String line : overlay)
            {
                RenderUtils.drawTextPlate(List.of(line), x, y, z, 0.02f);
                y -= 0.2;
            }
        }
    }
}
