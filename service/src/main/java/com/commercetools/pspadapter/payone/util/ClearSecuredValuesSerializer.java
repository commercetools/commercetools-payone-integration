package com.commercetools.pspadapter.payone.util;

import com.commercetools.pspadapter.payone.config.PropertyProvider;
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

import static com.commercetools.pspadapter.payone.config.PropertyProvider.HIDE_CUSTOMER_PERSONAL_DATA;

/**
 * Bean fields marked with this annotation should be serialized with value {@code <HIDDEN>}.
 * {@code Apply} accepts 1 boolean parameter. If the parameter is {@code true} then the value will be hidden.
 * If the parameter is {@code false} or was not provided then the code will check environment variable
 * {@code HIDE_CUSTOMER_PERSONAL_DATA}. If the value of the environment variable is {@code true} then the
 * field will be hidden. If the value of environment variable is not provided then it will be counted as {@code true}
 *
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
        final PropertyProvider propertyProvider = new PropertyProvider();
        final boolean hideCustomerPersonalData = propertyProvider.getProperty(HIDE_CUSTOMER_PERSONAL_DATA)
                .map(dataString -> !dataString.equals("false"))
                .orElse(true);
        if (property != null && property.getAnnotation(Apply.class) != null && (property.getAnnotation(Apply.class).value() || hideCustomerPersonalData)) {
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
        boolean value() default false;
    }
}
