package com.kingmang.ixion.runtime;

import com.kingmang.ixion.exception.Panic;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtil {

    public record IxListWrapper(ArrayList<?> list, String name) implements Iterable<Object>, Iterator<Object> {

        private static int currentIndex = 0;

        public IxListWrapper(String name) {
            this(new ArrayList<>(), name);
        }

        @Nonnull
        @Override
        public Iterator<Object> iterator() {
            currentIndex = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < list.size();
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                new Panic("no such element").send();
            }
            return list.get(currentIndex++);
        }

        @Override
        @Nonnull
        public String toString() {
            return list.toString();
        }


    }


    public static <A, B, O> Stream<O> zipMap(List<? extends A> a, List<? extends B> b, BiFunction<A, B, ? extends O> lambda) throws ArrayIndexOutOfBoundsException {
        var l = new ArrayList<O>();
        if (a.size() == b.size()) {
            var i1 = a.iterator();
            var i2 = b.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                var o = lambda.apply(i1.next(), i2.next());
                l.add(o);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Can't zip two Lists with differing number of elements.");
        }
        return l.stream();
    }

    public static <A, B, O> void zip(List<? extends A> a, List<? extends B> b, BiConsumer<A, ? super B> lambda) throws ArrayIndexOutOfBoundsException {
        if (a.size() == b.size()) {
            var i1 = a.iterator();
            var i2 = b.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                lambda.accept(i1.next(), i2.next());
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Can't zip two Lists with differing number of elements.");
        }
    }

    @SafeVarargs
    public static <T> Set<T> set(T... varargs) {
        return Set.of(varargs);
    }

    @SafeVarargs
    public static <T> Map<T, Long> countedSet(T... varargs) {
        return Arrays.stream(varargs).collect(Collectors.groupingBy(s -> s,
                Collectors.counting()));
    }


    public static <T> String joinConjunction(Collection<T> collection) {
        var strList = collection.stream().map(Objects::toString).toList();
        if (strList.isEmpty()) return "";
        if (strList.size() == 1) return strList.getFirst();
        if (strList.size() == 2) return String.join(" and ", strList);
        return String.join(", ", strList.subList(0, strList.size() - 1)) + " and " + strList.getLast();
    }

    @SafeVarargs
    public static <T> List<T> list(T... i) {
        return Arrays.asList(i);
    }

}
