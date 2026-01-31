package event.controller.event;

import dto.event.EventFullDto;
import dto.event.EventShortDto;
import dto.event.PublicEventSearchRequest;
import event.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEvents(@ModelAttribute @Valid PublicEventSearchRequest requestParams,
                                                         HttpServletRequest request) {
        int size = requestParams.getSize() != null ? Math.max(1, requestParams.getSize()) : 10;
        int from = requestParams.getFrom() != null ? requestParams.getFrom() : 0;
        int page = from / size;

        PageRequest pageRequest = PageRequest.of(page, size);
        List<EventShortDto> events = eventService.getPublicEvents(requestParams, pageRequest);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEvent(@PathVariable Long eventId,
                                                 @RequestHeader(name = "X-EWM-USER-ID") Long userId) {
        EventFullDto event = eventService.getPublicEventById(eventId, userId);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/recommendations")
    public void getRecommendations() {
        //TODO
    }

    @PutMapping("/{id}/like")
    public void setLike() {
        //TODO
    }
}