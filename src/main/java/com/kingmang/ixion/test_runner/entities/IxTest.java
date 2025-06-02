package com.kingmang.ixion.test_runner.entities;

import com.kingmang.ixion.runner.Runner;
import com.kingmang.ixion.test_runner.TestBenchmark;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 Тест .ix
 */
@AllArgsConstructor
@Getter
@Builder
public class IxTest {
    /*
    Данные о тесте
     */
    private final String filename;
    private final String name;
    private final IxTestTimeBound timeBound;

    /**
     * Запуск теста
     * @throws RuntimeException - ошибка при выполнении
     * @return - время выполнения
     */
    public long run() throws RuntimeException {
        TestBenchmark benchmark = new TestBenchmark();
        benchmark.begin();
        Runner.run(filename);
        return benchmark.measure();
    }

    @Override
    public String toString() {
        return name + " :: " + filename;
    }
}
