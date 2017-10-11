package com.fnproject.fn.flow.stages.states;

import com.fnproject.fn.flow.stages.Machine;
import com.fnproject.fn.flow.stages.Stages;
import com.fnproject.fn.flow.stages.State;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

import java.util.concurrent.TimeUnit;

public class Wait extends State {
    public String timestamp;
    public Integer seconds;
    public String inputPath;
    public String outputPath;
    public String next;
    public Boolean end;

    public Wait(String comment) {
        super(comment);
    }

    @Override
    public FlowFuture<Machine> transition(Machine machine) {
        FlowFuture<Void> f = Flows.currentFlow().delay(seconds, TimeUnit.SECONDS);

        if (end != null && end) {
            return f.thenApply(v -> machine);
        } else {
            machine.currentState = next;
            return f.thenApply(v -> machine).thenCompose(Stages::transition);
        }
    }
}
