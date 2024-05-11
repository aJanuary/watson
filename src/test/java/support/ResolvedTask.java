package support;

import java.time.Duration;
import java.util.function.Consumer;
import net.dv8tion.jda.api.utils.concurrent.Task;

public class ResolvedTask<T> implements Task<T> {
  private T resolvedValue;

  public ResolvedTask(T resolvedValue) {
    this.resolvedValue = resolvedValue;
  }

  @Override
  public boolean isStarted() {
    return true;
  }

  @Override
  public Task<T> onError(Consumer<? super Throwable> consumer) {
    return this;
  }

  @Override
  public Task<T> onSuccess(Consumer<? super T> consumer) {
    consumer.accept(resolvedValue);
    return this;
  }

  @Override
  public Task<T> setTimeout(Duration duration) {
    return this;
  }

  @Override
  public T get() {
    return resolvedValue;
  }

  @Override
  public void cancel() {}
}
