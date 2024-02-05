package com.idanshal.demos.controllers;

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
    void subscribe(@PathVariable String id) {
        subscriptionService.subscribe(id);
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
    void renew(@PathVariable String id) {
        subscriptionService.renew(id);
    }
}


