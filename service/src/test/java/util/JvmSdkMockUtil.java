package util;

import io.sphere.sdk.queries.PagedQueryResult;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mock CTP JVM SDK entities.
 */
public final class JvmSdkMockUtil {

    /**
     * Since {@link PagedQueryResult#of(Object)} and all related methods became deprecated - make this mocking for the
     * tests.
     * <p>
     * The returned result has count/limit/total equal to {@code results#length}, offset = 0.
     * <p>
     * {@link PagedQueryResult::head} returns optional of first {@code results} element, if exists.
     *
     * @param results list of values to return as {@link PagedQueryResult#getResults()}
     * @param <T>     type of values to return in the result
     * @return {@link PagedQueryResult} instance with "injected" {@code results}
     */
    @SafeVarargs
    @Nonnull
    public static <T> PagedQueryResult pagedQueryResultsMock(final T... results) {
        List<T> list = asList(results);
        PagedQueryResult res = mock(PagedQueryResult.class);
        Mockito.lenient().when(res.getCount()).thenReturn((long) list.size());
        Mockito.lenient().when(res.getLimit()).thenReturn((long) list.size());
        Mockito.lenient().when(res.getTotal()).thenReturn((long) list.size());
        Mockito.lenient().when(res.getOffset()).thenReturn(0L);
        Mockito.lenient().when(res.getResults()).thenReturn(list);
        Mockito.lenient().when(res.head()).thenReturn(list.stream().findFirst());
        return res;
    }

    private JvmSdkMockUtil() {
    }
}
