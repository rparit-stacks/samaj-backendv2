package com.rps.samaj.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.AbstractMap;

@Configuration
public class RedisCacheConfig implements CachingConfigurer {

    /** Cache names (keep stable; used as Redis key prefixes). */
    public static final class Names {
        private Names() {
        }

        public static final String MY_PROFILE = "myProfile";
        public static final String MY_SETTINGS = "mySettings";
        public static final String MY_PRIVACY = "myPrivacy";
        public static final String DIRECTORY_LIST = "directoryList";
        public static final String DIRECTORY_DETAIL = "directoryDetail";
        public static final String NEWS_CATEGORIES = "newsCategories";
        public static final String NEWS_ARTICLES = "newsArticles";

        // Events
        public static final String EVENTS_LIST = "eventsList";
        public static final String EVENT_DETAIL = "eventDetail";
        public static final String EVENT_ANALYTICS = "eventAnalytics";

        // CMS / app config
        public static final String CMS_BANNERS_ACTIVE = "cmsBannersActive";
        public static final String CMS_BANNERS_ALL = "cmsBannersAll";

        // History
        public static final String HISTORY_ADMIN_LIST = "historyAdminList";
        public static final String HISTORY_ADMIN_DETAIL = "historyAdminDetail";
        public static final String HISTORY_PUBLIC_LIST = "historyPublicList";
        public static final String HISTORY_PUBLIC_DETAIL = "historyPublicDetail";

        // Admin system/users
        public static final String ADMIN_ME = "adminMe";
        public static final String ADMIN_SERVICE_CATALOG = "adminServiceCatalog";
        public static final String ADMIN_CHILD_ADMINS = "adminChildAdmins";
        public static final String ADMIN_USERS_LIST = "adminUsersList";
        public static final String ADMIN_USER_DETAIL = "adminUserDetail";

        // Emergency
        public static final String EMERGENCY_LIST = "emergencyList";
        public static final String EMERGENCY_DETAIL = "emergencyDetail";

        // Community/feed
        public static final String COMMUNITY_POSTS = "communityPosts";

        // Gallery
        public static final String GALLERY_APPROVED = "galleryApproved";
        public static final String GALLERY_ALBUM = "galleryAlbum";

        // Documents
        public static final String DOCUMENTS_PUBLISHED = "documentsPublished";
        public static final String DOCUMENT_DETAIL = "documentDetail";

