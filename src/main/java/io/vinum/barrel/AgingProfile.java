package io.vinum.barrel;

import java.util.List;
import java.util.function.LongSupplier;

/** Declarative maturation names. Aging affects presentation, never a drink's alcohol dose. */
public record AgingProfile(List<Stage> stages) {
    public AgingProfile { stages = List.copyOf(stages); }
    public record Stage(LongSupplier minimumTicks, String translationKey) {}

    public String stage(long ageTicks) {
        String result = "tooltip.vinum.maturation.unaged";
        long selected = -1;
        for (Stage stage : stages) {
            long minimum = stage.minimumTicks().getAsLong();
            if (ageTicks >= minimum && minimum >= selected) {
                result = stage.translationKey();
                selected = minimum;
            }
        }
        return result;
    }
}
