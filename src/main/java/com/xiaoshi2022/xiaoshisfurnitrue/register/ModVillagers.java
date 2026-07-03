package com.xiaoshi2022.xiaoshisfurnitrue.register;

import com.google.common.collect.ImmutableSet;
import com.xiaoshi2022.xiaoshisfurnitrue.XiaoshisFurnitrue;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ModVillagers {
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, XiaoshisFurnitrue.MODID);

    public static final TagKey<Item> FURNITURE_TAG =
            TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(XiaoshisFurnitrue.MODID, "furniture"));

    private static final ResourceKey<PoiType> FURNITURE_POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
                    ResourceLocation.fromNamespaceAndPath(XiaoshisFurnitrue.MODID, "furniture_merchant"));

    public static final Supplier<VillagerProfession> FURNITURE_MERCHANT = VILLAGER_PROFESSIONS.register("furniture_merchant",
            () -> new VillagerProfession(
                    "furniture_merchant",
                    holder -> holder.is(FURNITURE_POI_KEY),
                    holder -> holder.is(FURNITURE_POI_KEY),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CARTOGRAPHER
            ));

    // ===== 自定义交易类：使用标签收购家具 =====
    private static class BuyFurnitureTrade implements VillagerTrades.ItemListing {
        private final int emeraldCost;
        private final int maxUses;
        private final int xp;

        public BuyFurnitureTrade(int emeraldCost, int maxUses, int xp) {
            this.emeraldCost = emeraldCost;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Override
        public MerchantOffer getOffer(net.minecraft.world.entity.Entity trader, net.minecraft.util.RandomSource rand) {
            // 从标签中随机选一个家具
            var holder = trader.level().registryAccess()
                    .registryOrThrow(Registries.ITEM)
                    .getTag(FURNITURE_TAG)
                    .flatMap(tag -> tag.getRandomElement(rand))
                    .orElse(null);

            if (holder == null) return null;

            ItemStack furniture = new ItemStack(holder.value(), 1);
            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, emeraldCost),
                    furniture,
                    maxUses, xp, 0.05F
            );
        }
    }

    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != FURNITURE_MERCHANT.get()) {
            return;
        }

        List<VillagerTrades.ItemListing> level1Trades = event.getTrades().get(1);
        List<VillagerTrades.ItemListing> level2Trades = event.getTrades().get(2);
        List<VillagerTrades.ItemListing> level3Trades = event.getTrades().get(3);
        List<VillagerTrades.ItemListing> level4Trades = event.getTrades().get(4);
        List<VillagerTrades.ItemListing> level5Trades = event.getTrades().get(5);

        // ===== 等级 1 =====
        // 收购：2 绿宝石 → 随机家具（使用标签）
        level1Trades.add(new BuyFurnitureTrade(2, 16, 5));
        // 出售：12 木板 → 1 绿宝石
        level1Trades.add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.OAK_PLANKS, 12),
                new ItemStack(Items.EMERALD, 1),
                16, 2, 0.05F));

        // ===== 等级 2 =====
        // 收购：3 绿宝石 → 随机家具
        level2Trades.add(new BuyFurnitureTrade(3, 12, 8));
        // 出售：6 铁锭 → 1 绿宝石
        level2Trades.add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.IRON_INGOT, 6),
                new ItemStack(Items.EMERALD, 1),
                12, 5, 0.05F));

        // ===== 等级 3 =====
        // 收购：5 绿宝石 → 随机家具
        level3Trades.add(new BuyFurnitureTrade(5, 8, 10));
        // 出售：8 红石 → 1 绿宝石
        level3Trades.add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.REDSTONE, 8),
                new ItemStack(Items.EMERALD, 1),
                8, 5, 0.05F));

        // ===== 等级 4 =====
        // 收购：4 绿宝石 → 随机家具
        level4Trades.add(new BuyFurnitureTrade(4, 8, 12));
        // 出售：6 玻璃 → 1 绿宝石
        level4Trades.add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.GLASS, 6),
                new ItemStack(Items.EMERALD, 1),
                8, 5, 0.05F));

        // ===== 等级 5 =====
        // 收购：8 绿宝石 → 2 个随机家具（打包）
        level5Trades.add((trader, rand) -> {
            var holder = trader.level().registryAccess()
                    .registryOrThrow(Registries.ITEM)
                    .getTag(FURNITURE_TAG)
                    .flatMap(tag -> tag.getRandomElement(rand))
                    .orElse(null);
            if (holder == null) return null;

            var holder2 = trader.level().registryAccess()
                    .registryOrThrow(Registries.ITEM)
                    .getTag(FURNITURE_TAG)
                    .flatMap(tag -> tag.getRandomElement(rand))
                    .orElse(null);
            if (holder2 == null) return null;

            return new MerchantOffer(
                    new ItemCost(Items.EMERALD, 8),
                    Optional.of(new ItemCost(holder.value(), 1)),
                    new ItemStack(holder2.value(), 1),
                    4, 20, 0.05F
            );
        });
        // 出售：3 钻石 → 4 绿宝石
        level5Trades.add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.DIAMOND, 3),
                new ItemStack(Items.EMERALD, 4),
                4, 15, 0.05F));
    }
}