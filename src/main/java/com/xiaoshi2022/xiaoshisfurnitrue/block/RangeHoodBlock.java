package com.xiaoshi2022.xiaoshisfurnitrue.block;

import com.mojang.serialization.MapCodec;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.RangeHoodBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class RangeHoodBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Shapes.box(0.625, 0.0, 0.2, 1.0, 1.0, 0.8),
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.25, 1.0)
    );
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Shapes.box(0.0, 0.0, 0.2, 0.375, 1.0, 0.8),
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.25, 1.0)
    );
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Shapes.box(0.2, 0.0, 0.625, 0.8, 1.0, 1.0),
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.25, 1.0)
    );
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Shapes.box(0.2, 0.0, 0.0, 0.8, 1.0, 0.375),
            Shapes.box(0.0, 0.0, 0.0, 1.0, 0.25, 1.0)
    );

    public RangeHoodBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(DOWN, false)
                .setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(RangeHoodBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, DOWN, OPEN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.setValue(DOWN, isRangeHoodBelow(level, pos, state.getValue(FACING)));
    }

    private static boolean isRangeHoodBelow(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState downState = level.getBlockState(pos.below());
        return downState.getBlock() instanceof RangeHoodBlock
                && downState.getValue(FACING) == facing;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            BlockState below = level.getBlockState(pos.below());
            if (below.getBlock() instanceof RangeHoodBlock) {
                level.setBlock(pos, state.setValue(DOWN, below.getValue(FACING) == state.getValue(FACING)), 3);
            }
            BlockState above = level.getBlockState(pos.above());
            if (above.getBlock() instanceof RangeHoodBlock) {
                level.setBlock(pos.above(), above.setValue(DOWN,
                        above.getValue(FACING) == state.getValue(FACING)), 3);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state.setValue(DOWN, isRangeHoodBelow(level, pos, state.getValue(FACING)));
        }
        if (direction == Direction.UP && neighborState.getBlock() instanceof RangeHoodBlock) {
            level.setBlock(pos.above(), neighborState.setValue(DOWN,
                    neighborState.getValue(FACING) == state.getValue(FACING)), 3);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RangeHoodBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType,
                com.xiaoshi2022.xiaoshisfurnitrue.register.ModBlockEntities.RANGE_HOOD_BLOCK_ENTITY.get(),
                RangeHoodBlockEntity::tick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 用最新状态而非参数快照，避免 OFF_HAND/MAIN_HAND 连续两次调用传入同一旧快照
        BlockState current = level.getBlockState(pos);

        if (player.isShiftKeyDown()) {
            boolean open = current.getValue(OPEN);
            level.setBlock(pos, current.setValue(OPEN, !open), 3);
            level.playSound(null, pos,
                    open ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN,
                    SoundSource.BLOCKS, 0.5f, 1.0f);
        } else {
            boolean powered = current.getValue(POWERED);
            level.setBlock(pos, current.setValue(POWERED, !powered), 3);
            level.playSound(null, pos,
                    powered ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND,
                    SoundSource.BLOCKS, 0.5f, 1.0f);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
