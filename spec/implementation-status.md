# Соответствие реализации / Implementation conformance

Срез: 19 октября 2026 года. Этот документ **информативный**: он описывает границы
текущего компилятора и не изменяет правила [русской](specification.ru.md) или
[английской](specification.en.md) спецификации Ixion Core 1.0.

Snapshot: 19 October 2026. This document is **informative**: it describes the current
compiler's boundaries and does not modify the Russian or English Core 1.0 contract.
No complete Core 1.0 conformance claim is made.

| Область / Area | Текущее состояние / Current status |
| --- | --- |
| JVM и Java / JVM and Java | Оба пути используются проверками ядра. / Both paths are exercised by core tests. |
| Структуры, кортежи, with / Structures, tuples, with | Неизменяемые поля и функциональное обновление реализованы. / Immutable fields and functional updates are implemented. |
| Generic/рекурсивные ADT / Generic/recursive ADTs | Конструкторы с данными, Option/Result и reified type tags реализованы. / Payload constructors, Option/Result, and reified type tags are implemented. |
| Case и шаблоны / Case and patterns | Вложенные шаблоны, guards, проверка полноты/недостижимости реализованы с конечным бюджетом анализа. / Nested patterns, guards, and bounded coverage/reachability checking are implemented. |
| Списки / Lists | Закрытое копирующее хранилище, immutable iterator, append/concat/get/updated/map/filter/fold, безопасная индексация и len реализованы. / Defensive storage, immutable iteration, functional APIs, safe indexing, and len are implemented. |
| Функциональные значения / Function values | Лямбды и snapshot-захваты реализованы; все формы ссылок на именованные функции/конструкторы как значения по §§10–12 ещё требуют полного аудита. / Lambdas and snapshot capture are implemented; all named function/constructor value-reference forms still require a full audit. |
| Равенство и hash / Equality and hash | Структурное равенство и обязательный hash ещё не реализованы. / Structural equality and required hash are not implemented. |
| Инициализация / Initialization | Полный контракт модульных const и циклов инициализации ещё не реализован. / The complete module-const and initialization-cycle contract is not implemented. |
| Generic-unions | Потенциально совпадающие альтернативы могут отвергаться как pattern.overlapping_generic_union; это ограничение реализации. / Potentially overlapping alternatives can be rejected as pattern.overlapping_generic_union; this is an implementation restriction. |
| Лексика и грамматика / Lexing and grammar | Строгая Unicode-модель, все числовые/escape-границы и весь нормативный синтаксис ещё не сертифицированы; часть legacy-форм принимается. В частности, у литерала списка ещё не поддержана завершающая запятая. / Strict Unicode rules, all numeric/escape boundaries, and the full grammar are not certified; some legacy forms remain accepted. In particular, list literals lack trailing-comma support. |
| Арифметика и range / Arithmetic and range | Полный контракт степени, диапазона, std::range и пограничных числовых случаев требует доработки/проверки обоих путей. / The complete exponentiation, range, std::range, and numeric-edge contract needs work/validation in both paths. |
| Native-граница / Foreign boundary | Запрет mutable JVM List для языкового списка реализован; общие nullable/resource adapters и транзитивный контроль ресурсов ещё не завершены. / Mutable JVM List compatibility is rejected for language lists; general nullable/resource adapters and transitive resource checking are unfinished. |
| Диагностика / Diagnostics | Используются структурированные ошибки; конкретные существующие коды и позиции ещё не полностью приведены к §22. / Structured errors exist; current codes and positions are not fully aligned with §22. |
| Java case-block | Возврат последним выражением локального const из блока ветви может дать ошибку Java-генерации; в учебных примерах итог вычисляется напрямую. / Returning a branch-local const as the final expression may fail Java generation; tutorial examples compute the final result directly. |

## Что именно проверено / What has been checked

В текущем наборе есть исполняемые примеры ADT, списков, generic-функций,
порядка эффектов и отрицательных диагностик. Программы из
[руководства для начинающих](../docs/ru.md) автоматически проверяются в ASM и Java.
Успешный набор тестов не доказывает соответствие каждому нормативному пункту.

The current suite includes executable ADT, list, generic-function, effect-order,
and negative-diagnostic examples. Programs in the beginner guide are automatically
checked through ASM and Java. A passing suite is not proof of every normative rule.

Нормативные правила нельзя ослаблять только ради существующего представления
JVM. Расхождения исправляются в реализации либо меняются явной новой редакцией
обеих спецификаций, а не скрытой поправкой в учебнике.

Normative rules must not be weakened solely to fit the current JVM representation.
Discrepancies are resolved in the implementation or by an explicit new edition of
both specifications, not by an implicit tutorial exception.
