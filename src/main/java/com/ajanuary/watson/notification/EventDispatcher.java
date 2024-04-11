package com.ajanuary.watson.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDispatcher {

  private Map<EventType, List<Runnable>> handlers = new HashMap<>();

  public void register(EventType eventType, Runnable handler) {
    handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
  }

  public void dispatch(EventType eventType) {
    handlers.getOrDefault(eventType, List.of()).forEach(Runnable::run);
  }
}
