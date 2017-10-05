package com.example.fn;

import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

import java.util.concurrent.TimeUnit;

public class Wait extends State {
    String timestamp;
    Integer seconds;
    String inputPath;
    String outputPath;
    String next;
    Boolean end;

    @Override
    FlowFuture<Machine> transition(Machine machine) {
        FlowFuture<Void> f = Flows.currentFlow().delay(seconds, TimeUnit.SECONDS);

        if (end != null && end) {
            return f.thenApply(v -> machine);
        } else {
            machine.currentState = next;
            return f.thenApply(v -> machine).thenCompose(States::transition);
        }
    }
}
