package com.example.redisconnectionfactory.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

public class MasterReplicaRedisConnectionFactory implements RedisConnectionFactory, InitializingBean, DisposableBean {
    private final LettuceConnectionFactory delegate;

    public MasterReplicaRedisConnectionFactory(LettuceConnectionFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void destroy() throws Exception {
        delegate.destroy();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        delegate.afterPropertiesSet();
    }

    @Override
    public RedisConnection getConnection() {
        return delegate.getConnection();
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        return delegate.getClusterConnection();
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return delegate.getConvertPipelineAndTxResults();
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return delegate.getSentinelConnection();
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return delegate.translateExceptionIfPossible(ex);
    }
}
