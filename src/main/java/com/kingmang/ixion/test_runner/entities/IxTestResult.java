package com.kingmang.ixion.test_runner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/*
Результат теста
 */
@Getter
@AllArgsConstructor
public class IxTestResult {
    /*
    Информация о результате
     */
    @Nullable
    private final RuntimeException error;
    private final boolean isSuccess;
}
