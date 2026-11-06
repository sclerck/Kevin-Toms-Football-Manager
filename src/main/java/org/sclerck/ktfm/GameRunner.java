package org.sclerck.ktfm;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Starts the interactive console game once the Spring context is ready. */
@Component
@ConditionalOnProperty(name = "ktfm.console.enabled", havingValue = "true", matchIfMissing = true)
public class GameRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        FootballManager.play();
    }
}
