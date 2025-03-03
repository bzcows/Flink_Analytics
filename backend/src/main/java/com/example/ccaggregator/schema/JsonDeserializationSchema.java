package com.example.ccaggregator.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDeserializationSchema<T> implements DeserializationSchema<T> {
    private static final Logger logger = LoggerFactory.getLogger(JsonDeserializationSchema.class);
    private final Class<T> clazz;
    private final ObjectMapper objectMapper;

    public JsonDeserializationSchema(Class<T> clazz) {
        this.clazz = clazz;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Override
    public T deserialize(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (Exception e) {
            logger.error("Failed to deserialize JSON", e);
            return null;
        }
    }

    @Override
    public boolean isEndOfStream(T t) {
        return false;
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return TypeInformation.of(clazz);
    }
}