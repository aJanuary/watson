package support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.Result;
import net.dv8tion.jda.internal.requests.restaction.operator.CombineRestAction;
import net.dv8tion.jda.internal.requests.restaction.operator.FlatMapErrorRestAction;
import net.dv8tion.jda.internal.requests.restaction.operator.FlatMapRestAction;
import net.dv8tion.jda.internal.requests.restaction.operator.MapErrorRestAction;
import net.dv8tion.jda.internal.requests.restaction.operator.MapRestAction;

@SuppressWarnings({"unchecked", "raw"})
public class MonitorableRestAction<U extends RestAction> {
  private final U action;
  private final AtomicBoolean wasInvoked;

  private MonitorableRestAction(U action, AtomicBoolean wasInvoked) {
    this.action = action;
    this.wasInvoked = wasInvoked;
  }

  public <X> X action() {
    return (X) action;
  }

  public boolean wasInvoked() {
    return wasInvoked.get();
  }

  public static <U extends RestAction> MonitorableRestAction<U> create(Class<U> clazz) {
    return create(clazz, null);
  }

  public static <U extends RestAction> MonitorableRestAction<U> createWithException(
      Class<U> clazz, Throwable exception) {
    var wasInvoked = new AtomicBoolean(false);
    var action = mock(clazz);

    doAnswer(
            i -> {
              wasInvoked.set(true);
              return null;
            })
        .when(action)
        .queue();
    doAnswer(
            i -> {
              wasInvoked.set(true);
              return null;
            })
        .when(action)
        .queue(any());
    doAnswer(
            i -> {
              wasInvoked.set(true);
              Consumer<? super Throwable> failure = i.getArgument(1);
              failure.accept(exception);
              return null;
            })
        .when(action)
        .queue(any(), any());
    when(action.complete())
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              throw exception;
            });
    try {
      when(action.complete(anyBoolean()))
          .thenAnswer(
              i -> {
                wasInvoked.set(true);
                throw exception;
              });
    } catch (RateLimitedException e) {
      // Ignore. Doesn't happen while mocking.
    }
    when(action.submit())
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return CompletableFuture.failedFuture(exception);
            });
    when(action.submit(anyBoolean()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return CompletableFuture.failedFuture(exception);
            });
    when(action.mapToResult())
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new MapRestAction(action, Result::success);
            });
    when(action.map(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new MapRestAction(action, i.getArgument(0));
            });
    when(action.onSuccess(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              Consumer consumer = i.getArgument(0);
              return new MapRestAction(
                  action,
                  r -> {
                    consumer.accept(r);
                    return r;
                  });
            });
    when(action.onErrorMap(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new MapErrorRestAction(action, x -> true, i.getArgument(0));
            });
    when(action.onErrorMap(any(), any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new MapErrorRestAction(action, i.getArgument(0), i.getArgument(1));
            });
    when(action.onErrorFlatMap(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new FlatMapErrorRestAction(action, x -> true, i.getArgument(0));
            });
    when(action.onErrorFlatMap(any(), any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new FlatMapErrorRestAction(action, i.getArgument(0), i.getArgument(1));
            });
    when(action.flatMap(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new FlatMapRestAction(action, null, i.getArgument(0));
            });
    when(action.flatMap(any(), any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new FlatMapErrorRestAction(action, i.getArgument(0), i.getArgument(1));
            });
    when(action.and(any(), any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new CombineRestAction(action, i.getArgument(0), i.getArgument(1));
            });
    when(action.and(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new CombineRestAction(action, i.getArgument(0), (a, b) -> null);
            });

    return new MonitorableRestAction<>(action, wasInvoked);
  }

  public static <T, U extends RestAction> MonitorableRestAction<U> create(Class<U> clazz, T value) {
    var wasInvoked = new AtomicBoolean(false);
    var action = mock(clazz);

    doAnswer(
            i -> {
              wasInvoked.set(true);
              return null;
            })
        .when(action)
        .queue();
    doAnswer(
            i -> {
              wasInvoked.set(true);
              Consumer<? super T> success = i.getArgument(0);
              success.accept(value);
              return null;
            })
        .when(action)
        .queue(any());
    doAnswer(
            i -> {
              wasInvoked.set(true);
              Consumer<? super T> success = i.getArgument(0);
              success.accept(value);
              return null;
            })
        .when(action)
        .queue(any(), any());
    when(action.complete())
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return value;
            });
    try {
      when(action.complete(anyBoolean()))
          .thenAnswer(
              i -> {
                wasInvoked.set(true);
                return value;
              });
    } catch (RateLimitedException e) {
      // Ignore. Doesn't happen while mocking.
    }
    when(action.submit())
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return CompletableFuture.completedFuture(value);
            });
    when(action.submit(anyBoolean()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return CompletableFuture.completedFuture(value);
            });
    when(action.mapToResult())
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new MapRestAction(action, Result::success);
            });
    when(action.map(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new MapRestAction(action, i.getArgument(0));
            });
    when(action.onSuccess(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              Consumer consumer = i.getArgument(0);
              return new MapRestAction(
                  action,
                  r -> {
                    consumer.accept(r);
                    return r;
                  });
            });
    when(action.onErrorMap(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new MapErrorRestAction(action, x -> true, i.getArgument(0));
            });
    when(action.onErrorMap(any(), any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new MapErrorRestAction(action, i.getArgument(0), i.getArgument(1));
            });
    when(action.onErrorFlatMap(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new FlatMapErrorRestAction(action, x -> true, i.getArgument(0));
            });
    when(action.onErrorFlatMap(any(), any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new FlatMapErrorRestAction(action, i.getArgument(0), i.getArgument(1));
            });
    when(action.flatMap(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new FlatMapRestAction(action, null, i.getArgument(0));
            });
    when(action.flatMap(any(), any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new FlatMapErrorRestAction(action, i.getArgument(0), i.getArgument(1));
            });
    when(action.and(any(), any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new CombineRestAction(action, i.getArgument(0), i.getArgument(1));
            });
    when(action.and(any()))
        .thenAnswer(
            i -> {
              wasInvoked.set(true);
              return new CombineRestAction(action, i.getArgument(0), (a, b) -> null);
            });

    return new MonitorableRestAction<>(action, wasInvoked);
  }
}
