package analyzer.service;

import analyzer.kafka.AnalyzerUserActionsKafkaClient;
import config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerUserActionsProcessor implements Runnable {
    private final KafkaProperties props;
    private final AnalyzerService analyzerService;
    private final AnalyzerUserActionsKafkaClient client;

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(client.getConsumer()::wakeup));
        try {
            while (true) {
                log.info("Getting user-actions...");
                processRecords(client.getConsumer().poll(props.getConsumer().getTimeoutMs()));
            }
        } catch (WakeupException ignored) {

        } catch (Exception e) {
            log.error("Error during process topic {}", props.getTopics().getEventsSimilarity(), e);
        } finally {
            try {
                client.getConsumer().commitSync(currentOffsets);
            } finally {
                log.info("Closing analyzer user-actions consumer...");
                client.stop();
            }
        }
    }

    private void processRecords(ConsumerRecords<Void, UserActionAvro> records) {
        for (ConsumerRecord<Void, UserActionAvro> record : records) {
            log.info("Processing record - topic:[{}] partition:[{}] offset:[{}] value: {}",
                    record.topic(), record.partition(), record.offset(), record.value());
            if (analyzerService.processUserAction(record.value())) {
                client.getConsumer().commitSync();
            }
        }
    }
}
