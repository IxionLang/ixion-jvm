package com.kingmang.ixion.test_runner;

/*
Бенчмарк
 */
public class TestBenchmark {
    // первое зафиксированное время в мс
    private long startTime;

    /**
     * Запускает таймер
     */
    public void begin() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Останавливает таймер и возвращает
     * время выполнения в мс.
     * @return время выполнения в секундах.
     */
    public long measure() {
        return System.currentTimeMillis()-startTime;
    }
}
