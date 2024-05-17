package chire.func;

import org.python.core.PyFunction;

public interface FunCall<T> {
    PyFunction get(T t);
}
