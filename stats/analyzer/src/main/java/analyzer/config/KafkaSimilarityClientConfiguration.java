package analyzer.config;

import analyzer.kafka.AnalyzerSimilarityKafkaClient;
import config.KafkaProperties;
import kafka.EventSimilarityAvroDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.List;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaSimilarityClientConfiguration {
    private final KafkaProperties kafkaProperties;

    @Bean
    @Scope("prototype")
    AnalyzerSimilarityKafkaClient kafkaSimilarityClient() {
        return new AnalyzerSimilarityKafkaClient() {
            private Consumer<Void, EventSimilarityAvro> consumer;

            @Override
            public Consumer<Void, EventSimilarityAvro> getConsumer() {
                if (consumer == null) {
                    initConsumer();
                }
                return consumer;
            }

            private void initConsumer() {
                Properties props = new Properties();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
                props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaProperties.getKeyDeserializer());
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityAvroDeserializer.class.getName());
                props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroups().getAnalyzerEventsSimilarity());
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaProperties.getConsumer().isAutoCommit());

                consumer = new KafkaConsumer<>(props);

                consumer.subscribe(List.of(kafkaProperties.getTopics().getEventsSimilarity()));
            }

            @Override
            public void stop() {
                if (consumer != null) {
                    consumer.close();
                }
            }
        };
    }
}
