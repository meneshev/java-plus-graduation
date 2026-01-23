package feign.event;

import dto.event.EventFullDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EventOperations {
    @GetMapping("/{id}")
    EventFullDto getById(@PathVariable("id") Long id);

    @GetMapping
    Set<EventFullDto> getByIds(@RequestParam("ids") Set<Long> ids);

    @GetMapping
    Map<Long, Long> getConfirmedRequestsBatchByEventIds(@RequestParam("eventIds") List<Long> eventIds);
}
