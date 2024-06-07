package com.ajanuary.watson.programme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.StdConverter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public record ProgrammeItem(
    String id,
    String title,
    List<String> tags,
    @JsonDeserialize(converter = DateConverter.class) LocalDate date,
    String time,
    int mins,
    @JsonDeserialize(converter = LocationConverter.class) String loc,
    @JsonDeserialize(using = PersonDeserializer.class) List<String> people,
    String desc,
    List<String> links) {

  public LocalDateTime startTime() {
    return LocalDateTime.of(date, LocalTime.parse(time));
  }

  public LocalDateTime endTime() {
    return startTime().plus(Duration.ofMinutes(mins));
  }

  private static final class DateConverter extends StdConverter<String, LocalDate> {

    @Override
    public LocalDate convert(String value) {
      return LocalDate.parse(value);
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
