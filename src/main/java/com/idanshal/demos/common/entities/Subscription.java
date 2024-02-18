package com.idanshal.demos.common.entities;

import com.idanshal.demos.common.enums.SubscriptionType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    @Id
    private String id;

    private String workflowId;

    @Enumerated(EnumType.STRING)
    private SubscriptionType subscriptionType;

    private boolean isActive;
}