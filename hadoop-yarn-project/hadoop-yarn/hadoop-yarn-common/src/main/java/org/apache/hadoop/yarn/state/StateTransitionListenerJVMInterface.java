package org.apache.hadoop.yarn.state;

public interface StateTransitionListenerJVMInterface<OPERAND, EVENT, STATE> {

    void postTransition(OPERAND arg0, STATE arg1, STATE arg2, EVENT arg3);

    void preTransition(OPERAND arg0, STATE arg1, EVENT arg2);
}
