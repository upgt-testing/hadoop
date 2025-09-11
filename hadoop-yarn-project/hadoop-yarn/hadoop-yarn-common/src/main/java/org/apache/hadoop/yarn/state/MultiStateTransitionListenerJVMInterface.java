package org.apache.hadoop.yarn.state;

public interface MultiStateTransitionListenerJVMInterface<OPERAND, EVENT, STATE> extends StateTransitionListenerJVMInterface<OPERAND, EVENT, STATE> {

    void postTransition(OPERAND arg0, STATE arg1, STATE arg2, EVENT arg3);

    void preTransition(OPERAND arg0, STATE arg1, EVENT arg2);

    void addListener_bridge(java.lang.Object arg0);
}
