package com.kingmang.ixion.test_runner;

import com.kingmang.ixion.test_runner.entities.IxTest;
import com.kingmang.ixion.test_runner.entities.IxTestResult;

/**
 * Тест раннер
 *
 */
public class TestRunner {
    /**
     * Запуск одного теста
     * @param test тест
     * @return результат выполнения
     */
    public IxTestResult runSingle(IxTest test) {
        var builder = IxTestResult.builder();
        try {
            long executionTime = test.run();
            builder.isSuccess(true);
            builder.executionTime(executionTime);
            if (test.getTimeBound() != null) {
                builder.isInTimeRange(test.getTimeBound().isInBounds(executionTime));
            } else builder.isInTimeRange(false);
        } catch (RuntimeException error) {
            builder.isSuccess(false);
            builder.error(error);
        }
        return builder.build();
    }

    /**
     * Запуск тестов
     */
    @SuppressWarnings({"DataFlowIssue"})
    public void run(IxTest[] tests) {
        // проходимся по тестам
        for (IxTest test : tests) {
            System.out.println("\u001B[92m[Running]\u001B[0m " + test);
            IxTestResult result = runSingle(test);
            if (result.isSuccess()) {
                System.out.println("| " + "Passed.");
                System.out.println("| Execution time: " + result.getExecutionTime() + "ms");
                if (test.getTimeBound() != null) {
                    System.out.println("| Time range: " + test.getTimeBound().getMax() + "ms"
                            + ", ~" + test.getTimeBound().getTolerance() + "ms");
                    System.out.println("| > In range: " + result.isInTimeRange());
                }
            } else {
                System.out.println("| " + "Failed.");
                System.out.println("|");
                System.out.print("| Exception: ");
                RuntimeException e = result.getError();
                System.out.print(e.getMessage());
                for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                    System.out.println("| >" + stackTraceElement);
                }
            }
        }
    }
}
