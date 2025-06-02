package com.kingmang.ixion.test_runner.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/*
Результат теста
 */
@Getter
@Builder
@AllArgsConstructor
@SuppressWarnings("ClassCanBeRecord")
public class IxTestResult {
    /*
    Информация о результате
     */
    @Nullable
    private final RuntimeException error;
    private final boolean isSuccess;
    private final long executionTime;
    private final boolean isInTimeRange;
}
