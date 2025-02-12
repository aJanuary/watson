package com.ajanuary.watson.programme;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ProgrammeItem(
    String id,
    String title,
    @JsonDeserialize(using = TagDeserializer.class) List<String> tags,
    LocalDate date,
    LocalTime time,
    ZonedDateTime dateTime,
    int mins,
    @JsonDeserialize(converter = LocationConverter.class) String loc,
    @JsonDeserialize(using = PersonDeserializer.class) List<String> people,
    String desc,
    @JsonDeserialize(using = LinksDeserializer.class) Map<String, String> links) {

  public ZonedDateTime startTime(ZoneId zoneId) {
    if (dateTime != null) {
      return dateTime.withZoneSameInstant(zoneId);
    } else {
      return ZonedDateTime.of(date, time, zoneId);
    }
  }
  public ZonedDateTime endTime(ZoneId zoneId) {
    return startTime(zoneId).plus(Duration.ofMinutes(mins));
  }

  private static final class TagDeserializer extends StdDeserializer<List<String>> {

    public TagDeserializer() {
      super(List.class);
    }

    @Override
    public List<String> deserialize(
        com.fasterxml.jackson.core.JsonParser p,
        com.fasterxml.jackson.databind.DeserializationContext ctxt)
        throws java.io.IOException {
      var result = new ArrayList<String>();
      var list = p.readValueAsTree();
      for (var i = 0; i < list.size(); i++) {
        var value = ((JsonNode) list.get(i));
        if (value.isTextual()) {
          result.add(value.asText());
        } else if (value.has("label")) {
          var tag = value.get("label").asText();
          result.add(tag);
        }
      }
      return result;
    }
  }

  private static final class LinksDeserializer extends StdDeserializer<Map<String, String>> {
    public LinksDeserializer() {
      super(Map.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        var value = ctxt.readTree(p);
        if (value.isArray()) {
          // For some reason the JSON is an empty list instead of an empty object
          return Map.of();
        } else {
          return ctxt.readTreeAsValue(value, Map.class);
        }
    }
  }

  private static final class DateConverter extends StdConverter<String, ZonedDateTime> {

    @Override
    public ZonedDateTime convert(String value) {
      return ZonedDateTime.parse(value);
    }
  }

  private static final class LocationConverter extends StdConverter<List<String>, String> {

    @Override
    public String convert(List<String> value) {
      return value.get(0);
    }
  }

  private static final class PersonDeserializer extends StdDeserializer<List<String>> {

    public PersonDeserializer() {
      super(List.class);
    }

    @Override
    public List<String> deserialize(
        com.fasterxml.jackson.core.JsonParser p,
        com.fasterxml.jackson.databind.DeserializationContext ctxt)
        throws java.io.IOException {
      var result = new ArrayList<String>();
      var list = p.readValueAsTree();
      for (var i = 0; i < list.size(); i++) {
        var name = ((JsonNode) list.get(i)).get("name").asText();
        result.add(name);
      }
      return result.stream()
          .sorted(
              (a, b) -> {
                if (a.endsWith("(moderator)")) {
                  return -1;
                }
                if (b.endsWith("(moderator)")) {
                  return 1;
                }
                return a.compareTo(b);
              })
          .toList();
    }
  }
}
