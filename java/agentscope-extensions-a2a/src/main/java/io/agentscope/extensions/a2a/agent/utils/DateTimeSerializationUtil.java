package io.agentscope.extensions.a2a.agent.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.a2a.util.Utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Utils for A2A date time serialization.
 *
 * @author xiweng.yy
 */
public class DateTimeSerializationUtil {
    
    /**
     * Change the A2A Client deserialization time by LocalDateTime.
     */
    public static void adaptOldVersionA2aDateTimeSerialization() {
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(OffsetDateTime.class, new JsonDeserializer<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            
            @Override
            public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                LocalDateTime localDateTime = LocalDateTime.parse(p.getValueAsString(), formatter);
                return OffsetDateTime.of(localDateTime, ZoneOffset.UTC); // 强制附加时区
            }
        });
        Utils.OBJECT_MAPPER.disable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS).registerModule(module)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                .setTimeZone(TimeZone.getTimeZone("UTC"));
    }
}
