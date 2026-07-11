package com.zbkj.common.utils;

import org.junit.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RedisUtilTest {

    @Test
    public void reusesConfiguredTemplatesAcrossOperations() {
        RedisConnectionFactory primaryFactory = mock(RedisConnectionFactory.class);
        RedisConnectionFactory secondaryFactory = mock(RedisConnectionFactory.class);

        RedisUtil redisUtil = new RedisUtil(primaryFactory, secondaryFactory);

        assertSame(redisUtil.getRedisTemplate(), redisUtil.getRedisTemplate());
        assertSame(redisUtil.getSecondRedisTemplate(), redisUtil.getSecondRedisTemplate());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void incrementCreatesMissingKeyAtomicallyWithoutPreflightLookup() {
        RedisTemplate<String, Object> primary = mock(RedisTemplate.class);
        RedisTemplate<String, Object> secondary = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(primary.opsForValue()).thenReturn(values);
        when(values.increment("counter", 1L)).thenReturn(1L);

        RedisUtil redisUtil = redisUtilUsing(primary, secondary);

        assertEquals(Long.valueOf(1L), redisUtil.incrAndCreate("counter"));
        verify(values).increment("counter", 1L);
        verify(primary, never()).hasKey("counter");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void secondaryDeleteNeverChecksThePrimaryDataSource() {
        RedisTemplate<String, Object> primary = mock(RedisTemplate.class);
        RedisTemplate<String, Object> secondary = mock(RedisTemplate.class);
        RedisUtil redisUtil = redisUtilUsing(primary, secondary);

        redisUtil.secondDelete("secondary-key");

        verify(secondary).delete("secondary-key");
        verify(primary, never()).hasKey("secondary-key");
    }

    private RedisUtil redisUtilUsing(RedisTemplate<String, Object> primary,
                                     RedisTemplate<String, Object> secondary) {
        RedisConnectionFactory primaryFactory = mock(RedisConnectionFactory.class);
        RedisConnectionFactory secondaryFactory = mock(RedisConnectionFactory.class);
        return new RedisUtil(primaryFactory, secondaryFactory) {
            @Override
            public RedisTemplate<String, Object> getRedisTemplate() {
                return primary;
            }

            @Override
            public RedisTemplate<String, Object> getSecondRedisTemplate() {
                return secondary;
            }
        };
    }
}
