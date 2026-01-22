package feign.event;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "event-service", path = "/api/events")
public interface EventClient extends EventOperations {
}
