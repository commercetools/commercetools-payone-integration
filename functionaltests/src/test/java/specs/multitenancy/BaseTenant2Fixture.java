package specs.multitenancy;

import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.queries.PaymentByIdGet;
import io.sphere.sdk.types.Type;
import org.junit.BeforeClass;
import specs.BaseFixture;
import specs.response.BasePaymentFixture;

import java.net.MalformedURLException;

/**
 * Base class for second tenant payments tests. It overrides CTP client, type cache map, own tenant name.
 */
abstract public class BaseTenant2Fixture extends BasePaymentFixture {

    private static BlockingSphereClient ctpClient2;

    private static LoadingCache<String, Type> typeCache2;

    protected static String project2Key;

    @BeforeClass
    static public void initializeCommercetoolsClient() throws MalformedURLException {
        BaseFixture.initializeCommercetoolsClient();
        project2Key = propertyProvider.getMandatoryNonEmptyProperty(TEST_DATA_CT_PROJECT_KEY_2);

        ctpClient2 = BlockingSphereClient.of(
                SphereClientFactory.of().createClient(project2Key,
                        propertyProvider.getMandatoryNonEmptyProperty(TEST_DATA_CT_CLIENT_ID_2),
                        propertyProvider.getMandatoryNonEmptyProperty(TEST_DATA_CT_CLIENT_SECRET_2)),
                CTP_REQUEST_TIMEOUT
        );

        typeCache2 = CacheBuilder.newBuilder().build(new TypeCacheLoader(ctpClient2));
    }

    @Override
    protected BlockingSphereClient ctpClient() {
        return ctpClient2;
    }

    @Override
    public LoadingCache<String, Type> getTypeCache() {
        return typeCache2;
    }

    @Override
    public String getTenantName() {
        return getConfigurationParameter(TEST_DATA_TENANT_NAME_2);
    }

    /**
     * Explicitly use ctpClient2 instance.
     */
    protected Payment fetchPaymentByNameFromTenant2(String paymentName) {
        String paymentId = getIdForLegibleName(paymentName);
        return ctpClient().executeBlocking(PaymentByIdGet.of(
                Preconditions.checkNotNull(paymentId, "paymentId must not be null!")));
    }
}
