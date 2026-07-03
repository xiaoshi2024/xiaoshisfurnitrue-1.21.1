package com.xiaoshi2022.xiaoshisfurnitrue.block;

import com.mojang.serialization.MapCodec;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WashboardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class WashboardBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape BASE_SHAPE = makeShape();
    private static final Map<Direction, VoxelShape> SHAPE_CACHE = new EnumMap<>(Direction.class);

    static {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction == Direction.NORTH) {
                SHAPE_CACHE.put(direction, BASE_SHAPE);
            } else {
                SHAPE_CACHE.put(direction, rotateShape(Direction.NORTH, direction, BASE_SHAPE));
            }
        }
    }

    public WashboardBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(WashboardBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WashboardBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, entity) -> {
            if (entity instanceof WashboardBlockEntity washboard) {
                washboard.tick(lvl, pos, st);
            }
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return SHAPE_CACHE.getOrDefault(facing, BASE_SHAPE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return SHAPE_CACHE.getOrDefault(facing, BASE_SHAPE);
    }

    // ===== 核心交互 =====
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getMainHandItem();

        // ===== 1. 空手右键：只播放 LAY 动画 =====
        if (heldItem.isEmpty()) {
            if (state.getValue(ACTIVE)) {
                return InteractionResult.CONSUME;
            }

            // 播放 LAY 动画
            level.setBlock(pos, state.setValue(ACTIVE, true), 3);
            level.playSound(null, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.6f, 0.9f);

            // 20 tick (1秒) 后停止动画
            level.scheduleTick(pos, this, 20);

            return InteractionResult.CONSUME;
        }

        // ===== 2. 手持物品：检查附魔 =====
        ItemEnchantments enchantments = heldItem.getEnchantments();
        if (enchantments.isEmpty()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§e这个物品没有附魔！"),
                    true
            );
            return InteractionResult.PASS;
        }

        // ===== 3. 附魔转经验 =====
        int experience = calculateExperience(enchantments, level);
        ItemStack resultItem = removeEnchantments(heldItem);

        ServerLevel serverLevel = (ServerLevel) level;

        // 播放音效
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.playSound(null, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.5f, 0.8f);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.3f, 0.5f);

        // 生成粒子效果
        for (int i = 0; i < 30; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetY = level.random.nextDouble() * 0.8;
            level.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 0.3 + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    (level.random.nextDouble() - 0.5) * 0.1,
                    0.1 + level.random.nextDouble() * 0.1,
                    (level.random.nextDouble() - 0.5) * 0.1);
        }

        // 生成水花粒子
        for (int i = 0; i < 10; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.8;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.8;
            level.addParticle(ParticleTypes.SPLASH,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 0.2,
                    pos.getZ() + 0.5 + offsetZ,
                    0, 0.2, 0);
        }

        // 生成经验球
        if (experience > 0) {
            ExperienceOrb.award(serverLevel,
                    player.position().add(0, 0.5, 0),
                    experience
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            String.format("§a获得了 §e%d §a经验值！", experience)
                    ),
                    true
            );
        }

        // 替换物品
        player.setItemInHand(InteractionHand.MAIN_HAND, resultItem);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a搓衣板使用完毕！"),
                true
        );

        // ===== 4. 爆炸消失（不掉落任何物品） =====
        destroyWashboard(level, pos);

        return InteractionResult.CONSUME;
    }

    /**
     * 定时任务：停止动画
     */
    public void tick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, false), 3);
        }
    }

    /**
     * 销毁搓衣板 - 爆炸效果，不掉落任何物品
     */
    private void destroyWashboard(Level level, BlockPos pos) {
        // 生成爆炸粒子效果
        for (int i = 0; i < 50; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = level.random.nextDouble() * 1.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
            level.addParticle(ParticleTypes.EXPLOSION,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 0.5 + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    0, 0, 0);
        }

        // 生成烟尘粒子
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
            double offsetY = level.random.nextDouble() * 1.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 0.5 + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    0, 0.1, 0);
        }

        // 播放爆炸音效
// 替代方案：使用 playSeededSound
        level.playSeededSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.0f, 0L);
        // 移除方块（不会掉落任何物品）
        level.removeBlock(pos, false);
    }

    /**
     * 计算附魔经验值
     */
    private int calculateExperience(ItemEnchantments enchantments, Level level) {
        int totalExp = 0;

        Registry<Enchantment> enchantmentRegistry = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT);

        for (var entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey().value();
            int levelValue = entry.getIntValue();

            int expPerLevel = 3 + (int) (enchantment.getWeight() * 1.5);
            totalExp += levelValue * expPerLevel;

            ResourceLocation key = enchantmentRegistry.getKey(enchantment);
            String name = key != null ? key.toString() : "unknown";

            if (name.contains("sharpness") || name.contains("protection") ||
                    name.contains("power") || name.contains("smite")) {
                totalExp += levelValue * 3;
            }
            if (name.contains("mending") || name.contains("unbreaking") ||
                    name.contains("efficiency") || name.contains("fortune")) {
                totalExp += levelValue * 4;
            }
            if (name.contains("silk_touch") || name.contains("looting") ||
                    name.contains("fire_aspect") || name.contains("knockback")) {
                totalExp += levelValue * 2;
            }
        }

        return Math.min(totalExp, 100);
    }

    /**
     * 移除附魔，保留物品其他属性
     */
    private ItemStack removeEnchantments(ItemStack stack) {
        // 附魔书 -> 普通书
        if (stack.is(Items.ENCHANTED_BOOK)) {
            return new ItemStack(Items.BOOK, stack.getCount());
        }

        // 创建新物品，只保留必要属性
        ItemStack result = new ItemStack(stack.getItem(), stack.getCount());

        // 保留耐久度
        if (stack.has(net.minecraft.core.component.DataComponents.MAX_DAMAGE)) {
            result.setDamageValue(stack.getDamageValue());
        }

        // 保留自定义名称
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));
        }

        // 保留物品描述
        if (stack.has(net.minecraft.core.component.DataComponents.LORE)) {
            result.set(net.minecraft.core.component.DataComponents.LORE,
                    stack.get(net.minecraft.core.component.DataComponents.LORE));
        }

        // 保留物品修饰
        if (stack.has(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS)) {
            result.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                    stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS));
        }

        return result;
    }

    // ===== 碰撞箱旋转 =====
    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        if (from == to) return shape;
        VoxelShape result = shape;
        int rotations = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < rotations; i++) {
            result = rotateShape90Clockwise(result);
        }
        return result;
    }

    private static VoxelShape rotateShape90Clockwise(VoxelShape shape) {
        final VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            rotated[0] = Shapes.join(rotated[0],
                    Shapes.box(
                            1 - maxZ, minY, minX,
                            1 - minZ, maxY, maxX
                    ), BooleanOp.OR);
        });
        return rotated[0];
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ===== 碰撞箱生成 =====
    private static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        double maxCoord = 14.0;
        double minX = clamp((-0.988 / maxCoord) + 0.5);
        double maxX = clamp((0.1 / maxCoord) + 0.5);
        double minY = clamp(0.0 / maxCoord);
        double maxY = clamp(13.377 / maxCoord);
        double minZ = clamp((-2.793 / maxCoord) + 0.5);
        double maxZ = clamp((2.695 / maxCoord) + 0.5);
        shape = Shapes.join(shape, Shapes.box(minX, minY, minZ, maxX, maxY, maxZ), BooleanOp.OR);
        return shape;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}