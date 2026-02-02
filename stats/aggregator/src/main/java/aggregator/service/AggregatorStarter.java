package aggregator.service;

import aggregator.kafka.AggregatorKafkaClient;
import aggregator.model.Event;
import aggregator.model.User;
import com.netflix.appinfo.InstanceInfo;
import config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.practicum.ewm.stats.avro.ActionTypeAvro.VIEW;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorStarter {
    private final KafkaProperties kafkaProperties;
    private final AggregatorKafkaClient kafkaClient;
    private final Map<User, Map<Event, Double>> userInteractions = new HashMap<>(); // действия пользователя с событием
    private final Map<Event, Double> eventWeights = new HashMap<>(); // общая сумма весов события
    private final Map<Event, Map<Event, Double>> minEventWeights = new HashMap<>(); // минимальный вес для каждой пары

    private void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaClient.getConsumer()::wakeup));
        try {
            while (true) {
                List<EventSimilarityAvro> userActions = process(kafkaClient.getConsumer().poll(kafkaProperties.getConsumer().getTimeoutMs()));

                if (!userActions.isEmpty()) {
                    userActions.forEach(eventSimilarity -> {
                        log.info("Sending event similarity: {}", eventSimilarity);
                        kafkaClient.getProducer().send(
                                new ProducerRecord<>(kafkaProperties.getTopics().getEventsSimilarity(), null, eventSimilarity)
                        );
                    });
                }
            }
        } catch (WakeupException ignore) {

        } catch (Exception e) {
            log.error("Error during process topic:{}", kafkaProperties.getTopics().getUserActions(), e);
        } finally {
            try {
                kafkaClient.getProducer().flush();
                kafkaClient.getConsumer().commitSync();
                //kafkaClient.getConsumer().commitSync(currentOffsets);
            } finally {
                log.info("Closing aggregator consumer and producer...");
                kafkaClient.stop();
            }
        }
    }

    private List<EventSimilarityAvro> process(ConsumerRecords<Void, UserActionAvro> userActions) {
        List<EventSimilarityAvro> eventsSimilarities = new ArrayList<>();

        for (ConsumerRecord<Void, UserActionAvro> userAction : userActions) {
            User user = new  User(userAction.value().getUserId());
            Event event = new  Event(userAction.value().getEventId());
            double weight = getWeight(userAction.value().getActionType());

            if (eventWeights.containsKey(event)) {

            } else {

            }
        }

        /*
         * При расчёте сходства двух мероприятий важно учитывать вклад только тех пользователей, которые взаимодействовали с обоими мероприятиями.
         * Если пользователь взаимодействовал только с одним, например зарегистрировался на мероприятие A, но ничего не делал с B,
         * то его вклад в определение подобия этих мероприятий min(1.0, 0.0) будет равен 0. Это справедливо и для вариантов формулы косинусного сходства, рассмотренных выше.
         * Только те пользователи, которые взаимодействовали с обоими мероприятиями, дают реальную информацию о сходстве.
         * Это основа совместной фильтрации: подобие двух объектов определяется на основании их общих пользователей.
         * */

        /*
         * Допустим, пользователь сначала просматривает мероприятие, затем регистрируется на него и лайкает.
         * Всё это происходит в рамках одного взаимодействия, и суммировать каждое действие по отдельности нельзя.
         * Следует использовать максимальный вес, который точнее отражает истинный интерес пользователя.
         * */

        /*
         * Это действие с новым мероприятием (то есть до этого ни один пользователь с мероприятием ещё не взаимодействовал).
         * В этом случае просто рассчитывается сходство мероприятия с остальными.
         * */

        /*
         * Это очередное взаимодействие с мероприятием. Пересчитывать сходство с нуля не нужно.
         * Вместо этого следует обновить частные суммы S_min(A, B), S_a и S_b.
         * Затем вычислить новое значение коэффициента подобия для каждой пары мероприятий A и B, где A — мероприятие,
         * для которого пришло сообщение об очередном взаимодействии с пользователем, а B — каждое из остальных мероприятий
         * */

        //TODO
        /* 1) Расчет похожести между двумя мероприятиями (aggregator), хранить в hashtable inmemory
        *  сообщение отправляем (eventSimilarity) только если изменилась похожесть
        *   2) Предсказание оценки
        *  Analyzer: есть БД, 2 таблицы (похожести, взаимодействия, 15:34)
        *   читаем взаимодействия и пишем в БД. коэфф взаимодействия может только расти
        *   похожесть: первый id всегда меньше, обновляем в БД если изменился коэфф.
        *   GRPC эндпоинты: 1) рейтинг (getInteractions), это сумма оценок для мероприятия, этот рейтинг будет в event-service;
        *    2) похожие мероприятия (getSimilarEvents), отобрать из БД похожие мероприятия согласно критериям
        *    3) рекомендации (getRecommendationForUser). смотрим с чем пользователь взаимодействовал, для каждого мероприятия
        *    с которым взаимодействовал рассчитываем похожесть, объединяем, сортируем
        * */
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}