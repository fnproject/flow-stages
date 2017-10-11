package com.fnproject.fn.flow.stages;

import com.fnproject.fn.api.flow.FlowFuture;

import java.io.Serializable;

public abstract class State implements Serializable {
    String comment;

    public State(String comment) {
        this.comment = comment;
    }

    public abstract FlowFuture<Machine> transition(Machine machine);
}
