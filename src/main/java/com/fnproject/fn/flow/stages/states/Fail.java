package com.fnproject.fn.flow.stages.states;

import com.fnproject.fn.flow.stages.Machine;
import com.fnproject.fn.flow.stages.State;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;

public class Fail extends State {
    String error;
    String cause;

    public Fail(String comment, String error, String cause) {
        super(comment);
        this.error = error;
        this.cause = cause;
    }

    @Override
    public FlowFuture<Machine> transition(Machine machine) {
        System.out.println("Failing state machine with error " + error + " and cause " + cause);
        return Flows.currentFlow().completedValue(machine);
    }
}
