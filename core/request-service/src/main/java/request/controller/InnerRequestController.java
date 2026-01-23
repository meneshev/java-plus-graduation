package request.controller;

import dto.request.EventConfirmedRequestsDto;
import dto.request.EventRequestStatusUpdateRequest;
import dto.request.EventRequestStatusUpdateResult;
import dto.request.ParticipationRequestDto;
import feign.request.RequestOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import request.dal.repository.ParticipationRequestRepository;
import request.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class InnerRequestController implements RequestOperations {
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestService service;

    @GetMapping
    public List<EventConfirmedRequestsDto> getRequestsByEventIds(@RequestParam("ids") List<Long> eventIds) {
        return requestRepository.findConfirmedRequestsCountByEventIds(eventIds);
    }

    @Override
    @GetMapping("/participation")
    public List<ParticipationRequestDto> getParticipationRequestsInner(Long userId, Long eventId) {
        return service.getRequestsByEvent(userId, eventId);
    }

    @Override
    @PatchMapping
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        return service.changeRequestStatus(userId, eventId, request);
    }


}
