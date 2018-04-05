package io.dropwizard.elasticsearch.util;

import com.google.common.net.HostAndPort;
import org.apache.http.HttpHost;
import org.elasticsearch.common.transport.TransportAddress;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Helper class for converting Guava {@link HostAndPort} objects to Elasticsearch {@link TransportAddress}.
 */
public class TransportAddressHelper {
    private static final int DEFAULT_PORT = 9300;

    /**
     * Convert a {@link HostAndPort} instance to {@link TransportAddress}. If the {@link HostAndPort} instance doesn't
     * contain a port the resulting {@link TransportAddress} will have {@link #DEFAULT_PORT} as port.
     *
     * @param hostAndPort a valid {@link HostAndPort} instance
     * @return a {@link TransportAddress} equivalent to the provided {@link HostAndPort} instance
     */
    public static TransportAddress fromHostAndPort(final HostAndPort hostAndPort) {
        InetSocketAddress address = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPortOrDefault(DEFAULT_PORT));
        return new TransportAddress(address);
    }

    /**
     * Convert a list of {@link HostAndPort} instances to an array of {@link TransportAddress} instances.
     *
     * @param hostAndPorts a {@link List} of valid {@link HostAndPort} instances
     * @return an array of {@link TransportAddress} instances
     * @see #fromHostAndPort(com.google.common.net.HostAndPort)
     */
    public static TransportAddress[] fromHostAndPorts(final List<HostAndPort> hostAndPorts) {
        if (hostAndPorts == null) {
            return new TransportAddress[0];
        } else {
            TransportAddress[] transportAddresses = new TransportAddress[hostAndPorts.size()];

            for (int i = 0; i < hostAndPorts.size(); i++) {
                transportAddresses[i] = fromHostAndPort(hostAndPorts.get(i));
            }

            return transportAddresses;
        }
    }

    public static TransportAddress[] fromStrings(final List<String> servers) {
        TransportAddress[] addresses;

        if (servers == null) {
            addresses = new TransportAddress[0];
        } else {
            addresses = new TransportAddress[servers.size()];
            for (int i = 0; i < servers.size(); i ++) {
                addresses[i] = fromString(servers.get(i));
            }
        }

        return addresses;
    }

    private static TransportAddress fromString(String server) {
        HttpHost httpHost = HttpHost.create(server);
        InetSocketAddress address = new InetSocketAddress(httpHost.getHostName(), httpHost.getPort() == -1 ? DEFAULT_PORT : httpHost.getPort());
        return new TransportAddress(address);
    }
}
