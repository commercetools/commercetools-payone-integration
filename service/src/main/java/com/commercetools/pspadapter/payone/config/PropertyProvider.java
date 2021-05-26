package com.commercetools.pspadapter.payone.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @author fhaertig
 * @since 15.12.15
 */
public class PropertyProvider {

    public static final String TENANTS = "TENANTS";

    public static final String PAYONE_API_VERSION = "PAYONE_API_VERSION";
    public static final String PAYONE_REQUEST_ENCODING = "PAYONE_REQUEST_ENCODING";

    public static final String PAYONE_SOLUTION_NAME = "PAYONE_SOLUTION_NAME";
    public static final String PAYONE_SOLUTION_VERSION = "PAYONE_SOLUTION_VERSION";
    public static final String PAYONE_INTEGRATOR_NAME = "PAYONE_INTEGRATOR_NAME";
    public static final String PAYONE_INTEGRATOR_VERSION = "PAYONE_INTEGRATOR_VERSION";
    public static final String LOG_LEVEL = "LOG_LEVEL";

    public static final String PAYONE_API_URL = "PAYONE_API_URL";
    public static final String HIDE_CUSTOMER_PERSONAL_DATA = "HIDE_CUSTOMER_PERSONAL_DATA";

    private final Map<String, String> internalProperties;

    private final List<Function<String, String>> propertiesGetters;

    public PropertyProvider() {
        String implementationTitle = ofNullable(getClass().getPackage().getImplementationTitle()).orElse("DEBUG-TITLE");
        String implementationVersion =
            ofNullable(getClass().getPackage().getImplementationVersion()).orElse("DEBUG-VERSION");

        internalProperties = new HashMap<>();
        internalProperties.put(PAYONE_API_VERSION, "3.9");
        internalProperties.put(PAYONE_REQUEST_ENCODING, "UTF-8");
        internalProperties.put(PAYONE_SOLUTION_NAME, "commercetools-platform");
        internalProperties.put(PAYONE_SOLUTION_VERSION, "1");
        internalProperties.put(PAYONE_INTEGRATOR_NAME, implementationTitle);
        internalProperties.put(PAYONE_INTEGRATOR_VERSION, implementationVersion);


        propertiesGetters = new ArrayList<>();
        propertiesGetters.add(System::getProperty);
        propertiesGetters.add(System::getenv);
        propertiesGetters.add(internalProperties::get);
    }

    /**
     * Try to get a property by name in the next order:<ul>
     *     <li>fetch from runtime properties (<i>-D</i> java arguments) using {@link System#getProperty(String)}</li>
     *     <li>fetch from environment variables using {@link System#getenv(String)}</li>
     *     <li>fetch from hardcoded {@link #internalProperties} map.</li>
     * </ul>
     * <p>
     * If neither of them exists - empty {@link Optional} is returned.
     *
     * @param propertyName the name of the requested property, must not be null
     * @return the property, an empty Optional if not present; empty values are treated as present
     */
    public Optional<String> getProperty(final String propertyName) {
        return isNotBlank(propertyName)
                ? propertiesGetters.stream()
                    .map(getter -> getter.apply(propertyName))
                    .filter(Objects::nonNull)
                    .findFirst()
                : empty();
    }

    /**
     * Gets a mandatory non-empty property.
     *
     * @param propertyName the name of the requested property, must not be null
     * @return the property value
     * @throws IllegalStateException if the property isn't defined or empty
     */
    public String getMandatoryNonEmptyProperty(final String propertyName) {
        return getProperty(propertyName)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> createIllegalStateException(propertyName));
    }

    private IllegalStateException createIllegalStateException(final String propertyName) {
        return new IllegalStateException("Value of " + propertyName + " is required and can not be empty!");
    }

    public List<Function<String, String>> getPropertiesGetters() {
        return propertiesGetters;
    }
}
