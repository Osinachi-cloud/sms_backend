package com.schoolsaas.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            builder.postConfigurer(mapper -> {
                mapper.coercionConfigFor(LogicalType.Map)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
                mapper.coercionConfigFor(LogicalType.POJO)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
                mapper.coercionConfigFor(LogicalType.Array)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
                mapper.coercionConfigFor(LogicalType.Textual)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
            });
        };
    }
}
