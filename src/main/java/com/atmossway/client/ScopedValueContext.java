package com.atmossway.client;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks scoped replacements while preserving an existing or nested value.
 */
final class ScopedValueContext<T> {
    private final ThreadLocal<Deque<Frame<T>>> frames =
            ThreadLocal.withInitial(ArrayDeque::new);

    Transition<T> begin(T current, T replacement) {
        boolean changed = replacement != null;
        frames.get().push(new Frame<>(current, changed));
        return new Transition<>(changed, changed ? replacement : current);
    }

    Transition<T> end(T current) {
        Deque<Frame<T>> stack = frames.get();
        if (stack.isEmpty()) {
            frames.remove();
            return new Transition<>(false, current);
        }
        Frame<T> frame = stack.pop();
        if (stack.isEmpty()) {
            frames.remove();
        }
        return new Transition<>(frame.changed(), frame.changed() ? frame.previous() : current);
    }

    void reset() {
        frames.remove();
    }

    record Transition<T>(boolean changed, T value) {
    }

    private record Frame<T>(T previous, boolean changed) {
    }
}
