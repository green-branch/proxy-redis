package com.amehta.proxy.redis.interact;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CachedRedisServiceManagerTest {

    private static Logger LOGGER = LoggerFactory.getLogger(CachedRedisServiceManagerTest.class);
    private static int cacheTimeout = 5;
    private CachedRedisServiceManager cachedRedisServiceManager;
    private JedisPool jedisPool = mock(JedisPool.class);
    private Jedis jedis = mock(Jedis.class);
    private ImmutablePair<String, String> kv1 = ImmutablePair.of("k1", "v1");
    private ImmutablePair<String, String> kv2 = ImmutablePair.of("k2", "v2");

    @Before
    public void setUp() {
        when(jedisPool.getResource())
                .thenReturn(jedis);
        when(jedis.get(kv1.getKey()))
                .thenReturn(kv1.getValue());
        when(jedis.get(kv2.getKey()))
                .thenReturn(kv2.getValue());
        cachedRedisServiceManager = new CachedRedisServiceManager(jedisPool,
                1,
                cacheTimeout,
                1);
    }

    @After
    public void tearDown() {
        reset(jedis, jedisPool);
    }

    @Test
    public void testCacheMissFlow() throws ExecutionException {
        Optional<String> value = cachedRedisServiceManager.getValue(kv1.getKey());
        assertEquals(kv1.getValue(), value.get());
        verify(jedis, times(1)).get(kv1.getKey());
    }

    @Test
    public void testCacheHitFlow() throws ExecutionException {
        Optional<String> value = cachedRedisServiceManager.getValue(kv1.getKey());
        Optional<String> valueAgain = cachedRedisServiceManager.getValue(kv1.getKey());
        assertEquals(kv1.getValue(), value.get());
        assertEquals(kv1.getValue(), valueAgain.get());
        verify(jedis, times(1)).get(kv1.getKey());
    }

    @Test
    public void testCacheLRUPropertySize() throws ExecutionException {
        // cacheSize is set to 1
        Optional<String> v1 = cachedRedisServiceManager.getValue(kv1.getKey());
        Optional<String> v2 = cachedRedisServiceManager.getValue(kv2.getKey());
        Optional<String> v1Again = cachedRedisServiceManager.getValue(kv1.getKey());
        assertEquals(kv1.getValue(), v1.get());
        assertEquals(kv2.getValue(), v2.get());
        assertEquals(kv1.getValue(), v1Again.get());
        verify(jedis, times(2)).get(kv1.getKey());
        verify(jedis, times(1)).get(kv2.getKey());
    }

    @Test
    public void testCacheLRUPropertyTimeout() throws ExecutionException, InterruptedException {
        // sleep for more than cacheTimeout
        Optional<String> v1 = cachedRedisServiceManager.getValue(kv1.getKey());
        assertEquals(kv1.getValue(), v1.get());
        int sleepTime = cacheTimeout + 2;
        LOGGER.info(format("About to sleep for %d seconds to test cache timeout", sleepTime));
        Thread.sleep(sleepTime * 1000L);
        Optional<String> v1Again = cachedRedisServiceManager.getValue(kv1.getKey());
        assertEquals(kv1.getValue(), v1.get());
        assertEquals(kv1.getValue(), v1Again.get());
        verify(jedis, times(2)).get(kv1.getKey());
    }
}
