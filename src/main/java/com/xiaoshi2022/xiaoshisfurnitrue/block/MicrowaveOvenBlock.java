package com.xiaoshi2022.xiaoshisfurnitrue.block;

import com.mojang.serialization.MapCodec;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.MicrowaveOvenBlockEntity;
import com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class MicrowaveOvenBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_FOOD = BooleanProperty.create("has_food");
    public static final BooleanProperty IS_COOKING = BooleanProperty.create("is_cooking");
    public static final IntegerProperty COOK_TIME = IntegerProperty.create("cook_time", 0, 20);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static final VoxelShape SHAPE = Shapes.box(
            0.05, 0.0, 0.05,
            0.95, 0.75, 0.95
    );

    public MicrowaveOvenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_FOOD, false)
                .setValue(IS_COOKING, false)
                .setValue(COOK_TIME, 0)
                .setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MicrowaveOvenBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_FOOD, IS_COOKING, COOK_TIME, OPEN);
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
        return new MicrowaveOvenBlockEntity(pos, state);
    }

    @javax.annotation.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MICROWAVE_OVEN_BLOCK_ENTITY.get(), MicrowaveOvenBlockEntity::tick);
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
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof MicrowaveOvenBlockEntity ovenEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        // 正在烹饪时禁止交互
        if (state.getValue(IS_COOKING)) {
            return InteractionResult.PASS;
        }

        boolean isOpen = state.getValue(OPEN);

        // ====== 门是关的 ======
        if (!isOpen) {
            // 有食物且非Shift -> 开始烹饪
            if (state.getValue(HAS_FOOD) && !player.isShiftKeyDown()) {
                ovenEntity.startCooking();
                return InteractionResult.CONSUME;
            }

            // 否则开门
            openDoor(level, pos, state);
            return InteractionResult.CONSUME;
        }

        // ====== 门是开的 ======

        // 1. Shift + 右键：取出食物（如果有的话）
        if (player.isShiftKeyDown()) {
            if (state.getValue(HAS_FOOD)) {
                ItemStack food = ovenEntity.getFood();
                if (!food.isEmpty()) {
                    ovenEntity.setFoodInternal(ItemStack.EMPTY);
                    level.setBlock(pos, state
                            .setValue(HAS_FOOD, false)
                            .setValue(COOK_TIME, 0), 3);
                    ovenEntity.setFood(ItemStack.EMPTY);
                    if (!player.getInventory().add(food)) {
                        player.drop(food, false);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 1.0f);
                    return InteractionResult.CONSUME;
                }
            }
            // Shift + 右键但没有食物：关门
            closeDoor(level, pos, state);
            return InteractionResult.CONSUME;
        }

        // ====== 非 Shift 键 ======

        // 2. 拿着可烹饪食物 -> 放入（空手或不可烹饪物品都不进入此分支）
        if (isCookable(level, heldItem) && !state.getValue(HAS_FOOD)) {
            ItemStack food = heldItem.copyWithCount(1);
            if (!player.isCreative()) {
                heldItem.shrink(1);
            }
            level.setBlock(pos, state
                    .setValue(HAS_FOOD, true)
                    .setValue(COOK_TIME, 0), 3);
            ovenEntity.setFood(food);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.5f, 1.0f);
            return InteractionResult.CONSUME;
        }

        // 3. 空手 或 拿着不可烹饪物品 -> 关门
        closeDoor(level, pos, state);
        return InteractionResult.CONSUME;
    }

    private void openDoor(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(OPEN, true), 3);
        level.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    private void closeDoor(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(OPEN, false), 3);
        level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 1.0f);
    }

    private boolean isCookable(Level level, ItemStack stack) {
        if (stack.isEmpty() || level == null) return false;

        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                .isPresent();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MicrowaveOvenBlockEntity be) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getFood());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}