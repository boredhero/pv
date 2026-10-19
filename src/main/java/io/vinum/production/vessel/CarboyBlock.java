package io.vinum.production.vessel;

import com.mojang.serialization.MapCodec;
public final class CarboyBlock extends FermentationVesselBlock {
    public static final MapCodec<CarboyBlock> CODEC = simpleCodec(CarboyBlock::new);
    public CarboyBlock(Properties properties) { super(properties); }
    @Override public MapCodec<CarboyBlock> codec() { return CODEC; }
}
