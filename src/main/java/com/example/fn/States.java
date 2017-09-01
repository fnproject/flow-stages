package com.example.fn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.*;

import java.io.IOException;
import java.util.HashMap;

public class States {

    private final CloudThreadRuntime rt = CloudThreads.currentRuntime();
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static CloudFuture<StateMachine> transition(StateMachine stateMachine) {
        CloudThreadRuntime rt = CloudThreads.currentRuntime();
        StateMachine.State state = stateMachine.states.get(stateMachine.currentState);
        switch(state.type) {
            case "Task":
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-type", "application/json");
                try {
                    byte[] bytes = objectMapper.writeValueAsBytes(stateMachine.document);

                    CloudFuture<StateMachine> f = rt.invokeFunction(state.resource, HttpMethod.POST, Headers.fromMap(headers), bytes)
                                .thenApply((response) -> {
                                    try {
                                        Object document = objectMapper.readValue(response.getBodyAsBytes(), Object.class);
                                        stateMachine.document = document;
                                        if (state.next != null) stateMachine.currentState = state.next;
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