package com.github.patrickbcullen.profile;

import org.apache.kafka.streams.state.HostInfo;

import java.util.Set;

public class HostStoreInfo {

    private String host;
    private int port;
    private Set<String> storeNames;

    public HostStoreInfo(final String host, final int port, final Set<String> storeNames) {
        this.host = host;
        this.port = port;
        this.storeNames = storeNames;
    }

    public boolean equivalent(HostInfo hostInfo) {
        return hostInfo.host().equals(host) && hostInfo.port() == port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
