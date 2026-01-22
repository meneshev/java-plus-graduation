package request.controller;

import dto.request.EventConfirmedRequestsDto;
import feign.request.RequestOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import request.dal.repository.ParticipationRequestRepository;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class InnerRequestController implements RequestOperations {
    private final ParticipationRequestRepository requestRepository;

    @GetMapping
    public List<EventConfirmedRequestsDto> getRequestsByEventIds(@RequestParam("ids") List<Long> eventIds) {
        return requestRepository.findConfirmedRequestsCountByEventIds(eventIds);
    }
}
