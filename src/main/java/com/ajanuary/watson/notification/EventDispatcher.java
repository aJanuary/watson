package com.ajanuary.watson.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EventDispatcher {

  private final Map<Class<? extends Event>, List<Consumer>> handlers = new HashMap<>();
  private final ExecutorService executors = Executors.newCachedThreadPool();

  public <T extends Event> void register(Class<T> eventType, Consumer<T> handler) {
    handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
  }

  public void dispatch(Event event) {
    for (var handler : handlers.getOrDefault(event.getClass(), List.of())) {
      executors.submit(() -> handler.accept(event));
    }
  }
}
