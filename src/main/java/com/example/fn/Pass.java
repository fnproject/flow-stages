package com.example.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.flow.Flow;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;

public class Pass extends State {
    String inputPath;
    String outputPath;
    Object result;
    String resultPath;
    String next;
    Boolean end;
    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    FlowFuture<Machine> transition(Machine machine) {
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
                return flow.completedValue(machine).thenCompose(States::transition);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
