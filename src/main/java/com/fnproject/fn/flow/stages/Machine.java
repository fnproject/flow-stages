package com.fnproject.fn.flow.stages;

import java.io.Serializable;
import java.util.Map;

public class Machine implements Serializable {
    public String currentState;
    public Object document;

    String comment;
    String startAt;

    Map<String, State> states;
    String version;
    String timeoutSeconds;
}
