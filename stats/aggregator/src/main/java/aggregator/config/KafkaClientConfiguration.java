package aggregator.config;

import aggregator.kafka.AggregatorKafkaClient;
import config.KafkaProperties;
import kafka.CommonAvroSerializer;
import kafka.UserActionAvroDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaClientConfiguration {
    private final KafkaProperties kafkaProperties;

    @Bean
    @Scope("prototype")
    AggregatorKafkaClient kafkaClient() {
        return new AggregatorKafkaClient() {
            private Consumer<Void, UserActionAvro> consumer;
            private Producer<Void, EventSimilarityAvro> producer;

            @Override
            public Producer<Void, EventSimilarityAvro> getProducer() {
                if (producer == null) {
                    initProducer();
                }
                return producer;
            }

            @Override
            public Consumer<Void, UserActionAvro> getConsumer() {
                if (consumer == null) {
                    initConsumer();
                }
                return consumer;
            }

            private void initProducer() {
                Properties props = new Properties();
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProperties.getKeySerializer());
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CommonAvroSerializer.class.getName());
                props.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProperties.getProducer().getLingerMs());
                producer = new KafkaProducer<>(props);
            }

            private void initConsumer() {
                Properties props = new Properties();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
                props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaProperties.getKeyDeserializer());
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class.getName());
                props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroups().getAggregatorUserActions());
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaProperties.getConsumer().isAutoCommit());

                consumer = new KafkaConsumer<>(props);

                consumer.subscribe(List.of(kafkaProperties.getTopics().getUserActions()));
            }

            @Override
            public void stop() {
                if (producer != null) {
                    producer.close();
                }

                if (consumer != null) {
                    consumer.close();
                }
            }
        };
    }
}
