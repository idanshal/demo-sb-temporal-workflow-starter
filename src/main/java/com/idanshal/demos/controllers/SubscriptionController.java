package com.idanshal.demos.controllers;

import com.idanshal.demos.dto.QueryResponse;
import com.idanshal.demos.dto.RenewResponse;
import com.idanshal.demos.dto.SubscribeResponse;
import com.idanshal.demos.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{id}")
    @ResponseStatus(code = HttpStatus.ACCEPTED)
    SubscribeResponse subscribe(@PathVariable String id) {
        String workflowId = subscriptionService.subscribe(id);
        return new SubscribeResponse(workflowId);
    }

    @PutMapping("/upgrade/{id}")
    void upgrade(@PathVariable String id) {
        subscriptionService.upgrade(id);
    }

    @PutMapping("/cancel/{id}")
    void cancel(@PathVariable String id) {
        subscriptionService.cancel(id);
    }

    @PutMapping("/renew/{id}")
    RenewResponse renew(@PathVariable String id) {
        String runId = subscriptionService.renew(id);
        return new RenewResponse(runId);
    }

    @GetMapping("/query/{id}")
    QueryResponse query(@PathVariable String id) {
        int paymentCount = subscriptionService.query(id);
        return new QueryResponse(paymentCount);
    }
}


