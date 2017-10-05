package com.example.fn;

import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

public class Succeed extends State {
    String inputPath;
    String outputPath;

    @Override
    FlowFuture<Machine> transition(Machine machine) {
        return Flows.currentFlow().completedValue(machine);
    }
}
