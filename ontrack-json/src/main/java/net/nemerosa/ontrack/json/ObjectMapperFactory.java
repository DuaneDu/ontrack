package net.nemerosa.ontrack.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import java.time.*;

/**
 * Note that support for @ConstructorProperties will be available in Jackson 2.7.0
 * (see https://github.com/FasterXML/jackson-databind/issues/905)
 */
public final class ObjectMapperFactory {

    public static final Version JSON_MODULE_VERSION = new Version(1, 0, 0, null, "net.nemerosa.ontrack", "ontrack-json");

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        // Support for JDK 8 times
        jdkTime(mapper);
        // Support for Kotlin
        mapper.registerModule(new KotlinModule());
        // OK
        return mapper;
    }

    public static ObjectMapper create(Class<?> viewClass) {
        return new CustomObjectMapper(viewClass);
    }

    private static void jdkTime(ObjectMapper mapper) {
        SimpleModule jdkTimeModule = new SimpleModule(
                "JDKTimeModule",
                JSON_MODULE_VERSION
        );
        // LocalDateTime
        jdkTimeModule.addSerializer(LocalDateTime.class, new JDKLocalDateTimeSerializer());
        jdkTimeModule.addDeserializer(LocalDateTime.class, new JDKLocalDateTimeDeserializer());
        // LocalTime
        jdkTimeModule.addSerializer(LocalTime.class, new JDKLocalTimeSerializer());
        jdkTimeModule.addDeserializer(LocalTime.class, new JDKLocalTimeDeserializer());
        // LocalDate
        jdkTimeModule.addSerializer(LocalDate.class, new JDKLocalDateSerializer());
        jdkTimeModule.addDeserializer(LocalDate.class, new JDKLocalDateDeserializer());
        // YearMonth
        jdkTimeModule.addSerializer(YearMonth.class, new JDKYearMonthSerializer());
        jdkTimeModule.addDeserializer(YearMonth.class, new JDKYearMonthDeserializer());
        // Support for durations
        jdkTimeModule.addSerializer(Duration.class, DurationSerializer.INSTANCE);
        jdkTimeModule.addDeserializer(Duration.class, DurationDeserializer.INSTANCE);
        // OK
        mapper.registerModule(jdkTimeModule);
    }

    private ObjectMapperFactory() {
    }

    private static class CustomObjectMapper extends ObjectMapper {

        public CustomObjectMapper(Class<?> viewClass) {
            super();
            this._serializationConfig = this._serializationConfig.withView(viewClass);
        }
    }

}
