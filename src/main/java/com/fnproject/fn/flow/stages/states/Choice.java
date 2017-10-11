package com.fnproject.fn.flow.stages.states;

import com.fnproject.fn.api.flow.FlowFuture;
import com.fnproject.fn.api.flow.Flows;
import com.fnproject.fn.flow.stages.Machine;
import com.fnproject.fn.flow.stages.Stages;
import com.fnproject.fn.flow.stages.State;
import com.fnproject.fn.flow.stages.TerminatedWithErrorException;
import com.jayway.jsonpath.JsonPath;

import java.util.List;

public class Choice extends State {
    public List<ChoiceRule> rules;
    public String defaultState;
    public String inputPath;
    public String outputPath;

    public Choice(String comment) {
        super(comment);
    }

    @Override
    public FlowFuture<Machine> transition(Machine machine) {
        // TODO: Support timestamp comparisons, and all combinators
        for(ChoiceRule rule : rules) {
            Double variable = JsonPath.parse(machine.document).read(rule.variable, Double.class);
            if(rule.predicate.test(variable)) {
                machine.currentState = rule.next;
                return Flows.currentFlow().completedValue(machine).thenCompose(Stages::transition);
            }
        }
        if (defaultState != null) {
            machine.currentState = defaultState;
            return Flows.currentFlow().completedValue(machine).thenCompose(Stages::transition);
        } else {
            throw new TerminatedWithErrorException("Stages.NoChoiceMatched");
        }
    }
}
