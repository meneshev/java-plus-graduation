package request.dal.mapper;

import dto.request.ParticipationRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import request.dal.entity.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    @Mapping(source = "event", target = "event")
    @Mapping(source = "requester", target = "requester")
    @Mapping(source = "status", target = "status")
    ParticipationRequestDto toDto(ParticipationRequest request);
}
