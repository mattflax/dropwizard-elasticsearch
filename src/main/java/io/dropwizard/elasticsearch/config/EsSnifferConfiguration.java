package io.dropwizard.elasticsearch.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EsSnifferConfiguration {

    @JsonProperty
    private boolean enabled = false;
    @JsonProperty
    private int sniffIntervalMillis = 600000;
    @JsonProperty
    private boolean sniffOnFailure = false;
    @JsonProperty
    private int sniffFailureMillis = 30000;
    @JsonProperty
    private boolean useHttps = false;

    public boolean isEnabled() {
        return enabled;
    }

    public int getSniffIntervalMillis() {
        return sniffIntervalMillis;
    }

    public boolean isSniffOnFailure() {
        return sniffOnFailure;
    }

    public int getSniffFailureMillis() {
        return sniffFailureMillis;
    }

    public boolean isUseHttps() {
        return useHttps;
    }
}
