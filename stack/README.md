# Автотесты [![Ci/CD](../../actions/workflows/classroom.yml/badge.svg?branch=main&event=workflow_dispatch)](../../actions/workflows/classroom.yml)

# Что лежит в репозитории

* `template_dont_edit` – директория с шаблонами для Logisim и SystemVerilog.
* `*_tb.sv` – тестирующие модули, использующиеся в автотестах (не удалять).

# Что нужно загружать в репозиторий

1. Проект Logisim (lite или normal). Если будут загружены 2 версии, то проверяется normal.
2. Скрипт `stack_structural.sv`, описывающий модуль `stack_structural` и содержащий все остальные необходимые модули.
3. Скрипт `stack_behaviour.sv`, описывающий модуль `stack_behaviour` и содержащий все остальные необходимые модули.

# Работа с шаблонами

1. Копируем шаблоны в корень репозитория.
2. Работаем в скопированных шаблонах.

Проверяются только файлы из корня репозитория. Не нужно править шаблоны в директории `template_dont_edit`.

> [!TIP]
> Уровень сложности определяется следующим образом:
>
> (Найден файл `stack_structural.sv` и в этом файле найден `"stack_structural_normal"`) ? `normal` : `lite`
>
> (Найден файл `stack_behaviour.sv` и в этом файле найден `"stack_behaviour_normal"`) ? `normal` : `lite`

# Проверка verilog локально (из корня репозитория)

1. Сборка: `iverilog -g2012 -o stack_tb.out stack_behaviour_tb.sv`
2. Симуляция: `vvp stack_tb.out +TIMES=5 +OUTCSV=st_stack_5.csv`
3. Проверка результатов по логам в файле `st_stack_5.csv`. Проверяем значения на выходе только при `CLK=1`, если выполнена lite версия, иначе – при `CLK=0` и `CLK=1`.

Закрытые тесты для Verilog представляют собой запуск testbench из репозитория с различными константами `TIMES`.

> [!CAUTION]
> `edge signal` использовать нельзя. `posedge` и `negedge` - можно

> [!WARNING]
> Если файл verilog-скрипта не найден, то в логах будет ошибка:
> ```log
> stack_structural__tb.sv: No such file or directory
> No top level modules, and no -s option.
> ```
