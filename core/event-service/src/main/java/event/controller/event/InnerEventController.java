package event.controller.event;

import dto.event.EventFullDto;
import event.service.EventService;
import event.service.EventStatsService;
import feign.event.EventOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class InnerEventController implements EventOperations {
    private final EventService eventService;
    private final EventStatsService eventStatsService;

    @Override
    public EventFullDto getById(Long id) {
        return eventService.getEventById(id);
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsBatchByEventIds(List<Long> eventIds) {
        return eventStatsService.getConfirmedRequestsBatch(eventIds);
    }
}
