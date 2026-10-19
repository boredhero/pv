package io.vinum.test.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Reusable, tick driven client actions. Conditions must only read client state. */
public interface ClientScenario {
    String name();
    List<Step> steps(ClientTestContext context);

    record Step(String name, Runnable start, BooleanSupplier complete, int timeoutTicks) {}

    final class Sequence {
        private final List<Step> steps = new ArrayList<>();
        public Sequence await(String name, Runnable start, BooleanSupplier complete, int timeoutTicks) {
            steps.add(new Step(name, start, complete, timeoutTicks));
            return this;
        }
        public Sequence action(String name, Runnable action) {
            return await(name, action, () -> true, 100);
        }
        public List<Step> build() { return List.copyOf(steps); }
    }
}
