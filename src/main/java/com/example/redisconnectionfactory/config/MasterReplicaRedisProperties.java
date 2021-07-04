package com.example.redisconnectionfactory.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;


@ConfigurationProperties(prefix = "spring.redis")
public class MasterReplicaRedisProperties {
    private final RedisProperties master = new RedisProperties();
    private List<RedisProperties> replicas;

    public int getDatabase() {
        return master.getDatabase();
    }

    public void setDatabase(int database) {
        master.setDatabase(database);
    }

    public String getUrl() {
        return master.getUrl();
    }

    public void setUrl(String url) {
        master.setUrl(url);
    }

    public String getHost() {
        return master.getHost();
    }

    public void setHost(String host) {
        master.setHost(host);
    }

    public String getUsername() {
        return master.getUsername();
    }

    public void setUsername(String username) {
        master.setUsername(username);
    }

    public String getPassword() {
        return master.getPassword();
    }

    public void setPassword(String password) {
        master.setPassword(password);
    }

    public int getPort() {
        return master.getPort();
    }

    public void setPort(int port) {
        master.setPort(port);
    }

    public boolean isSsl() {
        return master.isSsl();
    }

    public void setSsl(boolean ssl) {
        master.setSsl(ssl);
    }

    public void setTimeout(Duration timeout) {
        master.setTimeout(timeout);
    }

    public Duration getTimeout() {
        return master.getTimeout();
    }

    public Duration getConnectTimeout() {
        return master.getConnectTimeout();
    }

    public void setConnectTimeout(Duration connectTimeout) {
        master.setConnectTimeout(connectTimeout);
    }

    public String getClientName() {
        return master.getClientName();
    }

    public void setClientName(String clientName) {
        master.setClientName(clientName);
    }

    public void setLettuce(RedisProperties.Lettuce lettuce) {
        if(lettuce == null) {
            return;
        }
        master.getLettuce().setPool(lettuce.getPool());

        if(lettuce.getShutdownTimeout() != null) {
            master.getLettuce().setShutdownTimeout(lettuce.getShutdownTimeout());
        }

        RedisProperties.Lettuce.Cluster.Refresh refresh = lettuce.getCluster().getRefresh();
        master.getLettuce().getCluster().getRefresh().setAdaptive(refresh.isAdaptive());
        master.getLettuce().getCluster().getRefresh().setDynamicRefreshSources(refresh.isDynamicRefreshSources());
        master.getLettuce().getCluster().getRefresh().setPeriod(refresh.getPeriod());
    }

    public RedisProperties getMaster() {
        return master;
    }

    public List<RedisProperties> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<RedisProperties> replicas) {
        this.replicas = replicas;
    }
}
