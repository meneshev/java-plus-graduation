package config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.kafka.properties")
public class KafkaProperties {
    private String bootstrapServers;
    private String keySerializer;
    private String keyDeserializer;

    private Consumer consumer = new Consumer();
    private Producer producer = new Producer();
    private Topics topics = new Topics();


    @Getter
    @Setter
    public static class Consumer {
        private boolean autoCommit;
        private int timeoutMs;
        private Groups groups = new Groups();

        @Getter
        @Setter
        public static class Groups {
            private String aggregatorUserActions;
            private String analyzerUserActions;
            private String analyzerEventsSimilarity;
        }
    }

    @Getter
    @Setter
    public static class Producer {
        private int lingerMs;
    }

    @Getter
    @Setter
    public static class Topics {
        private String userActions;
        private String eventsSimilarity;
    }
}