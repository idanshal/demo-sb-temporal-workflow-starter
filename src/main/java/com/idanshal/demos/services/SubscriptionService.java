package com.idanshal.demos.services;

import com.idanshal.demos.common.workflows.SubscriptionWorkflow;
import com.idanshal.demos.common.entities.Subscription;
import com.idanshal.demos.common.repository.SubscriptionRepository;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.history.v1.History;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.api.workflowservice.v1.ResetWorkflowExecutionRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private static final String SUBSCRIPTION_TASK_QUEUE = "SubscriptionTaskQueue";
    private static final String SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID = "successfully {} workflow for customer id: {}. workflowId: {}";
    public static final String DEFAULT_NAMESPACE = "default";

    private final SubscriptionRepository subscriptionRepository;

    private final WorkflowClient client;
    public void subscribe(String id) {

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
        //subscriptionRepository.deleteById(id);
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "canceled", id, workflowId);
    }

    public void renew(String id) {
        Subscription subscription = getSubscriptionByCustomerId(id);
        String workflowId = subscription.getWorkflowId();
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.getDefaultInstance());
        long lastWorkflowTaskCompletedEventIdBeforeCancel = getLastWorkflowTaskCompletedEventIdBeforeCancel(service, workflowId);
        ResetWorkflowExecutionRequest resetRequest = ResetWorkflowExecutionRequest.newBuilder()
                .setNamespace(DEFAULT_NAMESPACE)
                .setRequestId(UUID.randomUUID().toString())
                .setWorkflowExecution(WorkflowExecution.newBuilder()
                        .setWorkflowId(workflowId)
                        .build())
                .setWorkflowTaskFinishEventId(lastWorkflowTaskCompletedEventIdBeforeCancel)
                .build();

        service.blockingStub().resetWorkflowExecution(resetRequest);
        log.info(SUCCESS_WORKFLOW_ACTION_FOR_CUSTOMER_ID_WORKFLOW_ID, "reset", id, workflowId);
    }

    private long getLastWorkflowTaskCompletedEventIdBeforeCancel(WorkflowServiceStubs service, String workflowId) {
        GetWorkflowExecutionHistoryRequest historyRequest = GetWorkflowExecutionHistoryRequest.newBuilder()
                .setNamespace(DEFAULT_NAMESPACE)
                .setExecution(WorkflowExecution.newBuilder()
                        .setWorkflowId(workflowId)
                        .build())
                .build();

        GetWorkflowExecutionHistoryResponse historyResponse = service.blockingStub().getWorkflowExecutionHistory(historyRequest);
        History history = historyResponse.getHistory();

        Optional<HistoryEvent> workflowExecutionCancelRequestedEvent = history.getEventsList().stream()
                .filter(HistoryEvent::hasWorkflowExecutionCancelRequestedEventAttributes)
                .findFirst();

        if (workflowExecutionCancelRequestedEvent.isEmpty()) {
            throw new IllegalStateException("No WorkflowExecutionCancelRequestedEvent found");
        }

        long workflowExecutionCancelRequestedEventId = workflowExecutionCancelRequestedEvent.get().getEventId();

        return history.getEventsList().stream()
                .filter(HistoryEvent::hasWorkflowTaskCompletedEventAttributes)
                .map(HistoryEvent::getEventId)
                .filter(eventId -> eventId < workflowExecutionCancelRequestedEventId)
                .max(Comparator.naturalOrder())
                .orElse(-1L);
    }

    private Subscription getSubscriptionByCustomerId(String id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for customer id: " + id));
    }
}
