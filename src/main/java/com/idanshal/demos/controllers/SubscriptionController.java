package com.idanshal.demos.controllers;

import com.idanshal.demos.workflows.SubscriptionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Controller
@RequiredArgsConstructor
public class SubscriptionController {

    private final WorkflowClient client;

    private final Map<String, String> subscriptionMap = new HashMap<>();


    @PostMapping("/subscribe/{id}")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    void subscribe(@PathVariable String id) {
        String workflowId = String.valueOf(UUID.randomUUID());
        SubscriptionWorkflow workflow = client.newWorkflowStub(SubscriptionWorkflow.class, WorkflowOptions.newBuilder().
                setTaskQueue("SubscriptionTaskQueue").setRetryOptions(RetryOptions.newBuilder().
                        setMaximumAttempts(3).build()).setWorkflowId(workflowId).build());

        WorkflowClient.start(workflow::execute, id);
        subscriptionMap.put(id, workflowId);
    }

    @PostMapping("/cancel/{id}")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    void cancel(@PathVariable String id) {
//        SubscriptionWorkflow subscriptionWorkflow = client.newWorkflowStub(SubscriptionWorkflow.class, subscriptionMap.get(id));
//        WorkflowStub.fromTyped(subscriptionWorkflow).cancel();

        WorkflowStub workflowStub = client.newUntypedWorkflowStub(subscriptionMap.get(id));
        workflowStub.cancel();

        //subscriptionWorkflow.cancel(id);
    }
}


