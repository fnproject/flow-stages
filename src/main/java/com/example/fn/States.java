package com.example.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.flow.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class States {

    private final Flow rt = Flows.currentFlow();
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static FlowFuture<Machine> transition(Machine machine) {

        State currentState = machine.states.get(machine.currentState);
        return currentState.transition(machine);
    }

    public String parseStateMachine(final ASL stateMachine) {

        Machine machine = toMachine(stateMachine);

        ExternalFlowFuture<HttpRequest> trigger = rt.createExternalFuture();

        trigger.thenApply((request) -> {
            try {
                machine.currentState = machine.startAt;
                Object document = objectMapper.readValue(request.getBodyAsBytes(), Object.class);
                machine.document = document;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return machine;
        }).thenCompose(States::transition);

        return trigger.completionUrl().toString();
    }

    public static Machine toMachine(ASL stateMachine) {
        Machine machine = new Machine();
        machine.comment = stateMachine.comment;
        machine.startAt = stateMachine.startAt;
        machine.states = new HashMap<>();
        machine.timeoutSeconds = stateMachine.timeoutSeconds;
        machine.version = stateMachine.version;

        Set<String> stateLabels = stateMachine.states.keySet();
        stateMachine.states.forEach((k, v) -> machine.states.put(k, translateValue(v, stateLabels)));
        return machine;
    }

    private static State translateValue(ASL.State rawState, Set<String> stateLabels) {
        switch(rawState.type) {
            case "Choice":
                Choice choiceState = new Choice();
                choiceState.comment = rawState.comment;
                if(!stateLabels.contains(rawState.choiceDefault)) {
                    throw new InvalidMachineException("Found a Default field referring to a non-existent state");
                }
                choiceState.defaultState = rawState.choiceDefault;
                choiceState.inputPath = rawState.inputPath;
                choiceState.outputPath = rawState.outputPath;
                if(rawState.choiceRules == null) {
                    throw new InvalidMachineException("Choice state must have a Choices field");
                }
                choiceState.rules = rawState.choiceRules
                        .stream()
                        .map(rawRule -> {
                            if(!stateLabels.contains(rawRule.next)) {
                                throw new InvalidMachineException("Found a Next field referring to a non-existent state");
                            }
                            return translateRule(rawRule);
                        })
                        .collect(Collectors.toList());
                return choiceState;
            case "Succeed":
                Succeed succeedState = new Succeed();
                succeedState.comment = rawState.comment;
                succeedState.inputPath = rawState.inputPath;
                succeedState.outputPath = rawState.outputPath;
                return succeedState;
            case "Fail":
                if(rawState.failCause == null) {
                    throw new InvalidMachineException("Fail state must have an Cause field");
                }
                if(rawState.failError == null) {
                    throw new InvalidMachineException("Fail state must have an Error field");
                }
                return new Fail(rawState.comment, rawState.failError, rawState.failCause);
            case "Pass":
                Pass passState = new Pass();
                passState.comment = rawState.comment;

                if (rawState.end != null && rawState.next == null) {
                    passState.end = rawState.end.booleanValue();
                } else if (rawState.end == null && rawState.next != null) {
                    if(!stateLabels.contains(rawState.next)) {
                        throw new InvalidMachineException("Found a Next field referring to a non-existent state");
                    }
                    passState.next = rawState.next;
                } else {
                    throw new InvalidMachineException("Only one of End or Next must be defined on a Pass state");
                }

                passState.inputPath = rawState.inputPath;
                passState.outputPath = rawState.outputPath;
                passState.result = rawState.result;
                passState.resultPath = rawState.resultPath;
                return passState;
            case "Task":
                Task taskState = new Task();
                taskState.comment = rawState.comment;

                if (rawState.end != null && rawState.next == null) {
                    taskState.end = rawState.end.booleanValue();
                } else if (rawState.end == null && rawState.next != null) {
                    if(!stateLabels.contains(rawState.next)) {
                        throw new InvalidMachineException("Found a Next field referring to a non-existent state");
                    }
                    taskState.next = rawState.next;
                } else {
                    throw new InvalidMachineException("Only one of End or Next must be defined on a Task state");
                }

                if(rawState.taskTimeoutSeconds != null) {
                    taskState.timeoutSeconds = Integer.valueOf(rawState.taskTimeoutSeconds);
                } else {
                    taskState.timeoutSeconds = 60;
                }
                if(rawState.taskHeartbeatSeconds != null) {
                    taskState.heartbeatSeconds = Integer.valueOf(rawState.taskHeartbeatSeconds);
                    if(taskState.heartbeatSeconds >= taskState.timeoutSeconds) {
                        throw new InvalidMachineException("HeartbeatSeconds must be smaller than TimeoutSeconds");
                    }
                }
                taskState.inputPath = rawState.inputPath;
                taskState.outputPath = rawState.outputPath;

                if(rawState.resource == null) {
                    throw new InvalidMachineException("Task state must have a Resource field");
                }
                taskState.resource = rawState.resource;

                taskState.retriers = rawState.errorRetry.stream().map(rawRetrier -> {
                    Task.Retrier retrier = new Task.Retrier();
                    retrier.backoffRate = rawRetrier.backoffRate;
                    retrier.errorEquals = rawRetrier.errorEquals;
                    retrier.maxAttempts = rawRetrier.maxAttempts;
                    retrier.intervalSeconds = rawRetrier.intervalSeconds;
                    return retrier;
                }).collect(Collectors.toList());

                taskState.catchers = rawState.errorCatch.stream().map(rawCatcher -> {
                    Task.Catcher catcher = new Task.Catcher();
                    catcher.errorEquals = rawCatcher.errorEquals;
                    catcher.next = rawCatcher.next;
                    catcher.resultPath = rawCatcher.resultPath;
                    return catcher;
                }).collect(Collectors.toList());

                return taskState;
            case "Wait":
                Wait waitState = new Wait();
                waitState.comment = rawState.comment;

                if (rawState.end != null && rawState.next == null) {
                    waitState.end = rawState.end.booleanValue();
                } else if (rawState.end == null && rawState.next != null) {
                    if(!stateLabels.contains(rawState.next)) {
                        throw new InvalidMachineException("Found a Next field referring to a non-existent state");
                    }
                    waitState.next = rawState.next;
                } else {
                    throw new InvalidMachineException("Only one of End or Next must be defined on a Wait state");
                }

                waitState.inputPath = rawState.inputPath;
                waitState.outputPath = rawState.outputPath;

                if(rawState.waitUntilTimestamp != null && rawState.waitForSeconds == null) {
                    waitState.timestamp = rawState.waitUntilTimestamp;
                } else if(rawState.waitUntilTimestamp == null && rawState.waitForSeconds != null) {
                    waitState.seconds = rawState.waitForSeconds;
                } else {
                    throw new InvalidMachineException("Wait state must contain exactly one of Seconds or Timestamp");
                }

                return waitState;
            default:
                throw new InvalidMachineException("State type must be one of Pass, Task, Choice, Wait, Succeed, or Fail");
        }
    }

    private static ChoiceRule translateRule(ASL.ChoiceRule rawRule) {
        if(rawRule.stringEquals != null) {
            return new ChoiceRule<String>(rawRule.next, rawRule.variable, s -> s.compareTo(rawRule.stringEquals) == 0);
        } else if(rawRule.stringGreaterThan != null) {
            return new ChoiceRule<String>(rawRule.next, rawRule.variable, s -> s.compareTo(rawRule.stringGreaterThan) > 0);
        } else if(rawRule.stringLessThan != null) {
            return new ChoiceRule<String>(rawRule.next, rawRule.variable, s -> s.compareTo(rawRule.stringLessThan) < 0);
        } else if(rawRule.stringGreaterThanEquals != null) {
            return new ChoiceRule<String>(rawRule.next, rawRule.variable, s -> s.compareTo(rawRule.stringGreaterThanEquals) >= 0);
        } else if(rawRule.stringLessThanEquals != null) {
            return new ChoiceRule<String>(rawRule.next, rawRule.variable, s -> s.compareTo(rawRule.stringLessThanEquals) <= 0);
        } else if(rawRule.booleanEquals != null) {
            return new ChoiceRule<Boolean>(rawRule.next, rawRule.variable, b -> b.compareTo(rawRule.booleanEquals) == 0);
        } else if(rawRule.numericEquals != null) {
            return new ChoiceRule<Double>(rawRule.next, rawRule.variable, i -> i.compareTo(rawRule.numericEquals) > 0);
        } else if(rawRule.numericGreaterThan != null) {
            return new ChoiceRule<Double>(rawRule.next, rawRule.variable, i -> i.compareTo(rawRule.numericGreaterThan) >= 0);
        } else if(rawRule.numericGreaterThanEquals != null) {
            return new ChoiceRule<Double>(rawRule.next, rawRule.variable, i -> i.compareTo(rawRule.numericGreaterThanEquals) >= 0);
        } else if(rawRule.numericLessThan != null) {
            return new ChoiceRule<Double>(rawRule.next, rawRule.variable, i -> i.compareTo(rawRule.numericLessThan) < 0);
        } else if(rawRule.numericLessThanEquals != null) {
            return new ChoiceRule<Double>(rawRule.next, rawRule.variable, i -> i.compareTo(rawRule.numericLessThanEquals) <= 0);
        } else {
            throw new InvalidMachineException("Unable to parse ChoiceRule");
        }
    }
}