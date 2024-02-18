package com.idanshal.demos.helpers;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.history.v1.History;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.api.workflowservice.v1.*;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.UUID;

@Component
public class WorkflowHelper {

    @Value("${spring.temporal.namespace}")
    private String namespace;

    WorkflowServiceGrpc.WorkflowServiceBlockingStub workflowServiceBlockingStub;


    @PostConstruct
    private void init() {
        final WorkflowServiceStubs workflowServiceStubs = WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.getDefaultInstance());
        workflowServiceBlockingStub = workflowServiceStubs.blockingStub();
    }

    public String resetWorkflow(String workflowId) {
        long lastWorkflowTaskCompletedEventIdBeforeCancel = getLastWorkflowTaskCompletedEventIdBeforeCancel(workflowId);
        ResetWorkflowExecutionRequest resetRequest = buildResetRequest(workflowId, lastWorkflowTaskCompletedEventIdBeforeCancel);
        ResetWorkflowExecutionResponse resetWorkflowExecutionResponse = workflowServiceBlockingStub.resetWorkflowExecution(resetRequest);
        return resetWorkflowExecutionResponse.getRunId();
    }

    private ResetWorkflowExecutionRequest buildResetRequest(String workflowId, long lastWorkflowTaskCompletedEventIdBeforeCancel) {
        return ResetWorkflowExecutionRequest.newBuilder()
                .setNamespace(namespace)
                .setRequestId(UUID.randomUUID().toString())
                .setWorkflowExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build())
                .setWorkflowTaskFinishEventId(lastWorkflowTaskCompletedEventIdBeforeCancel)
                .build();
    }
    private long getLastWorkflowTaskCompletedEventIdBeforeCancel(String workflowId) {
        GetWorkflowExecutionHistoryResponse historyResponse = getWorkflowExecutionHistoryResponse(workflowId);

        History history = historyResponse.getHistory();

        long cancelEventId = getWorkflowExecutionCancelRequestedEventId(history);

        return history.getEventsList().stream()
                .filter(HistoryEvent::hasWorkflowTaskCompletedEventAttributes)
                .map(HistoryEvent::getEventId)
                .filter(eventId -> eventId < cancelEventId)
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("No WorkflowTaskCompletedEvent found before WorkflowExecutionCancelRequestedEvent"));
    }

    private long getWorkflowExecutionCancelRequestedEventId(History history) {
        HistoryEvent workflowExecutionCancelRequestedEvent = history.getEventsList().stream()
                .filter(HistoryEvent::hasWorkflowExecutionCancelRequestedEventAttributes)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No WorkflowExecutionCancelRequestedEvent found"));

        return workflowExecutionCancelRequestedEvent.getEventId();
    }

    private GetWorkflowExecutionHistoryResponse getWorkflowExecutionHistoryResponse(String workflowId) {
        GetWorkflowExecutionHistoryRequest historyRequest = GetWorkflowExecutionHistoryRequest.newBuilder()
                .setNamespace(namespace)
                .setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build())
                .build();

        GetWorkflowExecutionHistoryResponse historyResponse;
        do {
            historyResponse = workflowServiceBlockingStub.getWorkflowExecutionHistory(historyRequest);
            historyRequest = historyRequest.toBuilder().setNextPageToken(historyResponse.getNextPageToken()).build();
        } while (!historyResponse.getNextPageToken().isEmpty());

        return historyResponse;
    }
}
