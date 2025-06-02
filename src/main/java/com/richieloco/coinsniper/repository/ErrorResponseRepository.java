package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.ErrorResponseRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface ErrorResponseRepository extends ReactiveCrudRepository<ErrorResponseRecord, UUID> {
}
