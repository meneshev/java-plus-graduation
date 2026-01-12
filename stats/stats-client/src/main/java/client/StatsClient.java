package client;

import exception.StatsServerUnavailable;
import model.EndpointHitDto;
import model.ViewStatsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
//TODO
// 1. Перенести проект сначала в main, проверить что он собирается и работает + перенести папку postman с тестами из предыдущего проекта
// 2. Настроить проект, чтобы в main сливали только с approve
// 3. Сделать DiscoveryClient и все требуемые настройки в ветке spring-cloud
// 3. Сделать PR на main

@Component
public class StatsClient {
    final RestClient restClient;
    final DiscoveryClient discoveryClient;

    public StatsClient(RestClient.Builder restClientBuilder,
                       @Value("${stats.server.url:http://localhost:9090}") String serverUrl) {
        this.restClient = restClientBuilder
                .baseUrl(serverUrl)
                .build();
    }

    private StatsClient getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception e) {
            throw new StatsServerUnavailable("Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    e);
        }
    }

    public void hit(EndpointHitDto endpointHitDto) {
        try {
            restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("Ошибка при отправке статистики: " + e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/stats")
                    .queryParam("start", start.format(formatter))
                    .queryParam("end", end.format(formatter))
                    .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                uris.forEach(uri -> uriBuilder.queryParam("uris", uri));
            }

            return restClient.get()
                    .uri(uriBuilder.build().toUriString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            System.err.println("Ошибка при получении статистики: " + e.getMessage());
            return List.of();
        }
    }
}