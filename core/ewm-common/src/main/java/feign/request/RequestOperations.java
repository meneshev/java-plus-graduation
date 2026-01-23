package feign.request;

import dto.request.EventConfirmedRequestsDto;
import dto.request.EventRequestStatusUpdateRequest;
import dto.request.EventRequestStatusUpdateResult;
import dto.request.ParticipationRequestDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface RequestOperations {
    @GetMapping
    public List<EventConfirmedRequestsDto> getRequestsByEventIds(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/participation")
    List <ParticipationRequestDto> getParticipationRequestsInner(@RequestParam("userId") Long userId,
                                                                 @RequestParam("eventId") Long eventId);

    @PatchMapping
    EventRequestStatusUpdateResult changeRequestStatus(@RequestParam("userId") Long userId,
                                                       @RequestParam("eventId") Long eventId,
                                                       @RequestBody EventRequestStatusUpdateRequest request);
}
