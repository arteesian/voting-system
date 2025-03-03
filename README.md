# voting-system
A client-server application that allows you to create voting in various sections.

## Задание
Сделать клиент-серверное приложение, которое позволяет создавать голосование в
различных разделах.
Приложение должно поддерживать:
● Запуск в режиме клиента и режиме сервера
● TCP / UDP взаимодейтсвие между сервером и клиентом с использованием
библиотеки Netty
● В режиме сервера уметь принимать команды от нескольких клиентов
одновременно
● Логирование в режиме сервера
● Unit тесты
● Клиентские команды
◦ login -u=username – подключиться к серверу с указанным именем
пользователя (все остальные команды доступны только после выполнения
login)
◦ create topic -n=<topic> - создает новый раздел c уникальным именем
заданным в параметре -n
◦ view - показывает список уже созданных разделов в формате: <topic (votes
in topic=<count>)>
▪ опциональный параметр -t=<topic> - в этом случае команда показывает
список голосований в конкретном разделе
◦ create vote -t=<topic> - запускает создание нового голосования в разделе
указанном в параметре -t
Для создания голосования (команда create vote -t=<topic>) нужно
последовательно запросить у пользователя:
• название (уникальное имя)
• тему голосования (описание)
• количество вариантов ответа
• варианты ответа
◦ view -t=<topic> -v=<vote> - отображает информацию по голосованию
▪ тема голосования
▪ варианты ответа и количество пользователей выбравших данный
вариант
◦ vote -t=<topic> -v=<vote> - запускает выбор ответа в голосовании для
текущего пользователя
Для этого приложение должно
▪ вывести варианты ответа для данного голосования
▪ запросить у пользователя выбор ответа
◦ delete -t=topic -v=<vote> - удалить голосование с именем <vote> из <topic>
(удалить может только пользователь его создавший)
◦ exit - завершение работы программы
● Серверные команды
◦ load <filename> - загрузка данных из файла
◦ save <filename> – сохранение в файл всех созданных разделов и
принадлежащим им голосований + их результатов (в любом удобном
формате).
◦ exit - завершение работы программы
