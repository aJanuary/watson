package com.ajanuary.watson.alarms;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler<T> {
  private final Logger logger;

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition waiting = lock.newCondition();

  @FunctionalInterface
  public interface NextEventTimeGetter {
    Optional<LocalDateTime> get() throws Exception;
  }

  @FunctionalInterface
  public interface EventsGetter<T> {
    List<T> getEventsBefore(LocalDateTime time) throws Exception;
  }

  public Scheduler(String name, JDA jda, ZoneId zoneId, TemporalAmount minTimeBetweenEvents, NextEventTimeGetter getNextEventTime, EventsGetter<T> eventsGetter, Consumer<T> onEvent) {
    logger = LoggerFactory.getLogger(name + " scheduler");
    Thread thread = new Thread(() -> {
      try {
        Instant timeCanRaiseNextEvent = Instant.now();
        while (true) {
          try {
            lock.lock();
            boolean hadError;
            Optional<LocalDateTime> nextEventTime = Optional.empty();
            try {
              nextEventTime = getNextEventTime.get();
              hadError = false;
            } catch (Exception e) {
              logger.error("Error getting next event", e);
              hadError = true;
            }
            while (nextEventTime.isEmpty() || !nextEventTime.get().isBefore(LocalDateTime.now(zoneId))) {
              if (nextEventTime.isEmpty()) {
                if (hadError) {
                  logger.info("Waiting 1 minute");
                  // If we had an SQL error, hope that it was temporary and wait a minute.
                  waiting.await(1, TimeUnit.MINUTES);
                } else {
                  logger.info("Waiting for a db notification");
                  waiting.await();
                }
              } else {
                long millisToSleep = Math.max(0, ChronoUnit.MILLIS.between(LocalDateTime.now(zoneId), nextEventTime.get()));
                logger.info("Waiting for {} ms until {}", millisToSleep, nextEventTime.get());
                waiting.await(millisToSleep, TimeUnit.MILLISECONDS);
              }
              try {
                nextEventTime = getNextEventTime.get();
                logger.info("Got next event time {}", nextEventTime);
                hadError = false;
              } catch (Exception e) {
                logger.error("Error getting next event", e);
                hadError = true;
              }
            }
            jda.awaitReady();
            List<T> events = eventsGetter.getEventsBefore(LocalDateTime.now(zoneId));
            for (T event : events) {
              Thread.sleep(Math.max(0, ChronoUnit.MILLIS.between(Instant.now(), timeCanRaiseNextEvent)));
              logger.info("Triggering event");
              onEvent.accept(event);
              timeCanRaiseNextEvent = Instant.now().plus(minTimeBetweenEvents);
            }
          } catch (InterruptedException e) {
            // Rethrow so it can terminate the while loop.
            throw e;
          } catch (Exception e) {
            logger.error("Error in scheduler", e);
          } finally {
            lock.unlock();
          }
        }
      } catch (InterruptedException e) {
        // Allow the thread to die
      }
    });
    thread.start();
    // TODO: Add mechanism to stop thread
  }

  public void notifyOfDbChange() {
    try {
      logger.info("Notified of db change");
      lock.lock();
      waiting.signal();
    } finally {
      lock.unlock();
    }
  }
}
