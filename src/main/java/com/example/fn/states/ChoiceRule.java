package com.example.fn.states;

import java.io.Serializable;
import java.util.function.Predicate;

public class ChoiceRule<X> implements Serializable {
    String next;
    String variable;
    SerPredicate<X> predicate;
    public ChoiceRule(String next, String variable, SerPredicate<X> predicate) {
        this.next = next;
        this.variable = variable;
        this.predicate = predicate;
    }
    public interface SerPredicate<X> extends Predicate<X>, Serializable {}
}
