package feign.request;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", path = "/api/requests")
public interface RequestClient extends RequestOperations {
}
