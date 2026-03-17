package com.campos.webscraper.application.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("PlaywrightConcurrencyService")
class PlaywrightConcurrencyServiceTest {

    @Test
    @DisplayName("limits the number of concurrent executions")
    void limitsConcurrentExecutions() throws InterruptedException {
        PlaywrightConcurrencyService service = new PlaywrightConcurrencyService(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);

        executor.submit(() -> service.execute(() -> {
            firstStarted.countDown();
            try {
                releaseFirst.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return "first";
        }));

        firstStarted.await();

        executor.submit(() -> service.execute(() -> {
            secondEntered.countDown();
            return "second";
        }));

        assertThat(secondEntered.await(200, TimeUnit.MILLISECONDS)).isFalse();

        releaseFirst.countDown();

        assertThat(secondEntered.await(1, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();
    }

    @Test
    @DisplayName("throws when interrupted while waiting")
    void throwsWhenInterrupted() throws InterruptedException {
        PlaywrightConcurrencyService service = new PlaywrightConcurrencyService(1);
        CountDownLatch started = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            service.execute(() -> {
                started.countDown();
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        });
        thread.start();
        started.await();
        Thread testThread = new Thread(() -> assertThatThrownBy(() -> service.execute(() -> null))
                .isInstanceOf(IllegalStateException.class));
        testThread.start();
        testThread.interrupt();
        testThread.join(2000);
        thread.interrupt();
    }
}
