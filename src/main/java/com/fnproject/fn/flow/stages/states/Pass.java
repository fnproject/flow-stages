package com.fnproject.fn.flow.stages.states;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.flow.stages.Machine;
import com.fnproject.fn.flow.stages.Stages;
import com.fnproject.fn.flow.stages.State;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;

public class Pass extends State {
    public String inputPath;
    public String outputPath;
    public Object result;
    public String resultPath;
    public String next;
    public Boolean end;
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public Pass(String comment) {
        super(comment);
        this.next = next;

        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.result = result;
        this.resultPath = resultPath;
    }

    public Pass(String comment, boolean end, String inputPath, String outputPath, String result, String resultPath) {
        super(comment);
        this.end = end;

        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.result = result;
        this.resultPath = resultPath;
    }

    @Override
    public FlowFuture<Machine> transition(Machine machine) {
        Flow flow = Flows.currentFlow();
        if(end != null && end) {
            return flow.completedValue(machine);
        } else {
            System.out.println("Transitioning from state " + machine.currentState + " to state " + next);

            if (next != null) {
                machine.currentState = next;
            }

            try {
                Object document = machine.document;

                if(result != null) {
                    if(resultPath != null) {
                        String s = objectMapper.writeValueAsString(machine.document);
                        // This only updates existing values, doesn't add fields
                        // TODO: It is supposed to set them if they don't exist
                        String s2 = JsonPath.parse(s).set(resultPath, result).jsonString();
                        document = objectMapper.readValue(s2, Object.class);
                    } else {
                        document = result;
                    }
                }

                machine.document = document;
                return flow.completedValue(machine).thenCompose(Stages::transition);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
