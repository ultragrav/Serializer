package net.ultragrav.serializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConcurrencyStepper {
    private final List<CompletableFuture<Void>> stepList = new ArrayList<>();

    public ConcurrencyStepper(int numSteps) {
        for (int i = 0; i < numSteps; i++) {
            stepList.add(new CompletableFuture<>());
        }
    }

    /**
     * This will wait for the previous step;
     */
    public void step(int stepNum) {
        if (stepNum - 1 < 0) {
            return;
        }
        stepList.get(stepNum - 1).join();
    }

    public void complete(int stepNum) {
        stepList.get(stepNum).complete(null);
    }
}
