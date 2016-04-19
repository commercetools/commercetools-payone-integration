package com.commercetools.pspadapter.payone.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author fhaertig
 * @since 19.04.16
 */
public class ClearSecuredValuesSerializer extends JsonSerializer<String> implements ContextualSerializer {

    public static final String PLACEHOLDER = "<HIDDEN>";

    private final BeanProperty property;

    public ClearSecuredValuesSerializer() {
        this(null);
    }
    private ClearSecuredValuesSerializer(final BeanProperty property) {
        this.property = property;
    }

    @Override
    public void serialize(final String value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException, JsonProcessingException {
        if (property != null && property.getAnnotation(Apply.class) != null) {
            gen.writeString(PLACEHOLDER);
        } else {
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(final SerializerProvider prov, final BeanProperty property) throws JsonMappingException {
        return new ClearSecuredValuesSerializer(property);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Apply {
        boolean value() default true;
    }
}
