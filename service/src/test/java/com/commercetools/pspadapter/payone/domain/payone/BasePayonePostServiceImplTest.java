package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * This base class creates real {@link PayonePostService} implementation instance, so you can execute requests and parse
 * responses.
 */
@Ignore("Base class for testing payone post service real requests.")
@RunWith(MockitoJUnitRunner.class)
public class BasePayonePostServiceImplTest {

    /**
     * Service to execute requests. Instantiated in {@link #setUp()}.
     */
    PayonePostService payonePostService;

    /**
     * Mock for properties provider. See {@link #setUp()} for more details.
     */
    @Mock
    protected PropertyProvider propertyProvider;

    /**
     * Config to fetch default payone gateway URL. Not mocked so far, but may be mocked in future.
     */
    PayoneConfig payoneConfig;

    /**
     * Setup basic service configuration. By default:<ul>
     *     <li>{@code propertyProvider}</li>
     *       <ul>
     *         <li>all {@code propertyProvider.getProperty()} mocked to {@link Optional#empty()}, so later
     *         {@code new PayoneConfig(propertyProvider)} uses default values</li>
     *         <li>{@code getMandatoryNonEmptyProperty()} mocked to "testDummyValue" string</li>
     *       </ul>
     *     <li>{@code payoneConfig}</li>
     *       <ul>
     *         <li>has all properties default</li>
     *       </ul>
     *     <li>payonePostService</li>
     *       <ul>
     *         <li>Since all above properties set to empty or default -- {@code payonePostService#serverAPIURL}
     *         equals to default {@link PayoneConfig#DEFAULT_PAYONE_API_URL}</li>
     *       </ul>
     * </ul>
     */
    @Before
    public void setUp()  {
        when(propertyProvider.getProperty(anyString())).thenReturn(Optional.empty());
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_KEY)).thenReturn("testDummyValue");

        payoneConfig = new PayoneConfig(propertyProvider);
        payonePostService = PayonePostServiceImpl.of(payoneConfig.getApiUrl());
    }
}
