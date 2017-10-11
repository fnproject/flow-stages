package com.fnproject.fn.flow.stages.states;

import com.fnproject.fn.flow.stages.Machine;
import com.fnproject.fn.flow.stages.State;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

public class Succeed extends State {
    String inputPath;
    String outputPath;

    public Succeed(String comment, String inputPath, String outputPath) {
        super(comment);
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    @Override
    public FlowFuture<Machine> transition(Machine machine) {
        return Flows.currentFlow().completedValue(machine);
    }
}
