package client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.yandex.practicum.grpc.stats.user.UserActionProto;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendUserAction(UserActionProto userActionProto) {
        try {
            log.info("Sending user action proto: {}", userActionProto);
            client.collectUserAction(userActionProto);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Filed to send user action proto", e);
        }
    }
}