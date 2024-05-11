package support;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DotenvFake implements Dotenv {

  private final Map<String, String> env = new HashMap<>();

  public DotenvFake add(String key, String value) {
    env.put(key, value);
    return this;
  }

  @Override
  public Set<DotenvEntry> entries() {
    return env.entrySet().stream()
        .map(entry -> new DotenvEntry(entry.getKey(), entry.getValue()))
        .collect(Collectors.toSet());
  }

  @Override
  public Set<DotenvEntry> entries(Filter filter) {
    return entries();
  }

  @Override
  public String get(String key) {
    return env.get(key);
  }

  @Override
  public String get(String key, String defaultValue) {
    return env.getOrDefault(key, defaultValue);
  }
}
