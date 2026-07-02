package com.xiaoshi2022.xiaoshisfurnitrue.item;

import com.xiaoshi2022.xiaoshisfurnitrue.block.WashboardBlock;
import com.xiaoshi2022.xiaoshisfurnitrue.render.WashboardRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class WashboardItem extends BlockItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    private static final RawAnimation LAY = RawAnimation.begin().thenPlay("lay");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WashboardItem(WashboardBlock block, Item.Properties properties) {
        super(block, properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<WashboardItem> controller = new AnimationController<WashboardItem>(this, "controller", 0, this::predicate);
        controller.triggerableAnim("lay", LAY);
        controllers.add(controller);
    }

    private PlayState predicate(AnimationState<WashboardItem> state) {
        state.setAnimation(IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private WashboardRenderer renderer;

            @Override
            public GeoItemRenderer<WashboardItem> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new WashboardRenderer();
                return this.renderer;
            }
        });
    }
}
