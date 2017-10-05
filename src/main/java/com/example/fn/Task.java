package com.example.fn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.api.flow.HttpMethod;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class Task extends State {
    String resource;
    Integer timeoutSeconds;
    Integer heartbeatSeconds;
    String inputPath;
    String outputPath;
    String next;
    Boolean end;
    String resultPath;
    String result; // This shouldn't be here?
    List<Retrier> retriers;
    List<Catcher> catchers;

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    FlowFuture<Machine> transition(Machine machine) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-type", "application/json");
        try {
            Object inputDocument = machine.document;
            if (inputPath != null) {
                String s = objectMapper.writeValueAsString(machine.document);
                inputDocument = JsonPath.parse(s).read(inputPath, Object.class);
            }
            byte[] bytes = objectMapper.writeValueAsBytes(inputDocument);
            System.out.println(new String(bytes));

            FlowFuture<Machine> f = Flows.currentFlow().invokeFunction(resource, HttpMethod.POST, Headers.fromMap(headers), bytes)
                    .thenApply((response) -> {
                        try {
                            Object document = objectMapper.readValue(response.getBodyAsBytes(), Object.class);
                            // TODO: If "OutputPath" is present, use this to pull out the value from the result
                            if(result != null) {
                                if(resultPath != null) {
                                    String s = objectMapper.writeValueAsString(document);
                                    String s2 = JsonPath.parse(s).set(resultPath, result).jsonString();
                                    document = objectMapper.readValue(s2, Object.class);
                                } else {
                                    document = result; // Don't think this is in ASL?
                                }
                            }

                            // Reset all current retry attempts to zero, in case we've retried
                            if(retriers != null) {
                                for (Retrier retry : retriers) {
                                    retry.currentAttempts = 0;
                                }
                            }
                            machine.document = document;

                            System.out.println("Transitioning from state " + machine.currentState + " to state " + next);
                            machine.currentState = next;

                            return machine;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .exceptionally((Throwable e) -> {
                        System.out.println("Task failed with error: " + e.getMessage());
                        if(retriers != null) {
                            for(Retrier retry : retriers) {
                                for(String error : retry.errorEquals) {
                                    if (error.equals("States.ALL") || error.equals(e.getMessage())) {
                                        if (retry.currentAttempts < retry.maxAttempts) {
                                            System.out.println(String.format("Retrying function, currentAttempts=%d, maxAttempts=%d", retry.currentAttempts, retry.maxAttempts));
                                            retry.currentAttempts = retry.currentAttempts + 1;
                                            return machine;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        if(catchers != null) {
                            for(Catcher c : catchers) {
                                for(String error : c.errorEquals) {
                                    if (error.equals("States.ALL") || error.equals(e.getMessage())) {
                                        System.out.println("Caught an error, transitioning");

                                        // Can't store the error at the moment, because it's a Java exception
                                        // rather than a JSON document (as the error is in Lambda)

                                        machine.currentState = c.next;
                                        return machine;
                                    }
                                }
                            }
                        }
                        throw new RuntimeException("Failing state machine, as uncaught error occurred");
                    });
            if (end != null && end) {
                return f;
            } else {
                return f.thenCompose(States::transition);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static class Retrier implements Serializable {
        List<String> errorEquals;
        Integer intervalSeconds;
        Integer maxAttempts;
        Double backoffRate;

        // Mutable
        Integer currentAttempts = 0;
    }

    static class Catcher implements Serializable {
        List<String> errorEquals;
        String resultPath;
        String next;
    }
}
