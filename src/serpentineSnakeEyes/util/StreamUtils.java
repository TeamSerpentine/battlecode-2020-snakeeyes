package serpentineSnakeEyes.util;

import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StreamUtils {

	@FunctionalInterface
	public interface PredicateWithExceptions<T, E extends Exception> {
		boolean accept(T t) throws E;
	}

	@FunctionalInterface
	public interface SupplierWithExceptions<T, E extends Exception> {
		T get() throws E;
	}

	@FunctionalInterface
	public interface ConsumerWithExceptions<T, E extends Exception> {
		void accept(T t) throws E;
	}

	@FunctionalInterface
	public interface FunctionWithExceptions<T, R, E extends Exception> {
		R apply(T t) throws E;
	}

	@FunctionalInterface
	public interface BinaryOperatorWithExceptions<T, E extends Exception> {
		T apply(T t1, T t2) throws E;
	}

	@FunctionalInterface
	public interface ComparatorWithExceptions<T, E extends Exception> {
		int compare(T o1, T o2) throws E;
	}

	public static <T, E extends Exception> Predicate<T> rethrowPredicate(PredicateWithExceptions<T, E> predicate) throws E {
		return t -> {
			boolean result = false;
			try {
				result = predicate.accept(t);
			} catch (Exception exception) {
				throwActualException(exception);
			}
			return result;
		};
	}

	public static <T, E extends Exception> Supplier<T> rethrowSupplier(SupplierWithExceptions<T, E> supplier) throws E {
		return () -> {
			try {
				return supplier.get();
			} catch (Exception exception) {
				throwActualException(exception);
			}
			return null;
		};
	}

	public static <T, E extends Exception> Consumer<T> rethrowConsumer(ConsumerWithExceptions<T, E> consumer) throws E {
		return t -> {
			try {
				consumer.accept(t);
			} catch (Exception exception) {
				throwActualException(exception);
			}
		};
	}

	public static <T, E extends Exception> BinaryOperator<T> rethrowBinaryOperatorFunction(BinaryOperatorWithExceptions<T, E> function) throws E {
		return (t, u) -> {
			try {
				return function.apply(t, u);
			} catch (Exception exception) {
				throwActualException(exception);
				return null;
			}
		};
	}

	public static <T, R, E extends Exception> Function<T, R> rethrowFunction(FunctionWithExceptions<T, R, E> function) throws E {
		return t -> {
			try {
				return function.apply(t);
			} catch (Exception exception) {
				throwActualException(exception);
				return null;
			}
		};
	}

	public static <T, E extends Exception> Comparator<T> rethrowComparator(ComparatorWithExceptions<T, E> comparator) throws E {
		return (o1, o2) -> {
			try {
				return comparator.compare(o1, o2);
			} catch (Exception exception) {
				throwActualException(exception);
				return 0;
			}
		};
	}

	public static <T, R> Function<T, R> recoverFunction(Function<T, R> function) {
		return t -> {
			try {
				return function.apply(t);
			} catch (Exception exception) {
				return null;
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static <E extends Exception> void throwActualException(Exception exception) throws E {
		throw (E) exception;
	}
}
