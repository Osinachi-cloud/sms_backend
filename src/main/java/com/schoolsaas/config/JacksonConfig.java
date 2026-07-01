package com.schoolsaas.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            builder.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            builder.withCoercionConfig(LogicalType.Map, config ->
                    config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull));
            builder.withCoercionConfig(LogicalType.POJO, config ->
                    config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull));
            builder.withCoercionConfig(LogicalType.Array, config ->
                    config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull));
            builder.withCoercionConfig(LogicalType.Textual, config ->
                    config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull));
        };
    }
}
