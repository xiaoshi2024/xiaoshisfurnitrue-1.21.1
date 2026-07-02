package com.xiaoshi2022.xiaoshisfurnitrue.block;

import com.mojang.serialization.MapCodec;
import com.xiaoshi2022.xiaoshisfurnitrue.block.entity.WashboardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

    // 从 Blockbench 模型生成的碰撞箱（立起来的长方体）
    private static final VoxelShape BASE_SHAPE = makeShape();

    // 缓存不同朝向的碰撞箱 - 使用 EnumMap 更高效
    private static final Map<Direction, VoxelShape> SHAPE_CACHE = new EnumMap<>(Direction.class);

    static {
        // 预计算所有朝向的碰撞箱
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

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WashboardBlockEntity(pos, state);
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

    /**
     * 旋转碰撞箱到指定朝向
     */
    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        if (from == to) return shape;

        VoxelShape result = shape;
        // 计算需要旋转的次数（每次90度）
        int rotations = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;

        for (int i = 0; i < rotations; i++) {
            result = rotateShape90Clockwise(result);
        }
        return result;
    }

    /**
     * 绕Y轴顺时针旋转90度
     */
    private static VoxelShape rotateShape90Clockwise(VoxelShape shape) {
        final VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            // 绕Y轴顺时针旋转90度: (x,z) -> (1-z, x)
            rotated[0] = Shapes.join(rotated[0],
                    Shapes.box(
                            1 - maxZ, minY, minX,
                            1 - minZ, maxY, maxX
                    ), BooleanOp.OR);
        });
        return rotated[0];
    }

    // ===== 空手右键交互 =====
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        boolean isActive = state.getValue(ACTIVE);
        boolean newState = !isActive;

        // 更新方块状态（BlockState 由 Minecraft 自动同步到客户端，动画控制器直接读此属性）
        level.setBlock(pos, state.setValue(ACTIVE, newState), 3);

        // 播放音效
        level.playSound(null, pos,
                newState ? SoundEvents.WOOL_PLACE : SoundEvents.WOOL_BREAK,
                SoundSource.BLOCKS, 0.5f, 1.0f);

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

    // ===== 从 Blockbench 模型生成的碰撞箱 =====
    private static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();

        // 模型的实际尺寸（从 Blockbench 导出）
        // 使用更精确的归一化
        double maxCoord = 14.0; // 模型最大坐标值

        // 计算所有部件的边界框
        // 从模型中提取的主要部件坐标范围：
        // X: -0.988 ~ 0.1 (宽约1.088)
        // Y: 0 ~ 13.377 (高约13.377)
        // Z: -2.793 ~ 2.695 (深约5.488)

        double minX = (-0.988 / maxCoord) + 0.5;
        double maxX = (0.1 / maxCoord) + 0.5;
        double minY = 0.0 / maxCoord;
        double maxY = 13.377 / maxCoord;
        double minZ = (-2.793 / maxCoord) + 0.5;
        double maxZ = (2.695 / maxCoord) + 0.5;

        // 确保所有值在 0-1 范围内
        minX = clamp(minX);
        maxX = clamp(maxX);
        minY = clamp(minY);
        maxY = clamp(maxY);
        minZ = clamp(minZ);
        maxZ = clamp(maxZ);

        // 添加主体碰撞箱
        shape = Shapes.join(shape, Shapes.box(minX, minY, minZ, maxX, maxY, maxZ), BooleanOp.OR);

        // 如果需要更精确的碰撞箱，可以添加更多部件
        // 这里使用整体边界框已经足够

        return shape;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}