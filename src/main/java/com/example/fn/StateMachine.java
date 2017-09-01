package com.example.fn;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class StateMachine implements Serializable{
    String currentState;
    Object document;

    @JsonProperty("Comment")
    String comment;
    @JsonProperty("StartAt")
    String startAt;
    @JsonProperty("States")
    Map<String, State> states;
    @JsonProperty("Version")
    String version;
    @JsonProperty("TimeoutSeconds")
    String timeoutSeconds;

    static class State implements Serializable {
        @JsonProperty("Type")
        String type;
        @JsonProperty("Resource")
        String resource;
        @JsonProperty("Next")
        String next;
        @JsonProperty("Comment")
        String comment;
        @JsonProperty("End")
        Boolean end;
        @JsonProperty("InputPath")
        String inputPath;
        @JsonProperty("OutputPath")
        String outputPath;
        @JsonProperty("Result")
        Object result;
        @JsonProperty("ResultPath")
        String resultPath;
        @JsonProperty("Retry")
        List<Retry> errorRetry;
        @JsonProperty("Catch")
        String errorCatch;

        // Type: Task
        @JsonProperty("TimeoutSeconds")
        String taskTimeoutSeconds;
        @JsonProperty("HeartbeatSeconds")
        String taskHeartbeatSeconds;

        // Type: Choice
        @JsonProperty("Choices")
        List choiceRules;

        // Type: Wait
        @JsonProperty("Timestamp")
        String waitUntilTimestamp;
        @JsonProperty("Seconds")
        Integer waitForSeconds;

        // Type: Fail
        @JsonProperty("Error")
        String failError;
        @JsonProperty("Cause")
        String failCause;

    }
    class ChoiceRule implements Serializable {
        @JsonProperty("StringEquals")
        String stringEquals;
        @JsonProperty("StringLessThan")
        String stringLessThan;
        @JsonProperty("StringGreaterThan")
        String stringGreaterThan;
        @JsonProperty("StringLessThanEquals")
        String stringLessThanEquals;
        @JsonProperty("StringGreaterThanEquals")
        String stringGreaterThanEquals;
        @JsonProperty("NumericEquals")
        Double numericEquals;
        @JsonProperty("NumericLessThan")
        Double numericLessThan;
        @JsonProperty("NumericLessThanEquals")
        Double numericLessThanEquals;
        @JsonProperty("NumericGreaterThanEquals")
        Double numericGreaterThanEquals;
        @JsonProperty("BooleanEquals")
        Boolean booleanEquals;
        @JsonProperty("TimestampEquals")
        String timestampEquals;
        @JsonProperty("TimestampLessThan")
        String timestampLessThan;
        @JsonProperty("TimestampGreaterThan")
        String timestampGreaterThan;
        @JsonProperty("TimestampLessThanEquals")
        String timestampLessThanEquals;
        @JsonProperty("TimestampGreaterThanEquals")
        String getTimestampGreaterThanEquals;
        @JsonProperty("And")
        List<ChoiceRule> and;
        @JsonProperty("Or")
        List<ChoiceRule> or;
        @JsonProperty("Not")
        ChoiceRule not;
    }

    class Retry implements Serializable {
        @JsonProperty("ErrorEquals")
        List<String> errorEquals;
        @JsonProperty("IntervalSeconds")
        Integer intervalSeconds;
        @JsonProperty("MaxAttempts")
        Integer maxAttempts;
        @JsonProperty("BackoffRate")
        Double backoffRate;
    }

    class Catch implements Serializable {
        @JsonProperty("ErrorEquals")
        List<String> errorEquals;
        @JsonProperty("ResultPath")
        String resultPath;
        @JsonProperty("Next")
        String next;
    }


}
