package io.dropwizard.elasticsearch.health;

import com.codahale.metrics.health.HealthCheck;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.HttpMethod;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EsClusterHealthCheck}
 */
public class EsClusterHealthCheckTest {

    private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);
    private static final StatusLine BAD_STATUS_LINE = new BasicStatusLine(PROTOCOL_VERSION, 500, "Server error");
    private static final StatusLine GOOD_STATUS_LINE = new BasicStatusLine(PROTOCOL_VERSION, 200, "OK");

    private final RestHighLevelClient highLevelClient = mock(RestHighLevelClient.class);
    private final RestClient lowLevelClient = mock(RestClient.class);

    @Before
    public void setup() {
        when(highLevelClient.getLowLevelClient()).thenReturn(lowLevelClient);
    }

    @Test(expected = NullPointerException.class)
    public void initializationWithNullTransportClientShouldFail() {
        new EsClusterHealthCheck(null);
    }

    @Test(expected = NullPointerException.class)
    public void initializationWithNullRestClientShouldFail() {
        new EsClusterHealthCheck((RestHighLevelClient) null, false);
    }

    @Test
    public void initializationWithClientShouldSucceed() {
        new EsClusterHealthCheck(mock(Client.class));
    }

    @Test(expected = IOException.class)
    public void restClientThrowsIOException() throws Exception {
        EsClusterHealthCheck healthCheck = new EsClusterHealthCheck(highLevelClient, true);
        when(lowLevelClient.performRequest(HttpMethod.GET, EsClusterHealthCheck.CLUSTER_HEALTH_ENDPOINT))
                .thenThrow(new IOException("Error"));

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void restClientUnhealthyWhenStatusIsBad() throws Exception {
        EsClusterHealthCheck healthCheck = new EsClusterHealthCheck(highLevelClient, true);
        Response response = mock(Response.class);
        when(response.getStatusLine()).thenReturn(BAD_STATUS_LINE);
        when(lowLevelClient.performRequest(HttpMethod.GET, EsClusterHealthCheck.CLUSTER_HEALTH_ENDPOINT))
                .thenReturn(response);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void restClientUnhealthyWhenStatusValueIsMissing() throws Exception {
        EsClusterHealthCheck healthCheck = new EsClusterHealthCheck(highLevelClient, true);
        Response response = mock(Response.class);
        when(response.getStatusLine()).thenReturn(GOOD_STATUS_LINE);
        when(response.getEntity()).thenReturn(new NStringEntity("{ }", ContentType.APPLICATION_JSON));
        when(lowLevelClient.performRequest(HttpMethod.GET, EsClusterHealthCheck.CLUSTER_HEALTH_ENDPOINT))
                .thenReturn(response);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void restClientUnhealthyWhenStatusIsRed() throws Exception {
        EsClusterHealthCheck healthCheck = new EsClusterHealthCheck(highLevelClient, true);
        Response response = mock(Response.class);
        when(response.getStatusLine()).thenReturn(GOOD_STATUS_LINE);
        when(response.getEntity()).thenReturn(new NStringEntity("{ \"status\": \"red\" }", ContentType.APPLICATION_JSON));
        when(lowLevelClient.performRequest(HttpMethod.GET, EsClusterHealthCheck.CLUSTER_HEALTH_ENDPOINT))
                .thenReturn(response);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void restClientUnhealthyWhenStatusIsYellow() throws Exception {
        EsClusterHealthCheck healthCheck = new EsClusterHealthCheck(highLevelClient, true);
        Response response = mock(Response.class);
        when(response.getStatusLine()).thenReturn(GOOD_STATUS_LINE);
        when(response.getEntity()).thenReturn(new NStringEntity("{ \"status\": \"yellow\" }", ContentType.APPLICATION_JSON));
        when(lowLevelClient.performRequest(HttpMethod.GET, EsClusterHealthCheck.CLUSTER_HEALTH_ENDPOINT))
                .thenReturn(response);

        HealthCheck.Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void restClientHealthyWhenStatusIsYellow() throws Exception {
        EsClusterHealthCheck healthCheck = new EsClusterHealthCheck(highLevelClient, false);
        Response response = mock(Response.class);
        when(response.getStatusLine()).thenReturn(GOOD_STATUS_LINE);
        when(response.getEntity()).thenReturn(new NStringEntity("{ \"status\": \"yellow\" }", ContentType.APPLICATION_JSON));
        when(lowLevelClient.performRequest(HttpMethod.GET, EsClusterHealthCheck.CLUSTER_HEALTH_ENDPOINT))
                .thenReturn(response);

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void restClientHealthyWhenStatusIsGreen() throws Exception {
        EsClusterHealthCheck healthCheck = new EsClusterHealthCheck(highLevelClient, false);
        Response response = mock(Response.class);
        when(response.getStatusLine()).thenReturn(GOOD_STATUS_LINE);
        when(response.getEntity()).thenReturn(new NStringEntity("{ \"status\": \"green\" }", ContentType.APPLICATION_JSON));
        when(lowLevelClient.performRequest(HttpMethod.GET, EsClusterHealthCheck.CLUSTER_HEALTH_ENDPOINT))
                .thenReturn(response);

        HealthCheck.Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }
}
