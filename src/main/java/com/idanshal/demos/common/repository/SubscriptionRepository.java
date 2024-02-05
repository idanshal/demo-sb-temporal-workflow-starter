package com.idanshal.demos.common.repository;

import com.idanshal.demos.common.entities.Subscription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends CrudRepository<Subscription, String> {
}