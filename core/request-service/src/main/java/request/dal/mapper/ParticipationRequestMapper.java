package request.dal.mapper;

import dto.request.ParticipationRequestDto;
import org.mapstruct.*;
import request.dal.entity.ParticipationRequest;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ParticipationRequestMapper {

    @Mapping(source = "event", target = "event")
    @Mapping(source = "requester", target = "requester")
    @Mapping(source = "status", target = "status")
    ParticipationRequestDto toDto(ParticipationRequest request);
}
