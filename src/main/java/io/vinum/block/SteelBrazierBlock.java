package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.util.VoxelShapeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class SteelBrazierBlock extends SteelComponentBlock {
    public static final MapCodec<SteelBrazierBlock> CODEC = simpleCodec(SteelBrazierBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SteelBrazierBlock(Properties properties) {
        super(properties, VoxelShapeHelper.addShapes(box(1, 0, 1, 5, 8, 5), box(11, 0, 1, 15, 8, 5),
            box(1, 0, 11, 5, 8, 15), box(11, 0, 11, 15, 8, 15), box(1, 8, 1, 15, 15, 15)));
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    public MapCodec<SteelBrazierBlock> codec() { return CODEC; }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        boolean charge = stack.is(Items.FIRE_CHARGE);
        if ((!flint && !charge) || state.getValue(WATERLOGGED) || (charge && state.getValue(LIT))) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            boolean light = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, light), UPDATE_ALL);
            level.playSound(null, pos, light ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (light && !player.getAbilities().instabuild) {
                if (flint) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                else stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        if (!state.getValue(WATERLOGGED) && fluid.getType() == Fluids.WATER) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(WATERLOGGED, true).setValue(LIT, false), UPDATE_ALL);
                level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
            }
            return true;
        }
        return false;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(LIT) && entity instanceof LivingEntity && !entity.fireImmune()) {
            entity.hurt(level.damageSources().campfire(), 0.5F);
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && (projectile.isOnFire() || projectile instanceof Fireball)
                && projectile.mayInteract(level, hit.getBlockPos()) && !state.getValue(LIT) && !state.getValue(WATERLOGGED)) {
            level.setBlock(hit.getBlockPos(), state.setValue(LIT, true), UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0.04, 0);
        if (random.nextInt(10) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5F + random.nextFloat(), 0.6F + random.nextFloat() * 0.7F, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }
}
