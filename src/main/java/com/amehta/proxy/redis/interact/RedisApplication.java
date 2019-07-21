package com.amehta.proxy.redis.interact;

import com.amehta.proxy.redis.interact.health.TemplateHealthCheck;
import com.google.common.cache.LoadingCache;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.util.Optional;


public class RedisApplication extends Application<RedisConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisApplication.class);
    private static CachedRedisService cachedRedisService;

    @Override
    public String getName() {
        return "redis-app";
    }

    @Override
    public void initialize(Bootstrap<RedisConfiguration> bootstrap) {
    }

    private JedisPool getJedisPool(String redisAddress, int redisPort, int jedisPoolSize) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(jedisPoolSize);
        return new JedisPool(config, redisAddress, redisPort);
    }


    @Override
    public void run(RedisConfiguration configuration,
                    Environment environment) {

        configuration.getThreadPoolSize();
        String redisAddress = configuration.getRedisAddress();
        int redisPort = configuration.getRedisPort();
        int jedisPoolSize = configuration.getJedisPoolSize();
        int cacheSize = configuration.getCacheSize();
        int cacheTimeout = configuration.getCacheTimeout();
        int cacheConcurrency = configuration.getCacheConcurrency();

        JedisPool jedisPool = getJedisPool(redisAddress, redisPort, jedisPoolSize);
        CachedRedisService cachedRedisService = new CachedRedisService(
            jedisPool,
            cacheSize,
            cacheTimeout,
            cacheConcurrency
        );

        final RedisAppResource resource = new RedisAppResource(
                cachedRedisService
        );

        final TemplateHealthCheck healthCheck =
                new TemplateHealthCheck();

        environment.healthChecks().register("template", healthCheck);
        environment.jersey().register(resource);
    }

    public static void main(String[] args) throws Exception {
        new RedisApplication().run(args);
    }

}