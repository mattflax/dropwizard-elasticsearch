package io.dropwizard.elasticsearch.health;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;

import javax.ws.rs.HttpMethod;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link HealthCheck} which checks the cluster state of an Elasticsearch cluster.
 *
 * @see <a href="http://www.elasticsearch.org/guide/reference/api/admin-cluster-health/">Admin Cluster Health</a>
 */
public class EsClusterHealthCheck extends HealthCheck {

    static final String CLUSTER_HEALTH_ENDPOINT = "/_cluster/health";

    private final Client client;
    private final RestHighLevelClient restClient;
    private final boolean failOnYellow;

    /**
     * Construct a new Elasticsearch cluster health check using the
     * TransportClient.
     *
     * @param client       an Elasticsearch {@link Client} instance connected to the cluster
     * @param failOnYellow whether the health check should fail if the cluster health state is yellow
     */
    public EsClusterHealthCheck(Client client, boolean failOnYellow) {
        this(checkNotNull(client), null, failOnYellow);
    }

    /**
     * Construct a new Elasticsearch cluster health check which will fail if the cluster health state is
     * {@link ClusterHealthStatus#RED}.
     *
     * @param client an Elasticsearch {@link Client} instance connected to the cluster
     */
    public EsClusterHealthCheck(Client client) {
        this(client, false);
    }

    /**
     * Construct a new Elasticsearch cluster health check using the RestClient.
     * @param restClient a REST client configured for the cluster..
     * @param failOnYellow whether the health check should fail if the cluster health state is yellow
     */
    public EsClusterHealthCheck(RestHighLevelClient restClient, boolean failOnYellow) {
        this(null, checkNotNull(restClient), failOnYellow);
    }

    private EsClusterHealthCheck(Client client, RestHighLevelClient restClient, boolean failOnYellow) {
        this.client = client;
        this.restClient = restClient;
        this.failOnYellow = failOnYellow;
    }

    /**
     * Perform a check of the Elasticsearch cluster health.
     *
     * @return if the Elasticsearch cluster is healthy, a healthy {@link com.codahale.metrics.health.HealthCheck.Result};
     *         otherwise, an unhealthy {@link com.codahale.metrics.health.HealthCheck.Result} with a descriptive error
     *         message or exception
     * @throws Exception if there is an unhandled error during the health check; this will result in
     *                   a failed health check
     */
    @Override
    protected Result check() throws Exception {
        if (client != null) {
            return checkTransportClient();
        } else {
            return checkRestClient();
        }
    }

    private Result checkTransportClient() {
        final ClusterHealthStatus status = client.admin().cluster().prepareHealth().get().getStatus();
        return checkClusterHealthStatus(status);
    }

    private Result checkRestClient() throws Exception {
        Response response = restClient.getLowLevelClient().performRequest(HttpMethod.GET, CLUSTER_HEALTH_ENDPOINT);
        if (response.getStatusLine().getStatusCode() >= 300) {
            return Result.unhealthy("Status error from server: %d - %s", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        } else {
            String body = EntityUtils.toString(response.getEntity());
            Map jsonMap = new ObjectMapper().readValue(body, Map.class);
            if (jsonMap.containsKey("status")) {
                return checkClusterHealthStatus(ClusterHealthStatus.fromString(jsonMap.get("status").toString()));
            } else {
                return Result.unhealthy("No status in _cluster/health response - %s", body);
            }
        }
    }

    private Result checkClusterHealthStatus(ClusterHealthStatus status) {
        if (status == ClusterHealthStatus.RED || (failOnYellow && status == ClusterHealthStatus.YELLOW)) {
            return Result.unhealthy("Last status: %s", status.name());
        } else {
            return Result.healthy("Last status: %s", status.name());
        }
    }

}
