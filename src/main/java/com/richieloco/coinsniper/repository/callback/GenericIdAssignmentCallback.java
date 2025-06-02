package com.richieloco.coinsniper.repository.callback;

import com.richieloco.coinsniper.entity.Identifiable;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class GenericIdAssignmentCallback implements BeforeConvertCallback<Identifiable> {

    @Override
    public Mono<Identifiable> onBeforeConvert(Identifiable entity, SqlIdentifier table) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        return Mono.just(entity);
    }
}