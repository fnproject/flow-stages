package com.example.fn;

import java.io.Serializable;
import java.util.function.Predicate;

public class ChoiceRule<X> implements Serializable {
    String next;
    String variable;
    SerPredicate<X> predicate;
    ChoiceRule(String next, String variable, SerPredicate<X> predicate) {
        this.next = next;
        this.variable = variable;
        this.predicate = predicate;
    }
    interface SerPredicate<X> extends Predicate<X>, Serializable {}
}
