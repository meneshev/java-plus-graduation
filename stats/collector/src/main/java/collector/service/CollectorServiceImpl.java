package collector.service;

import collector.kafka.CollectorKafkaClient;
import collector.mapper.UserActionsAvroMapper;
import config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.stats.user.UserActionProto;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {
    private final KafkaProperties kafkaProperties;
    private final CollectorKafkaClient collectorKafkaClient;

    @Override
    public void sendMessage(UserActionProto userActionProto) {
        collectorKafkaClient.getProducer().send(
               new ProducerRecord<>(kafkaProperties.getTopics().getUserActions(),
                       null,
                       UserActionsAvroMapper.toUserActionAvro(userActionProto)
               )
        );
        log.info("Sent user actions to collector: {}", userActionProto);
    }
}