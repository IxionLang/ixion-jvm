package com.kingmang.ixion.modules;

import com.kingmang.ixion.exception.Panic;
import com.kingmang.ixion.runtime.CollectionUtil;

import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;


@SuppressWarnings("unused")
public class Prelude {

    public static void panic(Object msg) {
        new Panic((String) msg).send();
    }

	public static void println(Object arg) {
		System.out.println(arg.toString());
	}

	public static void print(Object arg) {
		System.out.print(arg.toString());
	}

    public static int parse_int(String value) {
        return Integer.parseInt(value);
    }

	public static <T> void list_append(List<T> r, T a) {
		r.add(a);
	}

	public static <T> T list_pop(List<T> r) {
		return r.removeLast();
	}

	public static <T> Iterator<Integer> range(int start, int stop) {
		return IntStream.range(start, stop).boxed().iterator();
	}


	public static <T> T list_get(CollectionUtil.IxListWrapper r, int pos) {
		return (T) r.list().get(pos);
	}

	public static int len(Object r) {
		if(r instanceof CollectionUtil.IxListWrapper list)
            return list.list().size() + 1;
	    else if (r instanceof String s)
            return s.length();

        return -1;
    }
}
