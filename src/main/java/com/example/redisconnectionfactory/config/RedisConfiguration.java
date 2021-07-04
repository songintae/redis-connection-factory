package com.example.redisconnectionfactory.config;

import com.example.redisconnectionfactory.repository.KeyValueRepository;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import static io.lettuce.core.ReadFrom.REPLICA_PREFERRED;
import static org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import static org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.builder;


/**
 * RedisConnectionFactory 설정
 * Master/Replica ConnectionFactory는 특정기능(Pub/Sub)을 지원하지 않게 때문에 필요한곳에서 적절히 사용해야한다.
 */
@Configuration
@EnableConfigurationProperties(MasterReplicaRedisProperties.class)
public class RedisConfiguration {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
                                                    RedisProperties properties,
                                                    ClientResources clientResources) {
        LettuceClientConfigurationBuilder builder = getLettuceClientConfigurationBuilder(builderCustomizers, clientResources, properties);
        return new LettuceConnectionFactory(getStandaloneConfig(properties), builder.build());
    }

    @Bean
    public RedisConnectionFactory masterReplicaRedisConnectionFactory(ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
                                                         MasterReplicaRedisProperties masterReplicaRedisProperties,
                                                         ClientResources clientResources) {
        return new MasterReplicaRedisConnectionFactory(masterReplicaLettuceConnectionFactory(builderCustomizers, masterReplicaRedisProperties, clientResources));
    }

    @Bean
    public KeyValueRepository keyValueRepository(RedisConnectionFactory masterReplicaRedisConnectionFactory) {
        return new KeyValueRepository(masterReplicaRedisConnectionFactory);
    }

    private static LettuceConnectionFactory masterReplicaLettuceConnectionFactory(
            ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
            MasterReplicaRedisProperties masterReplicaRedisProperties,
            ClientResources clientResources) {

        RedisProperties master = masterReplicaRedisProperties.getMaster();
        //ClientConfig는 Master 기준으로 설정한다.
        LettuceClientConfigurationBuilder builder = getLettuceClientConfigurationBuilder(builderCustomizers, clientResources, master);

        //Replica 설정이 없을 경우 Master 기준으로 StandaloneConfig를 사용한다.
        if(masterReplicaRedisProperties.getReplicas().isEmpty()) {
            return new LettuceConnectionFactory(getStandaloneConfig(masterReplicaRedisProperties.getMaster()), builder.build());
        }

        //Replica 주소 설정
        RedisStaticMasterReplicaConfiguration configuration = new RedisStaticMasterReplicaConfiguration(master.getHost(), master.getPort());
        for (RedisProperties replica : masterReplicaRedisProperties.getReplicas()) {
            configuration.addNode(replica.getHost(), replica.getPort());
        }
        return new LettuceConnectionFactory(configuration, builder.readFrom(REPLICA_PREFERRED).build());
    }

    private static LettuceClientConfigurationBuilder getLettuceClientConfigurationBuilder(
            ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
            ClientResources clientResources,
            RedisProperties properties) {
        LettuceClientConfigurationBuilder builder = createBuilder(properties.getLettuce().getPool());
        applyProperties(builder, properties);
        if (StringUtils.hasText(properties.getUrl())) {
            customizeConfigurationFromUrl(builder, properties);
        }
        builder.clientOptions(createClientOptions(properties));
        builder.clientResources(clientResources);
        builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder;
    }

    protected static RedisStandaloneConfiguration getStandaloneConfig(RedisProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        if (StringUtils.hasText(properties.getUrl())) {
            ConnectionInfo connectionInfo = parseUrl(properties.getUrl());
            config.setHostName(connectionInfo.getHostName());
            config.setPort(connectionInfo.getPort());
            config.setUsername(connectionInfo.getUsername());
            config.setPassword(RedisPassword.of(connectionInfo.getPassword()));
        }
        else {
            config.setHostName(properties.getHost());
            config.setPort(properties.getPort());
            config.setUsername(properties.getUsername());
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
        config.setDatabase(properties.getDatabase());
        return config;
    }

    private static LettuceClientConfigurationBuilder createBuilder(RedisProperties.Pool pool) {
        if (pool == null) {
            return builder();
        }
        return new PoolBuilderFactory().createBuilder(pool);
    }

    private static LettuceClientConfigurationBuilder applyProperties(
            LettuceClientConfigurationBuilder builder,
            RedisProperties properties) {
        if (properties.isSsl()) {
            builder.useSsl();
        }
        if (properties.getTimeout() != null) {
            builder.commandTimeout(properties.getTimeout());
        }
        if (properties.getLettuce() != null) {
            RedisProperties.Lettuce lettuce = properties.getLettuce();
            if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout(properties.getLettuce().getShutdownTimeout());
            }
        }
        if (StringUtils.hasText(properties.getClientName())) {
            builder.clientName(properties.getClientName());
        }
        return builder;
    }

    private static ClientOptions createClientOptions(RedisProperties properties) {
        ClientOptions.Builder builder = initializeClientOptionsBuilder(properties);
        Duration connectTimeout = properties.getConnectTimeout();
        if (connectTimeout != null) {
            builder.socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build());
        }
        return builder.timeoutOptions(TimeoutOptions.enabled()).build();
    }

    private static ClientOptions.Builder initializeClientOptionsBuilder(RedisProperties properties) {
        if (properties.getCluster() != null) {
            ClusterClientOptions.Builder builder = ClusterClientOptions.builder();
            RedisProperties.Lettuce.Cluster.Refresh refreshProperties = properties.getLettuce().getCluster().getRefresh();
            ClusterTopologyRefreshOptions.Builder refreshBuilder = ClusterTopologyRefreshOptions.builder()
                    .dynamicRefreshSources(refreshProperties.isDynamicRefreshSources());
            if (refreshProperties.getPeriod() != null) {
                refreshBuilder.enablePeriodicRefresh(refreshProperties.getPeriod());
            }
            if (refreshProperties.isAdaptive()) {
                refreshBuilder.enableAllAdaptiveRefreshTriggers();
            }
            return builder.topologyRefreshOptions(refreshBuilder.build());
        }
        return ClientOptions.builder();
    }

    private static void customizeConfigurationFromUrl(LettuceClientConfigurationBuilder builder,
                                               RedisProperties properties) {
        ConnectionInfo connectionInfo = parseUrl(properties.getUrl());
        if (connectionInfo.isUseSsl()) {
            builder.useSsl();
        }
    }

    private static class PoolBuilderFactory {

        LettuceClientConfigurationBuilder createBuilder(RedisProperties.Pool properties) {
            return LettucePoolingClientConfiguration.builder().poolConfig(getPoolConfig(properties));
        }

        private GenericObjectPoolConfig<?> getPoolConfig(RedisProperties.Pool properties) {
            GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(properties.getMaxActive());
            config.setMaxIdle(properties.getMaxIdle());
            config.setMinIdle(properties.getMinIdle());
            if (properties.getTimeBetweenEvictionRuns() != null) {
                config.setTimeBetweenEvictionRunsMillis(properties.getTimeBetweenEvictionRuns().toMillis());
            }
            if (properties.getMaxWait() != null) {
                config.setMaxWaitMillis(properties.getMaxWait().toMillis());
            }
            return config;
        }
    }

    protected static ConnectionInfo parseUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!"redis".equals(scheme) && !"rediss".equals(scheme)) {
                throw new RedisUrlSyntaxException(url);
            }
            boolean useSsl = ("rediss".equals(scheme));
            String username = null;
            String password = null;
            if (uri.getUserInfo() != null) {
                String candidate = uri.getUserInfo();
                int index = candidate.indexOf(':');
                if (index >= 0) {
                    username = candidate.substring(0, index);
                    password = candidate.substring(index + 1);
                } else {
                    password = candidate;
                }
            }
            return new ConnectionInfo(uri, useSsl, username, password);
        } catch (URISyntaxException ex) {
            throw new RedisUrlSyntaxException(url, ex);
        }
    }

    static class ConnectionInfo {

        private final URI uri;

        private final boolean useSsl;

        private final String username;

        private final String password;

        ConnectionInfo(URI uri, boolean useSsl, String username, String password) {
            this.uri = uri;
            this.useSsl = useSsl;
            this.username = username;
            this.password = password;
        }

        boolean isUseSsl() {
            return this.useSsl;
        }

        String getHostName() {
            return this.uri.getHost();
        }

        int getPort() {
            return this.uri.getPort();
        }

        String getUsername() {
            return this.username;
        }

        String getPassword() {
            return this.password;
        }

    }
}
