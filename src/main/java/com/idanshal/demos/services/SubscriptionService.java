package com.idanshal.demos.services;

import com.idanshal.demos.common.entities.Subscription;
import com.idanshal.demos.common.repository.SubscriptionRepository;
import com.idanshal.demos.common.workflows.SubscriptionWorkflow;
import com.idanshal.demos.helpers.WorkflowHelper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private static final String SUBSCRIPTION_TASK_QUEUE = "SubscriptionTaskQueue";
    private static final String SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID = "successfully {} workflow for customer id: {}. workflowId: {}";

    private final SubscriptionRepository subscriptionRepository;

    private final WorkflowClient client;

    private final WorkflowHelper workflowHelper;

    public String subscribe(String id) {
        if (subscriptionRepository.existsById(id)) {
            throw new IllegalArgumentException("customer id already exists");
        }

        String workflowId = String.valueOf(UUID.randomUUID());

        SubscriptionWorkflow workflow = client.newWorkflowStub(SubscriptionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(SUBSCRIPTION_TASK_QUEUE).setWorkflowId(workflowId).build());

        WorkflowClient.start(workflow::execute, id);

        Subscription subscription = Subscription.builder().id(id).workflowId(workflowId).build();
        subscriptionRepository.save(subscription);
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "executed", id, workflowId);
        return workflowId;
    }


    public void upgrade(String id) {
        Subscription subscription = getSubscriptionByCustomerId(id);
        String workflowId = subscription.getWorkflowId();
        SubscriptionWorkflow subscriptionWorkflow = client.newWorkflowStub(SubscriptionWorkflow.class, workflowId);
        subscriptionWorkflow.approveUpgrade();
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "signaled", id, workflowId);
    }

    public void cancel(String id) {
        Subscription subscription = getSubscriptionByCustomerId(id);
        String workflowId = subscription.getWorkflowId();
        WorkflowStub workflowStub = client.newUntypedWorkflowStub(workflowId);
        workflowStub.cancel();
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "canceled", id, workflowId);
    }

    public String renew(String id) {
        Subscription subscription = getSubscriptionByCustomerId(id);
        String workflowId = subscription.getWorkflowId();
        String runId = workflowHelper.resetWorkflow(workflowId);
        subscription.setActive(true);
        subscriptionRepository.save(subscription);
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "reset", id, workflowId);
        return runId;
    }


    public int query(String id) {
        Subscription subscription = getSubscriptionByCustomerId(id);
        String workflowId = subscription.getWorkflowId();
        SubscriptionWorkflow subscriptionWorkflow = client.newWorkflowStub(SubscriptionWorkflow.class, workflowId);
        return subscriptionWorkflow.getPaymentCount();
    }

    private Subscription getSubscriptionByCustomerId(String id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for customer id: " + id));
    }
}
