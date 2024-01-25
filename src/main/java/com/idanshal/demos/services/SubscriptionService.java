package com.idanshal.demos.services;

import com.idanshal.demos.workflows.SubscriptionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private static final String SUBSCRIPTION_TASK_QUEUE = "SubscriptionTaskQueue";
    private static final String SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID = "successfully {} workflow for customer id: {}. workflowId: {}";

    private final WorkflowClient client;
    private final Map<String, String> subscriptionMap = new HashMap<>();

    public void subscribe(@PathVariable String id) {

        if (subscriptionMap.containsKey(id)) {
            throw new IllegalArgumentException("customer id already exists");
        }

        String workflowId = String.valueOf(UUID.randomUUID());

        SubscriptionWorkflow workflow = client.newWorkflowStub(SubscriptionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(SUBSCRIPTION_TASK_QUEUE).setWorkflowId(workflowId).build());

        WorkflowClient.start(workflow::execute, id);
        subscriptionMap.put(id, workflowId);
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "executed", id, workflowId);
    }


    public void upgrade(String id) {
        @NonNull String workflowId = subscriptionMap.get(id);
        SubscriptionWorkflow subscriptionWorkflow = client.newWorkflowStub(SubscriptionWorkflow.class, workflowId);
        subscriptionWorkflow.approveUpgrade();
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "signaled", id, workflowId);
    }

    public void cancel(String id) {
        @NonNull String workflowId = subscriptionMap.get(id);
        WorkflowStub workflowStub = client.newUntypedWorkflowStub(workflowId);
        workflowStub.cancel();
        subscriptionMap.remove(workflowId);
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "canceled", id, workflowId);
    }


}
