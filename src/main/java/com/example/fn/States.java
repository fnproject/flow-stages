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
        if (state == null) {
            throw new RuntimeException("State should not be null");
        }
        switch(state.type) {
            case "Succeed":
                return rt.completedValue(stateMachine);
            case "Fail":
                if (state.failCause == null) {
                    throw new RuntimeException("State of type Fail must contain Cause field");
                }
                if (state.failError == null) {
                    throw new RuntimeException("State of type Fail must contain Error field");
                }
                return rt.completedValue(stateMachine);
            case "Wait": {
                if (state.next == null && state.end == null) {
                    throw new RuntimeException("State of type Wait must contain one of Next or End fields");
                }
                CloudFuture<Void> f = rt.delay(state.waitForSeconds, TimeUnit.SECONDS);

                if (state.end != null && state.end) {
                    return f.thenApply(v -> stateMachine);
                } else {
                    stateMachine.currentState = state.next;
                    return f.thenApply(v -> stateMachine).thenCompose(States::transition);
                }
            }
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

                if (state.choiceRules == null) {
                    throw new RuntimeException("State of type Choice must contain a Choices field");
                }

                // TODO: Support timestamp comparisons, combinators, and all string equality comparisons
                for(StateMachine.ChoiceRule rule : state.choiceRules) {

                    if (rule.next == null) {
                        throw new RuntimeException("A Choice rule must contain Next field");
                    }

                    if(rule.numericEquals != null) {
                        Double variable = JsonPath.parse(stateMachine.document).read(rule.variable, Double.class);
                        if (variable.equals(rule.numericEquals)) {
                            stateMachine.currentState = rule.next;
                            return rt.completedValue(stateMachine).thenCompose(States::transition);
                        }
                    } else if(rule.numericGreaterThanEquals != null) {
                        Double variable = JsonPath.parse(stateMachine.document).read(rule.variable, Double.class);
                        if (variable >= rule.numericGreaterThanEquals) {
                            stateMachine.currentState = rule.next;
                            return rt.completedValue(stateMachine).thenCompose(States::transition);
                        }
                    } else if(rule.numericLessThan != null) {
                        Double variable = JsonPath.parse(stateMachine.document).read(rule.variable, Double.class);
                        if (variable < rule.numericLessThan) {
                            stateMachine.currentState = rule.next;
                            return rt.completedValue(stateMachine).thenCompose(States::transition);
                        }
                    } else if(rule.numericLessThanEquals != null) {
                        Double variable = JsonPath.parse(stateMachine.document).read(rule.variable, Double.class);
                        if (variable <= rule.numericLessThanEquals) {
                            stateMachine.currentState = rule.next;
                            return rt.completedValue(stateMachine).thenCompose(States::transition);
                        }
                    } else if(rule.booleanEquals != null) {
                        Boolean variable = JsonPath.parse(stateMachine.document).read(rule.variable, Boolean.class);
                        if (variable.equals(rule.booleanEquals)) {
                            stateMachine.currentState = rule.next;
                            return rt.completedValue(stateMachine).thenCompose(States::transition);
                        }
                    } else if(rule.stringEquals != null) {
                        String variable = JsonPath.parse(stateMachine.document).read(rule.variable, String.class);
                        if (variable.equals(rule.stringEquals)) {
                            stateMachine.currentState = rule.next;
                            return rt.completedValue(stateMachine).thenCompose(States::transition);
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

                if (state.next == null && state.end == null) {
                    throw new RuntimeException("State of type Task must contain Next or End field");
                }

                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-type", "application/json");
                try {
                    Object inputDocument = stateMachine.document;
                    if (state.inputPath != null) {
                        String s = objectMapper.writeValueAsString(stateMachine.document);
                        inputDocument = JsonPath.parse(s).read(state.inputPath, Object.class);
                    }
                    byte[] bytes = objectMapper.writeValueAsBytes(inputDocument);
                    System.out.println(new String(bytes));
                    CloudFuture<StateMachine> f = rt.invokeFunction(state.resource, HttpMethod.POST, Headers.fromMap(headers), bytes)
                                .thenApply((response) -> {
                                    try {
                                        Object document = objectMapper.readValue(response.getBodyAsBytes(), Object.class);
                                        // TODO: If "OutputPath" is present, use this to pull out the value from the result
                                        if(state.result != null) {
                                            if(state.resultPath != null) {
                                                String s = objectMapper.writeValueAsString(document);
                                                String s2 = JsonPath.parse(s).set(state.resultPath, state.result).jsonString();
                                                document = objectMapper.readValue(s2, Object.class);
                                            } else {
                                                document = state.result;
                                            }
                                        }

                                        // Reset all current retry attempts to zero, in case we've retried
                                        if(state.errorRetry != null) {
                                            for (StateMachine.Retry retry : state.errorRetry) {
                                                retry.currentAttempts = 0;
                                            }
                                        }
                                        stateMachine.document = document;

                                        System.out.println("Transitioning from state " + stateMachine.currentState + " to state " + state.next);
                                        stateMachine.currentState = state.next;

                                        return stateMachine;
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                            .exceptionally((e) -> {
                                System.out.println("Task failed with error: " + e.getMessage());
                                if(state.errorRetry != null) {
                                    for(StateMachine.Retry retry : state.errorRetry) {
                                        for(String error : retry.errorEquals) {
                                            if (error.equals(e.getMessage())) {
                                                if (retry.currentAttempts < retry.maxAttempts) {
                                                    System.out.println(String.format("Retrying function, currentAttempts=%d, maxAttempts=%d", retry.currentAttempts, retry.maxAttempts));
                                                    retry.currentAttempts = retry.currentAttempts + 1;
                                                    return stateMachine;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                                throw new RuntimeException("Failing state machine, as uncaught error occurred");
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