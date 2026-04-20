package com.rps.samaj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "samaj")
public class SamajProperties {

    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private Bootstrap bootstrap = new Bootstrap();
    private Matrimony matrimony = new Matrimony();
    private Cors cors = new Cors();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public Matrimony getMatrimony() {
        return matrimony;
    }

    public void setMatrimony(Matrimony matrimony) {
        this.matrimony = matrimony;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public static class Jwt {
        private String secret = "";
        /** Default 24h; override with samaj.jwt.access-ttl-minutes */
        private int accessTtlMinutes = 1440;
        /** Default 30 days; override with samaj.jwt.refresh-ttl-days */
        private int refreshTtlDays = 30;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getAccessTtlMinutes() {
            return accessTtlMinutes;
        }

        public void setAccessTtlMinutes(int accessTtlMinutes) {
            this.accessTtlMinutes = accessTtlMinutes;
        }

        public int getRefreshTtlDays() {
            return refreshTtlDays;
        }

        public void setRefreshTtlDays(int refreshTtlDays) {
            this.refreshTtlDays = refreshTtlDays;
        }
    }

    public static class Storage {
        /** LOCAL or S3 */
        private String provider = "LOCAL";
        private String root = "./data/samaj-uploads";
        private String publicBaseUrl = "http://localhost:8080/files";
        private S3 s3 = new S3();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public S3 getS3() {
            return s3;
        }

        public void setS3(S3 s3) {
            this.s3 = s3;
        }

        public static class S3 {
            private String bucket = "";
            private String region = "ap-south-1";
            private String accessKeyId = "";
            private String secretAccessKey = "";
            /** Optional: custom endpoint for R2 / MinIO */
            private String endpoint = "";

            public String getBucket() {
                return bucket;
            }

            public void setBucket(String bucket) {
                this.bucket = bucket;
            }

            public String getRegion() {
                return region;
            }

            public void setRegion(String region) {
                this.region = region;
            }

            public String getAccessKeyId() {
                return accessKeyId;
            }

            public void setAccessKeyId(String accessKeyId) {
                this.accessKeyId = accessKeyId;
            }

            public String getSecretAccessKey() {
                return secretAccessKey;
            }

            public void setSecretAccessKey(String secretAccessKey) {
                this.secretAccessKey = secretAccessKey;
            }

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }
        }
    }

    public static class Bootstrap {
        private ParentAdmin parentAdmin = new ParentAdmin();

        public ParentAdmin getParentAdmin() {
            return parentAdmin;
        }

        public void setParentAdmin(ParentAdmin parentAdmin) {
            this.parentAdmin = parentAdmin;
        }

        public static class ParentAdmin {
            private String email = "";
            private String password = "";

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }
    }

    /**
     * Extra browser origins for CORS. Use when you serve the same app on multiple hosts
     * (e.g. {@code https://example.com} vs {@code https://www.example.com} — those are different origins).
     */
    public static class Cors {
        private List<String> additionalOriginPatterns = new ArrayList<>();

        public List<String> getAdditionalOriginPatterns() {
            return additionalOriginPatterns;
        }

        public void setAdditionalOriginPatterns(List<String> additionalOriginPatterns) {
            this.additionalOriginPatterns = additionalOriginPatterns != null ? additionalOriginPatterns : new ArrayList<>();
        }
    }

    /** Optional integrations (chat webhooks, etc.). */
    public static class Matrimony {
        /** Shared secret for POST /api/v1/matrimony/webhooks/chat (header X-Matrimony-Webhook-Secret). */
        private String webhookSecret = "";

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }
}
