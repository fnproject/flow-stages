package com.example.fn;

import com.fnproject.fn.api.flow.FlowFuture;

import java.io.Serializable;

public abstract class State implements Serializable {
    String comment;
    abstract FlowFuture<Machine> transition(Machine machine);
}
