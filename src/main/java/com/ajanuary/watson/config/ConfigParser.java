package com.ajanuary.watson.config;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

public abstract class ConfigParser {

  protected final String path;

  private ConfigParser(String path) {
    this.path = path;
  }

  public static ObjectConfigParserWithValue parse(JsonNode rootNode) {
    return new UnknownTypeConfigParser(rootNode, null).object().required();
  }

  public String path() {
    return path;
  }

  public static class UnknownTypeConfigParser extends ConfigParser {

    private final JsonNode node;

    private UnknownTypeConfigParser(JsonNode node, String path) {
      super(path);
      this.node = node;
    }

    public StringConfigParser string() {
      if (node != null && !node.isNull() && !node.isTextual()) {
        throw new ConfigException(path + " must be a string");
      }

      return new StringConfigParser(node, path);
    }

    public ObjectConfigParser object() {
      if (node != null && !node.isNull() && !node.isObject()) {
        throw new ConfigException(path + " must be an object");
      }

      return new ObjectConfigParser(node, path);
    }

    public BooleanConfigParser bool() {
      if (node != null && !node.isNull() && !node.isBoolean()) {
        throw new ConfigException(path + " must be a boolean");
      }

      return new BooleanConfigParser(node, path);
    }

    public ListConfigParser list() {
      if (node != null && !node.isNull() && !node.isArray()) {
        throw new ConfigException(path + " must be a list");
      }

      return new ListConfigParser(node, path);
    }
  }

  public static class ObjectConfigParser extends ConfigParser {

    private final JsonNode node;

    private ObjectConfigParser(JsonNode node, String path) {
      super(path);
      this.node = node;
    }

    public <T> Optional<T> map(Function<ObjectConfigParserWithValue, T> mapper) {
      if (node == null || node.isNull()) {
        return Optional.empty();
      }
      try {
        return Optional.of(mapper.apply(new ObjectConfigParserWithValue(node, path)));
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        throw new ConfigException("Malformed value for " + path + ": " + e.getMessage(), e);
      }
    }

    public ObjectConfigParserWithValue required() {
      if (node == null || node.isNull()) {
        throw new ConfigException(path + " is required");
      }
      return new ObjectConfigParserWithValue(node, path);
    }
  }

  public static class ObjectConfigParserWithValue extends ConfigParser {

    private final JsonNode node;

    private ObjectConfigParserWithValue(JsonNode node, String path) {
      super(path);
      this.node = node;
    }

    public UnknownTypeConfigParser get(String key) {
      var keyPath = path == null ? key : path + "." + key;
      return new UnknownTypeConfigParser(node.get(key), keyPath);
    }

    public <TValue> Map<String, TValue> toMap(
        Function<UnknownTypeConfigParser, TValue> valueMapper) {
      var map = new HashMap<String, TValue>();
      for (Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
        var field = it.next();
        var itPath = path + "." + field.getKey();
        try {
          map.put(
              field.getKey(),
              valueMapper.apply(new UnknownTypeConfigParser(field.getValue(), itPath)));
        } catch (ConfigException e) {
          throw e;
        } catch (Exception e) {
          throw new ConfigException("Malformed value for " + itPath + ": " + e.getMessage(), e);
        }
      }
      return map;
    }
  }

  public static class ListConfigParser extends ConfigParser {

    private final JsonNode node;

    private ListConfigParser(JsonNode node, String path) {
      super(path);
      this.node = node;
    }

    public ListConfigParserWithValue required() {
      if (node == null || node.isNull()) {
        throw new ConfigException(path + " is required");
      }

      var list = new ArrayList<UnknownTypeConfigParser>();
      var index = 0;
      for (var element : node) {
        list.add(new UnknownTypeConfigParser(element, path + "[" + index + "]"));
        index += 1;
      }

      return new ListConfigParserWithValue(list, path);
    }
  }

  public static class ListConfigParserWithValue extends ConfigParser {

    List<UnknownTypeConfigParser> value;

    private ListConfigParserWithValue(List<UnknownTypeConfigParser> value, String path) {
      super(path);
      this.value = value;
    }

    public List<UnknownTypeConfigParser> value() {
      return value;
    }
  }

  public static class StringConfigParser extends ConfigParser {

    private final JsonNode node;

    private StringConfigParser(JsonNode node, String path) {
      super(path);
      this.node = node;
    }

    public StringConfigParserWithValue required() {
      if (node == null || node.isNull()) {
        throw new ConfigException(path + " is required");
      }
      return new StringConfigParserWithValue(node.textValue(), path);
    }

    public StringConfigParserWithValue defaultingTo(String defaultValue) {
      var value = node == null || node.isNull() ? defaultValue : node.textValue();
      return new StringConfigParserWithValue(value, path);
    }
  }

  public static class StringConfigParserWithValue extends ConfigParser {

    private final String value;

    private StringConfigParserWithValue(String value, String path) {
      super(path);
      this.value = value;
    }

    public String value() {
      return value;
    }

    public StringConfigParserWithValue validate(Function<String, Optional<String>> validator) {
      try {
        var errorM = validator.apply(value);
        if (errorM.isPresent()) {
          throw new ConfigException("Malformed value for " + path + ": " + errorM.get());
        }
        return this;
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        throw new ConfigException("Malformed value for " + path + ": " + e.getMessage(), e);
      }
    }

    public <T> T map(Function<String, T> mapper) {
      try {
        return mapper.apply(value);
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        throw new ConfigException("Malformed value for " + path + ": " + e.getMessage(), e);
      }
    }
  }

  public static class BooleanConfigParser extends ConfigParser {

    private final JsonNode node;

    private BooleanConfigParser(JsonNode node, String path) {
      super(path);
      this.node = node;
    }

    public BooleanConfigParserWithValue required() {
      if (node == null || node.isNull()) {
        throw new ConfigException(path + " is required");
      }
      return new BooleanConfigParserWithValue(node.booleanValue(), path);
    }

    public BooleanConfigParserWithValue defaultingTo(boolean defaultValue) {
      return new BooleanConfigParserWithValue(defaultValue, path);
    }
  }

  public static class BooleanConfigParserWithValue extends ConfigParser {

    private final boolean value;

    private BooleanConfigParserWithValue(boolean value, String path) {
      super(path);
      this.value = value;
    }

    public boolean value() {
      return value;
    }

    public BooleanConfigParserWithValue validate(Function<Boolean, Optional<String>> validator) {
      try {
        var errorM = validator.apply(value);
        if (errorM.isPresent()) {
          throw new ConfigException(path + " " + errorM.get());
        }
        return this;
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        throw new ConfigException("Malformed value for " + path + ": " + e.getMessage(), e);
      }
    }

    public <T> T map(Function<Boolean, T> mapper) {
      try {
        return mapper.apply(value);
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        throw new ConfigException("Malformed value for " + path + ": " + e.getMessage(), e);
      }
    }
  }
}
