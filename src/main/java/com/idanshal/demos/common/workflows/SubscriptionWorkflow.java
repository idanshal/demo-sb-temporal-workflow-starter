package com.idanshal.demos.common.workflows;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SubscriptionWorkflow {
    @WorkflowMethod
    void execute(String customerIdentifier);
    @SignalMethod
    void approveUpgrade();

    @QueryMethod
    int getPaymentCount();
}