package event.dal.mapper;

import dto.compilation.CompilationResponse;
import dto.compilation.NewCompilationRequest;
import dto.event.EventShortDto;
import event.dal.entity.Compilation;
import event.dal.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toEntity(NewCompilationRequest request);

    @Mapping(target = "events", source = "events", qualifiedByName = "eventsToShortDtos")
    CompilationResponse toDto(Compilation compilation);

    @Named("eventsToShortDtos")
    default Set<EventShortDto> eventsToShortDtos(Set<Event> events) {
        if (events == null) {
            return null;
        }
        return events.stream()
                .map(EventMapper.INSTANCE::toShortDto)
                .collect(Collectors.toSet());
    }
}