package io.dropwizard.elasticsearch.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.net.HostAndPort;
import io.dropwizard.validation.ValidationMethod;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for Elasticsearch related settings.
 */
public class EsConfiguration {
    @JsonProperty
    @NotNull
    private List<String> servers = Collections.emptyList();

    @JsonProperty
    @NotEmpty
    private String clusterName = "elasticsearch";

    @JsonProperty
    @NotNull
    private Map<String, String> settings = Collections.emptyMap();

    @JsonProperty
    private Map<String, String> headers = Collections.emptyMap();

    @JsonProperty
    private String settingsFile = null;

    @JsonProperty
    private boolean transportClient = false;

    @JsonProperty
    private EsSnifferConfiguration sniffer = new EsSnifferConfiguration();

    public List<String> getServers() {
        return servers;
    }

    public String getClusterName() {
        return clusterName;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public String getSettingsFile() {
        return settingsFile;
    }

    public boolean isTransportClient() {
        return transportClient;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public EsSnifferConfiguration getSniffer() {
        return sniffer;
    }

    @ValidationMethod
    @JsonIgnore
    public boolean isValidConfig() {
        return !servers.isEmpty();
    }
}
