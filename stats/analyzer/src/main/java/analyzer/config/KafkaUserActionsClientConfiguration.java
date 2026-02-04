package analyzer.config;

import analyzer.kafka.AnalyzerUserActionsKafkaClient;
import config.KafkaProperties;
import kafka.UserActionAvroDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaUserActionsClientConfiguration {
    private final KafkaProperties kafkaProperties;

    @Bean
    @Scope("prototype")
    AnalyzerUserActionsKafkaClient kafkaClient() {
        return new AnalyzerUserActionsKafkaClient() {
            private Consumer<Void, UserActionAvro> consumer;

            @Override
            public Consumer<Void, UserActionAvro> getConsumer() {
                if (consumer == null) {
                    initConsumer();
                }
                return consumer;
            }

            private void initConsumer() {
                Properties props = new Properties();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
                props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaProperties.getKeyDeserializer());
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class.getName());
                props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroups().getAnalyzerUserActions());
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaProperties.getConsumer().isAutoCommit());

                consumer = new KafkaConsumer<>(props);

                consumer.subscribe(List.of(kafkaProperties.getTopics().getUserActions()));
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
