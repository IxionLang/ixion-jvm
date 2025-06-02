package com.kingmang.ixion.test_runner;

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

    /**
     * Запуск теста
     * @throws RuntimeException - ошибка при выполнении
     */
    public void run() throws RuntimeException {
    }

    @Override
    public String toString() {
        return "[" + name + ", file: " + filename + "]";
    }
}
