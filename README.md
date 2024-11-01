Разработать приложение repo - generator с использованием технологий Java (версия 17 и выше) и Spring Boot (версия 3 и выше) со следующим функционалом: 
1. Приложение интегрируется с двумя облачными git - сервисами (например, GitHub и Bitbucket ) , один из которых выступает в качестве сервиса - источника, а другой – в качестве целевого сервиса .
2. У каждого сервиса есть AP I по работе с ним , документация : GitHub https://docs.github.com/en/rest Bitbucket https://developer.atlassian.com/cloud/bitbucket/rest/ GitLab https://docs.gitlab.com/ee/api/rest/ которое позволяет создать репозиторий, удалить репозиторий, получить список репозиториев и т.п. Для работы с каждым сервисом необходимо написать HTTP - клиент – интерфейс и его реализацию , например, используя класс RestTemplate , Feign или аналогичные технологии .
3. Пользователь приложения может получить список всех репозиториев в сервисе - источнике. То есть, как называются репозитории, например, на GitHub .
4. Пользователь может запустить обновление локального репозитория (на диске) из сервиса - источника . Если репозиторий есть в сервисе - источнике, но его нет на диске (т.е. приложение видит этот репозиторий впервые), то репозиторий клонируется на диск. Если репозиторий уже есть на диске (т.е. он уже был склонирован ранее), то необходимо обновить информацию из нег о для каждой ветки.
5. Пользователь может запустить обновление репозитория в целевом сервисе из локального репозитория (на диске) . Если репозитория нет в целевом сервисе, то предварительно его создать (через API ). После этого приложение проходится по всем веткам и пушит каждую ветку в удаленный репозиторий.
6. Пользователь может запустить синхронизацию всех репозиториев из сервиса - источника на диск .
7. Пользователь может запустить синхронизацию всех локальных репозиториев (на диск е) в целевой сервис.
8. Для за пуска вышеуказанных команд необходимо написать Spring - контроллер используя аннотацию @ RestController . Запросы должны подходить по семантике REST ( GET для получения данных и т.п.) .
9. Для работы с контроллером добавить библиотеку Swagger .
10. Для работы с git - репозиторием ( команды clone , pull , push и т.п.) можно использовать библиотеки, например JGit .
11. Н астройки приложения ( имя пользователя , токены доступа и др) хранить в файл е application . yml . 
12. Для работы с библиотеками использовать Maven или Gradle .
