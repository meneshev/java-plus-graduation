package analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    final AnalyzerSimilaritiesProcessor similaritiesProcessor;
    final AnalyzerUserActionsProcessor userActionsProcessor;

    @Override
    public void run(String... args) throws Exception {
        Thread similaritiesThread = new Thread(similaritiesProcessor);
        similaritiesThread.setName("SimilaritiesHandlerThread");
        similaritiesThread.start();

        Thread userActionsThread = new Thread(userActionsProcessor);
        userActionsThread.setName("UserActionsHandlerThread");
        userActionsThread.start();
    }
}
