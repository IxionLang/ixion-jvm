package com.kingmang.ixion.test_runner.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
Боунд по времени для теста
 */
@SuppressWarnings("ClassCanBeRecord")
@Getter
@AllArgsConstructor
public class IxTestTimeBound {
    /*
    Данные о времени
     */
    private final long max;
    private final long tolerance;

    /*
    Проверка
     */
    public boolean isInBounds(long executionTime) {
        return (executionTime - tolerance) <= max;
    }
}