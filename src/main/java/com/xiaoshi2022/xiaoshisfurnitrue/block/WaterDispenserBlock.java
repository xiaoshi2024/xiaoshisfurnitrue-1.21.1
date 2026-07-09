package com.xiaoshi2022.xiaoshisfurnitrue.block;

import com.mojang.serialization.MapCodec;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WaterDispenserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

import javax.annotation.Nullable;

public class WaterDispenserBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_WATER = BooleanProperty.create("has_water");
    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, 3);
    public static final BooleanProperty DISPENSING = BooleanProperty.create("dispensing");
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty HEATING = BooleanProperty.create("heating");
    public static final IntegerProperty TEMPERATURE = IntegerProperty.create("temperature", 0, 3);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public static final int DISPENSING_ANIM_TICKS = 15;
    public static final int MAX_LEVEL = 3;
    public static final int HEATING_INTERVAL = 20;

    private static final VoxelShape SHAPE = Shapes.box(0.1, 0.0, 0.1, 0.9, 1.0, 0.9);

    public WaterDispenserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_WATER, false)
                .setValue(WATER_LEVEL, 0)
                .setValue(DISPENSING, false)
                .setValue(OPEN, false)
                .setValue(HEATING, false)
                .setValue(TEMPERATURE, 0)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(WaterDispenserBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_WATER, WATER_LEVEL, DISPENSING, OPEN, HEATING, TEMPERATURE, POWERED);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(DISPENSING)) {
            level.setBlock(pos, state.setValue(DISPENSING, false), 3);
        }

        // 加热逻辑：必须有红石信号且有水才加热
        if (state.getValue(POWERED) && state.getValue(HAS_WATER)) {
            if (!state.getValue(HEATING)) {
                level.setBlock(pos, state.setValue(HEATING, true), 3);
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.5f, 1.0f);
            }

            int temp = state.getValue(TEMPERATURE);
            if (temp < 3) {
                int newTemp = temp + 1;
                level.setBlock(pos, state.setValue(TEMPERATURE, newTemp), 3);

                if (newTemp >= 2) {
                    double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
                    double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6;
                    level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 1.0, z, 0, 0.02, 0);
                }

                // ===== 水温达到沸腾时净化水质 =====
                if (newTemp == 3) {
                    level.playSound(null, pos, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.5f);

                    // 获取 BlockEntity 并执行净化
                    BlockEntity entity = level.getBlockEntity(pos);
                    if (entity instanceof WaterDispenserBlockEntity dispenser) {
                        dispenser.onHeatingComplete();
                    }
                }
            }
            level.scheduleTick(pos, this, HEATING_INTERVAL);
        } else {
            if (state.getValue(HEATING)) {
                level.setBlock(pos, state.setValue(HEATING, false), 3);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
            }
            if (state.getValue(HAS_WATER) && state.getValue(TEMPERATURE) > 0 && !state.getValue(HEATING)) {
                level.setBlock(pos, state.setValue(TEMPERATURE, state.getValue(TEMPERATURE) - 1), 3);
                level.scheduleTick(pos, this, HEATING_INTERVAL * 2);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HEATING) && state.getValue(HAS_WATER) && state.getValue(TEMPERATURE) >= 2) {
            if (random.nextInt(5) == 0) {
                double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
                double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
                level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 1.0, z, 0, 0.02 + random.nextDouble() * 0.02, 0);
            }

            if (state.getValue(TEMPERATURE) == 3 && random.nextInt(3) == 0) {
                double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
                double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
                level.addParticle(ParticleTypes.BUBBLE, x, pos.getY() + 0.2, z, 0, 0.05, 0);
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HEATING, false)
                .setValue(TEMPERATURE, 0)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterDispenserBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != state.getValue(POWERED)) {
                BlockState newState = state.setValue(POWERED, powered);
                if (!powered) {
                    newState = newState.setValue(HEATING, false);
                } else {
                    level.scheduleTick(pos, this, 1);
                }
                level.setBlock(pos, newState, 3);
            }
        }
    }

    // ===== 交互 =====
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof WaterDispenserBlockEntity dispenserEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        // ===== 1. 空手右键：显示状态信息 =====
        if (heldItem.isEmpty()) {
            boolean powered = state.getValue(POWERED);
            boolean heating = state.getValue(HEATING);
            int temp = state.getValue(TEMPERATURE);
            int levelVal = state.getValue(WATER_LEVEL);
            int purity = dispenserEntity.getWaterPurity();

            String status = powered ? "§a已通电" : "§c未通电";
            String heatStatus = heating ? "§6加热中" : "§7待机";
            String tempStatus = switch (temp) {
                case 0 -> "§b冷水";
                case 1 -> "§a温水";
                case 2 -> "§6热水";
                case 3 -> "§c沸腾";
                default -> "§7未知";
            };
            String purityStatus = switch (purity) {
                case 0 -> "§4脏水";
                case 1 -> "§6微脏";
                case 2 -> "§a可接受";
                case 3 -> "§b纯净";
                default -> "§7未知";
            };

            player.displayClientMessage(
                    Component.literal(String.format(
                            "§7[饮水机] %s | %s | %s | %s | 水量: %d/3",
                            status, heatStatus, tempStatus, purityStatus, levelVal
                    )),
                    true
            );
            return InteractionResult.CONSUME;
        }

        // ===== 2. 检查是否拿着水桶 - 加水（一桶加满 = 3级） =====
        if (heldItem.is(Items.WATER_BUCKET)) {
            int currentLevel = state.getValue(WATER_LEVEL);

            if (currentLevel >= MAX_LEVEL) {
                player.displayClientMessage(Component.literal("§c饮水机已满！"), true);
                return InteractionResult.CONSUME;
            }

            dispenserEntity.fillFullBucket(heldItem);

            BlockState newState = state
                    .setValue(WATER_LEVEL, MAX_LEVEL)
                    .setValue(HAS_WATER, true);
            level.setBlock(pos, newState, 3);

            if (!player.isCreative()) {
                heldItem.shrink(1);
                player.getInventory().add(new ItemStack(Items.BUCKET));
            }

            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);

            player.displayClientMessage(
                    Component.literal(String.format("§a已加满水！(%d/%d)", MAX_LEVEL, MAX_LEVEL)),
                    true
            );

            if (state.getValue(POWERED)) {
                level.scheduleTick(pos, this, 1);
            }

            return InteractionResult.CONSUME;
        }

        // ===== 3. 检查是否拿着玻璃瓶 - 接水（一次取1级） =====
        if (heldItem.is(Items.GLASS_BOTTLE)) {
            return dispenserEntity.dispenseWaterToBottle(player, heldItem);
        }

        // ===== 4. 通过反射支持其他容器接水（软联动） =====
        InteractionResult thirstResult = dispenserEntity.tryDispenseToContainer(player, heldItem, level, pos);
        if (thirstResult != InteractionResult.PASS) {
            return thirstResult;
        }

        // ===== 5. 使用 FluidUtil 处理其他液体容器交互 =====
        boolean interacted = FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, level, pos, hit.getDirection());

        if (interacted) {
            dispenserEntity.updateStateFromTank();

            BlockState currentState = level.getBlockState(pos);
            if (currentState.getValue(HAS_WATER)) {
                boolean isHot = currentState.getValue(TEMPERATURE) >= 2;
                level.setBlock(pos, currentState.setValue(DISPENSING, true), 3);
                level.scheduleTick(pos, this, DISPENSING_ANIM_TICKS);

                if (isHot) {
                    level.playSound(null, pos, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.0f);
                }
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        int levelVal = state.getValue(WATER_LEVEL);
        return (int) (levelVal / (float) MAX_LEVEL * 15);
    }
}