package com.kingmang.ixion.runtime;

import java.util.Stack;

public class Defer implements Runnable {
    private final Stack<Runnable> stack = new Stack<Runnable>();

    @Override
    public void run() {
        while (!stack.empty()) {
            Runnable r = stack.pop();
            r.run();
        }
    }

    public void defer(Runnable r) {
        stack.push(r);
    }
}