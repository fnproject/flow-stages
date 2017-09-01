package com.example.fn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.*;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class States {

    private final CloudThreadRuntime rt = CloudThreads.currentRuntime();
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static CloudFuture<StateMachine> transition(StateMachine stateMachine) {
        CloudThreadRuntime rt = CloudThreads.currentRuntime();
        StateMachine.State state = stateMachine.states.get(stateMachine.currentState);
        switch(state.type) {
            case "Succeed":
                return rt.completedValue(stateMachine);
            case "Fail":
                return rt.completedValue(stateMachine);
            case "Wait":
                if (state.next != null) stateMachine.currentState = state.next;
                return rt.delay(state.waitForSeconds, TimeUnit.SECONDS)
                        .thenApply(v -> stateMachine)
                        .thenCompose(States::transition);
            case "Pass":
                if(state.end != null && state.end) {
                    return rt.completedValue(stateMachine);
                } else {
                    System.out.println("Transitioning from state " + stateMachine.currentState + " to state " + state.next);

                    if (state.next != null) stateMachine.currentState = state.next;
                    try {
                        Object document = stateMachine.document;

                        if(state.result != null) {
                            if(state.resultPath != null) {
                                String s = objectMapper.writeValueAsString(stateMachine.document);
                                // TODO: This only updates, not sets new values, figure out why
                                String s2 = JsonPath.parse(s).set(state.resultPath, state.result).jsonString();
                                document = objectMapper.readValue(s2, Object.class);
                            } else {
                                document = state.result;
                            }
                        }

                        stateMachine.document = document;
                        return rt.completedValue(stateMachine).thenCompose(States::transition);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            case "Choice":
                for(StateMachine.ChoiceRule rule : state.choiceRules) {
                    if(rule.numericEquals != null) {
                        Double variable = JsonPath.parse(stateMachine.document).read(rule.variable, Double.class);
                        if (variable.equals(rule.numericEquals)) {
                            if (rule.next != null) {
                                stateMachine.currentState = rule.next;
                                return rt.completedValue(stateMachine).thenCompose(States::transition);
                            } else {
                                // Shouldn't get here either :\
                                return rt.completedValue(stateMachine);
                            }
                        }
                    } else if(rule.numericGreaterThanEquals != null) {
                        Double variable = JsonPath.parse(stateMachine.document).read(rule.variable, Double.class);
                        if (variable >= rule.numericGreaterThanEquals) {
                            if (rule.next != null) {
                                stateMachine.currentState = rule.next;
                                return rt.completedValue(stateMachine).thenCompose(States::transition);
                            } else {
                                // Shouldn't get here either :\
                                return rt.completedValue(stateMachine);
                            }
                        }
                    } else if(rule.numericLessThan != null) {
                        Double variable = JsonPath.parse(stateMachine.document).read(rule.variable, Double.class);
                        if (variable < rule.numericLessThan) {
                            if (rule.next != null) {
                                stateMachine.currentState = rule.next;
                                return rt.completedValue(stateMachine).thenCompose(States::transition);
                            } else {
                                // Shouldn't get here either :\
                                return rt.completedValue(stateMachine);
                            }
                        }
                    } else if(rule.numericLessThanEquals != null) {
                        Double variable = JsonPath.parse(stateMachine.document).read(rule.variable, Double.class);
                        if (variable <= rule.numericLessThanEquals) {
                            if (rule.next != null) {
                                stateMachine.currentState = rule.next;
                                return rt.completedValue(stateMachine).thenCompose(States::transition);
                            } else {
                                // Shouldn't get here either :\
                                return rt.completedValue(stateMachine);
                            }
                        }
                    } else if(rule.booleanEquals != null) {
                        Boolean variable = JsonPath.parse(stateMachine.document).read(rule.variable, Boolean.class);
                        if (variable.equals(rule.booleanEquals)) {
                            if (rule.next != null) {
                                stateMachine.currentState = rule.next;
                                return rt.completedValue(stateMachine).thenCompose(States::transition);
                            } else {
                                // Shouldn't get here either :\
                                return rt.completedValue(stateMachine);
                            }
                        }
                    } else if(rule.stringEquals != null) {
                        String variable = JsonPath.parse(stateMachine.document).read(rule.variable, String.class);
                        if (variable.equals(rule.stringEquals)) {
                            if (rule.next != null) {
                                stateMachine.currentState = rule.next;
                                return rt.completedValue(stateMachine).thenCompose(States::transition);
                            } else {
                                // Shouldn't get here either :\
                                return rt.completedValue(stateMachine);
                            }
                        }
                    }
                }
                if (state.choiceDefault != null) {
                    stateMachine.currentState = state.choiceDefault;
                    return rt.completedValue(stateMachine).thenCompose(States::transition);
                } else {
                    // Shouldn't get here
                    return rt.completedValue(stateMachine);
                }

            case "Task":
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-type", "application/json");
                try {
                    byte[] bytes = objectMapper.writeValueAsBytes(stateMachine.document);
                    System.out.println(new String(bytes));
                    CloudFuture<StateMachine> f = rt.invokeFunction(state.resource, HttpMethod.POST, Headers.fromMap(headers), bytes)
                                .thenApply((response) -> {
                                    try {
                                        Object document = objectMapper.readValue(response.getBodyAsBytes(), Object.class);

                                        // TODO: If "ResultPath" is present, then should use this to update the document
                                        stateMachine.document = document;

                                        if(state.next != null) {
                                            System.out.println("Transitioning from state " + stateMachine.currentState + " to state " + state.next);
                                            stateMachine.currentState = state.next;
                                        }
                                        return stateMachine;
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    if (state.end != null && state.end) {
                        return f;
                    } else {
                        return f.thenCompose(States::transition);
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            default:
                throw new RuntimeException("Unknown state type: " + state.type);
        }
    }

    public String parseStateMachine(StateMachine stateMachine) {

        ExternalCloudFuture<HttpRequest> trigger = rt.createExternalFuture();

        trigger.thenApply((request) -> {
            try {
                stateMachine.currentState = stateMachine.startAt;
                Object document = objectMapper.readValue(request.getBodyAsBytes(), Object.class);
                stateMachine.document = document;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return stateMachine;
        }).thenCompose(States::transition);

        return trigger.completionUrl().toString();
    }
}