package io.dropwizard.elasticsearch.managed;

import com.google.common.net.HostAndPort;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.elasticsearch.config.EsConfiguration;
import io.dropwizard.elasticsearch.util.TransportAddressHelper;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.lifecycle.Managed;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ManagedEsClient}.
 */
public class ManagedEsClientTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ConfigurationFactory<EsConfiguration> configFactory =
            new DefaultConfigurationFactoryFactory<EsConfiguration>()
                    .create(EsConfiguration.class, validator, Jackson.newObjectMapper(), "dw");

    private ManagedEsClient managedEsClient;

    @Before
    public void setup() {
        managedEsClient = null;
    }

    @After
    public void closeClient() throws Exception {
        if (managedEsClient != null) {
            managedEsClient.stop();
        }
    }

    @Test(expected = NullPointerException.class)
    public void ensureEsConfigurationIsNotNull() throws Exception {
        new ManagedEsClient((EsConfiguration) null);
    }

    @Test(expected = NullPointerException.class)
    public void ensureClientIsNotNull() {
        new ManagedEsClient((Client) null);
    }

    @Test
    public void stopShouldCloseTheClient() throws Exception {
        Client client = mock(Client.class);
        Managed managed = new ManagedEsClient(client);

        managed.start();
        managed.stop();

        verify(client).close();
    }

    @Test(expected = ConfigurationException.class)
    public void nodeClientDisallowed() throws URISyntaxException, IOException, ConfigurationException {
        URL configFileUrl = this.getClass().getResource("/node_client.yml");
        File configFile = new File(configFileUrl.toURI());
        EsConfiguration config = configFactory.build(configFile);

        try {
            managedEsClient = new ManagedEsClient(config);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void transportClientShouldBeCreatedFromConfig() throws URISyntaxException, IOException, ConfigurationException {
        URL configFileUrl = this.getClass().getResource("/transport_client.yml");
        File configFile = new File(configFileUrl.toURI());
        EsConfiguration config = configFactory.build(configFile);

        managedEsClient = new ManagedEsClient(config);
        Client client = managedEsClient.getClient();

        assertNotNull(client);
        assertTrue(client instanceof TransportClient);

        final TransportClient transportClient = (TransportClient) client;
        assertEquals(3, transportClient.transportAddresses().size());
        assertEquals(
                TransportAddressHelper.fromHostAndPort(HostAndPort.fromParts("127.0.0.1", 9300)),
                transportClient.transportAddresses().get(0));
        assertEquals(
                TransportAddressHelper.fromHostAndPort(HostAndPort.fromParts("127.0.0.1", 9301)),
                transportClient.transportAddresses().get(1));
        assertEquals(
                TransportAddressHelper.fromHostAndPort(HostAndPort.fromParts("127.0.0.1", 9302)),
                transportClient.transportAddresses().get(2));
    }

    @Test @Ignore
    public void managedClientShouldUseCustomElasticsearchConfig() throws URISyntaxException, IOException, ConfigurationException {
        URL configFileUrl = this.getClass().getResource("/custom_settings_file.yml");
        File configFile = new File(configFileUrl.toURI());
        EsConfiguration config = configFactory.build(configFile);

        managedEsClient = new ManagedEsClient(config);
        Client client = managedEsClient.getClient();

        assertNotNull(client);
        assertTrue(client instanceof TransportClient);

        TransportClient transportClient = (TransportClient) client;
        assertEquals(config.getClusterName(), transportClient.settings().get("cluster.name"));
        assertEquals("19300-19400", transportClient.settings().get("transport.tcp.port"));
    }

    @Test @Ignore
    public void managedClientObeysPrecedenceOfSettings() throws URISyntaxException, IOException, ConfigurationException {
        URL configFileUrl = this.getClass().getResource("/custom_settings_precedence.yml");
        File configFile = new File(configFileUrl.toURI());
        EsConfiguration config = configFactory.build(configFile);

        managedEsClient = new ManagedEsClient(config);
        Client client = managedEsClient.getClient();

        assertNotNull(client);
        assertTrue(client instanceof TransportClient);

        TransportClient transportClient = (TransportClient) client;
        assertEquals(config.getClusterName(), transportClient.settings().get("cluster.name"));
        assertEquals("29300-29400", transportClient.settings().get("transport.tcp.port"));
        assertEquals("target/data/yaml", transportClient.settings().get("path.home"));
    }
}
