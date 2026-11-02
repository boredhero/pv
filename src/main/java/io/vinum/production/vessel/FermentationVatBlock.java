package io.vinum.production.vessel;

import com.mojang.serialization.MapCodec;
public final class FermentationVatBlock extends FermentationVesselBlock {
    public static final MapCodec<FermentationVatBlock> CODEC = simpleCodec(FermentationVatBlock::new);
    public FermentationVatBlock(Properties properties) { super(properties); }
    @Override public MapCodec<FermentationVatBlock> codec() { return CODEC; }
}
