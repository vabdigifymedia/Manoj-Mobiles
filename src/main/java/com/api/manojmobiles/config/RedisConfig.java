package com.api.manojmobiles.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig implements CachingConfigurer {

        private final RedisProperties redisProperties;

        /**
         * Creates the Jackson 3.x JSON serializer used across RedisTemplate and
         * CacheManager.
         * Uses the builder API from Spring Data Redis 4.x with default typing enabled
         * for safe polymorphic deserialization.
         */
        @SuppressWarnings("removal")
        private RedisSerializer<Object> jsonRedisSerializer() {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.findAndRegisterModules();
                mapper.activateDefaultTyping(
                                mapper.getPolymorphicTypeValidator(),
                                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,
                                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
                return new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer(mapper);
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                RedisSerializer<Object> jsonSerializer = jsonRedisSerializer();

                // Key serializers — always String for readable Redis keys
                StringRedisSerializer stringSerializer = new StringRedisSerializer();
                template.setKeySerializer(stringSerializer);
                template.setHashKeySerializer(stringSerializer);

                // Value serializers — JSON for complex objects
                template.setValueSerializer(jsonSerializer);
                template.setHashValueSerializer(jsonSerializer);

                template.afterPropertiesSet();
                return template;
        }

        /**
         * StringRedisTemplate for lightweight string-only operations.
         * Used by OTP, rate limiting, and password reset services
         * where values are simple strings (OTP codes, counters, token strings).
         */
        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
                return new StringRedisTemplate(connectionFactory);
        }

        /**
         * RedisCacheManager with per-cache TTL configuration.
         *
         * Default TTL: 1 hour (for any unnamed cache).
         * Named caches get their own TTL from RedisProperties:
         * - "products" → app.redis.cache.product-ttl
         * - "categories" → app.redis.cache.category-ttl
         */
        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                // Default cache config — used for any cache not explicitly configured
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(jsonRedisSerializer()))
                                .disableCachingNullValues();

                // Per-cache TTL overrides
                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                cacheConfigurations.put("products", defaultConfig
                                .entryTtl(redisProperties.getCache().getProductTtl()));

                cacheConfigurations.put("categories", defaultConfig
                                .entryTtl(redisProperties.getCache().getCategoryTtl()));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }

        /**
         * Custom error handler so that cache failures (e.g., Redis down)
         * don't crash the application — they fall back to the database silently.
         */
        @Override
        public CacheErrorHandler errorHandler() {
                return new SimpleCacheErrorHandler() {
                        @Override
                        public void handleCacheGetError(RuntimeException exception,
                                        org.springframework.cache.Cache cache,
                                        Object key) {
                                log.warn("Cache GET failed for cache='{}', key='{}'. Falling back to database. Error: {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCachePutError(RuntimeException exception,
                                        org.springframework.cache.Cache cache,
                                        Object key, Object value) {
                                log.warn("Cache PUT failed for cache='{}', key='{}'. Error: {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCacheEvictError(RuntimeException exception,
                                        org.springframework.cache.Cache cache,
                                        Object key) {
                                log.warn("Cache EVICT failed for cache='{}', key='{}'. Error: {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCacheClearError(RuntimeException exception,
                                        org.springframework.cache.Cache cache) {
                                log.warn("Cache CLEAR failed for cache='{}'. Error: {}",
                                                cache.getName(), exception.getMessage());
                        }
                };
        }
}