        // Achievers
        public static final String ACHIEVERS_MARQUEE = "achieversMarquee";
        public static final String ACHIEVERS_APPROVED = "achieversApproved";
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON serializer so we can cache DTOs cleanly.
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = RedisSerializer.json();

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> perCache = Map.ofEntries(
                new AbstractMap.SimpleEntry<>(Names.MY_PROFILE, base.entryTtl(Duration.ofSeconds(60))),
                new AbstractMap.SimpleEntry<>(Names.MY_SETTINGS, base.entryTtl(Duration.ofSeconds(60))),
                new AbstractMap.SimpleEntry<>(Names.MY_PRIVACY, base.entryTtl(Duration.ofSeconds(60))),
                new AbstractMap.SimpleEntry<>(Names.DIRECTORY_LIST, base.entryTtl(Duration.ofMinutes(10))),
                new AbstractMap.SimpleEntry<>(Names.DIRECTORY_DETAIL, base.entryTtl(Duration.ofMinutes(10))),
                new AbstractMap.SimpleEntry<>(Names.NEWS_CATEGORIES, base.entryTtl(Duration.ofMinutes(30))),
                new AbstractMap.SimpleEntry<>(Names.NEWS_ARTICLES, base.entryTtl(Duration.ofMinutes(2))),

                // Events: user-specific fields (RSVP) → short TTL
                new AbstractMap.SimpleEntry<>(Names.EVENTS_LIST, base.entryTtl(Duration.ofSeconds(30))),
                new AbstractMap.SimpleEntry<>(Names.EVENT_DETAIL, base.entryTtl(Duration.ofSeconds(30))),
                new AbstractMap.SimpleEntry<>(Names.EVENT_ANALYTICS, base.entryTtl(Duration.ofMinutes(2))),

                // CMS banners: rarely changes
                new AbstractMap.SimpleEntry<>(Names.CMS_BANNERS_ACTIVE, base.entryTtl(Duration.ofMinutes(15))),
                new AbstractMap.SimpleEntry<>(Names.CMS_BANNERS_ALL, base.entryTtl(Duration.ofMinutes(10))),

                // History: mostly read-only
                new AbstractMap.SimpleEntry<>(Names.HISTORY_PUBLIC_LIST, base.entryTtl(Duration.ofMinutes(30))),
                new AbstractMap.SimpleEntry<>(Names.HISTORY_PUBLIC_DETAIL, base.entryTtl(Duration.ofMinutes(30))),
                new AbstractMap.SimpleEntry<>(Names.HISTORY_ADMIN_LIST, base.entryTtl(Duration.ofMinutes(5))),
                new AbstractMap.SimpleEntry<>(Names.HISTORY_ADMIN_DETAIL, base.entryTtl(Duration.ofMinutes(10))),

                // Admin: short TTL to reflect permission changes quickly
                new AbstractMap.SimpleEntry<>(Names.ADMIN_ME, base.entryTtl(Duration.ofSeconds(45))),
                new AbstractMap.SimpleEntry<>(Names.ADMIN_SERVICE_CATALOG, base.entryTtl(Duration.ofHours(6))),
                new AbstractMap.SimpleEntry<>(Names.ADMIN_CHILD_ADMINS, base.entryTtl(Duration.ofSeconds(60))),
                new AbstractMap.SimpleEntry<>(Names.ADMIN_USERS_LIST, base.entryTtl(Duration.ofSeconds(60))),
                new AbstractMap.SimpleEntry<>(Names.ADMIN_USER_DETAIL, base.entryTtl(Duration.ofSeconds(60)))
                ,
                // Emergency: changes frequently but read a lot
                new AbstractMap.SimpleEntry<>(Names.EMERGENCY_LIST, base.entryTtl(Duration.ofSeconds(20))),
                new AbstractMap.SimpleEntry<>(Names.EMERGENCY_DETAIL, base.entryTtl(Duration.ofSeconds(30))),

                // Community feed
                new AbstractMap.SimpleEntry<>(Names.COMMUNITY_POSTS, base.entryTtl(Duration.ofSeconds(20))),

                // Gallery
                new AbstractMap.SimpleEntry<>(Names.GALLERY_APPROVED, base.entryTtl(Duration.ofMinutes(5))),
                new AbstractMap.SimpleEntry<>(Names.GALLERY_ALBUM, base.entryTtl(Duration.ofMinutes(5))),

                // Documents
                new AbstractMap.SimpleEntry<>(Names.DOCUMENTS_PUBLISHED, base.entryTtl(Duration.ofMinutes(3))),
                new AbstractMap.SimpleEntry<>(Names.DOCUMENT_DETAIL, base.entryTtl(Duration.ofMinutes(5))),

                // Achievers
                new AbstractMap.SimpleEntry<>(Names.ACHIEVERS_MARQUEE, base.entryTtl(Duration.ofMinutes(10))),
                new AbstractMap.SimpleEntry<>(Names.ACHIEVERS_APPROVED, base.entryTtl(Duration.ofMinutes(5)))
        );

        RedisCacheManager redis = RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory))
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();

        // If Redis is down, app should still work (just no caching).
        CompositeCacheManager composite = new CompositeCacheManager(redis);
        composite.setFallbackToNoOpCache(true);
        return composite;
    }

    @Override
    public SimpleCacheErrorHandler errorHandler() {
        // Never fail requests due to cache errors.
        return new SimpleCacheErrorHandler();
    }
}

