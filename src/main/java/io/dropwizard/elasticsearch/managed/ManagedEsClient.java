package io.dropwizard.elasticsearch.managed;

import com.google.common.io.Resources;
import io.dropwizard.elasticsearch.config.EsConfiguration;
import io.dropwizard.elasticsearch.util.TransportAddressHelper;
import io.dropwizard.lifecycle.Managed;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.ElasticsearchHostsSniffer;
import org.elasticsearch.client.sniff.HostsSniffer;
import org.elasticsearch.client.sniff.SniffOnFailureListener;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * A Dropwizard managed Elasticsearch {@link Client} for Elasticsearch 5.
 * <p>
 * Elasticsearch 5 no longer allows using a Node Client to connect to the service. The advice
 * is to run a local coordinating node (with whichever plugins you require), and to use the
 * {@link TransportClient} to connect to your cluster via that node.
 * <p>
 * If the {@code nodeClient} configuration option is selected, the client will fail and
 * throw an {@link UnsupportedOperationException}.
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/client-connected-to-client-node.html">Connecting a Client to a Coordinating Only Node</a>
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/transport-client.html">Transport Client</a>
 */
public class ManagedEsClient implements Managed {

    private Client client;
    private RestHighLevelClient restHighLevelClient;
    private Sniffer sniffer;

    /**
     * Create a new managed Elasticsearch {@link Client}. A {@link TransportClient} will be created with {@link EsConfiguration#servers}
     * as transport addresses.
     *
     * @param config a valid {@link EsConfiguration} instance
     * @throws IOException                   if a settings file has been specified and cannot be read.
     * @throws UnsupportedOperationException if {@code nodeClient=true} has been configured. This version
     *                                       of Elasticsearch does not provide a NodeClient.
     */
    public ManagedEsClient(final EsConfiguration config) throws IOException {
        checkNotNull(config, "EsConfiguration must not be null");

        // Initialise the settings
        final Settings.Builder settingsBuilder = Settings.builder();
        // If a settings file is given, read settings from there
        if (!isNullOrEmpty(config.getSettingsFile())) {
            Path path = Paths.get(config.getSettingsFile());
            if (!path.toFile().exists()) {
                try {
                    final URL url = Resources.getResource(config.getSettingsFile());
                    path = new File(url.toURI()).toPath();
                } catch (URISyntaxException | NullPointerException e) {
                    throw new IllegalArgumentException("settings file cannot be found", e);
                }
            }
            settingsBuilder.loadFromPath(path);
        }

        // Add any additional user-specific settings
        if (!config.getSettings().isEmpty()) {
            config.getSettings().forEach(settingsBuilder::put);
        }

        final Settings settings = settingsBuilder
                .put("cluster.name", config.getClusterName())
                .build();

        if (config.isTransportClient()) {
            final TransportAddress[] addresses = TransportAddressHelper.fromStrings(config.getServers());
            this.client = new PreBuiltTransportClient(settings).addTransportAddresses(addresses);
        } else {
            // Build a REST client
            HttpHost[] hosts = config.getServers().stream().map(HttpHost::create).toArray(HttpHost[]::new);
            RestClientBuilder clientBuilder = RestClient.builder(hosts);
            if (!config.getHeaders().isEmpty()) {
                Header[] headers = config.getHeaders().entrySet().stream()
                        .map(e -> new BasicHeader(e.getKey(), e.getValue()))
                        .toArray(BasicHeader[]::new);
                clientBuilder.setDefaultHeaders(headers);
            }

            // If Sniffer is enabled, initialise that too
            if (config.getSniffer().isEnabled()) {
                SniffOnFailureListener failureListener =  null;
                if (config.getSniffer().isSniffOnFailure()) {
                    failureListener = new SniffOnFailureListener();
                    clientBuilder.setFailureListener(failureListener);
                }
                this.restHighLevelClient = new RestHighLevelClient(clientBuilder);

                SnifferBuilder snifferBuilder = Sniffer.builder(restHighLevelClient.getLowLevelClient())
                        .setSniffIntervalMillis(config.getSniffer().getSniffIntervalMillis())
                        .setSniffAfterFailureDelayMillis(config.getSniffer().getSniffFailureMillis());
                if (config.getSniffer().isUseHttps()) {
                    HostsSniffer hostsSniffer = new ElasticsearchHostsSniffer(restHighLevelClient.getLowLevelClient(),
							ElasticsearchHostsSniffer.DEFAULT_SNIFF_REQUEST_TIMEOUT,
							ElasticsearchHostsSniffer.Scheme.HTTPS);
                    snifferBuilder.setHostsSniffer(hostsSniffer);
                }
                this.sniffer = snifferBuilder.build();
                if (failureListener != null) {
                    failureListener.setSniffer(sniffer);
                }
            } else {
				this.restHighLevelClient = new RestHighLevelClient(clientBuilder);
			}
        }
    }


    /**
     * Create a new managed Elasticsearch {@link Client} from the provided {@link Client}.
     *
     * @param client an initialized {@link Client} instance
     */
    public ManagedEsClient(Client client) {
        this.client = checkNotNull(client, "Elasticsearch client must not be null");
    }

    /**
     * Starts the Elasticsearch {@link Node} (if appropriate). Called <i>before</i> the service becomes available.
     *
     * @throws Exception if something goes wrong; this will halt the service startup.
     */
    @Override
    public void start() throws Exception {
    }

    /**
     * Stops the Elasticsearch {@link Client} and (if appropriate) {@link Node} objects. Called <i>after</i> the service
     * is no longer accepting requests.
     *
     * @throws Exception if something goes wrong.
     */
    @Override
    public void stop() throws Exception {
        closeClient();
        closeSniffer();
        closeRestClient();
    }

    /**
     * Get the managed Elasticsearch {@link Client} instance.
     *
     * @return a valid Elasticsearch {@link Client} instance
     */
    public Client getClient() {
        return client;
    }

    /**
     * Get the low-level REST client.
     *
     * @return the REST client, or {@code null} if using the Transport client.
     */
    public RestClient getRestClient() {
        return restHighLevelClient.getLowLevelClient();
    }

	/**
	 * Get the high-level REST client.
	 *
	 * @return the high-level REST client, or {@code null} if using the Transport client.
	 */
	public RestHighLevelClient getRestHighLevelClient() {
		return restHighLevelClient;
	}

	private void closeClient() {
        if (null != client) {
            client.close();
        }
    }

    private void closeSniffer() throws IOException {
        if (null != sniffer) {
            sniffer.close();
        }
    }

    private void closeRestClient() throws IOException {
        if (null != restHighLevelClient) {
			restHighLevelClient.close();
        }
    }

}
