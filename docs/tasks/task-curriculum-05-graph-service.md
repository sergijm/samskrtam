# Задача: curriculum-service — TopicGraphService (циклы + топологические слои)

**Что:** Сервисный класс с двумя алгоритмами: проверка цикла при добавлении prerequisite и вычисление слоёв для `/graph`.
**Зачем:** Единственная содержательная бизнес-логика сервиса, см. `docs/services/curriculum-service.md` §3.

## Зависит от
task-curriculum-03-entities.md

## Шаги
1. Класс `TopicGraphService` (пакет `sm.selflearn.samskrtam.curriculum.service`), зависит от `TopicPrerequisiteRepository`.
2. Метод `boolean wouldCreateCycle(UUID topicId, UUID prerequisiteTopicId)`:
   - если `topicId.equals(prerequisiteTopicId)` — вернуть `true` (self-loop тоже считаем как «отклонить», хотя в контроллере это отдельная явная проверка раньше вызова этого метода — см. task-06);
   - обход в ширину/глубину от `prerequisiteTopicId`: на каждом шаге брать текущие prerequisite узла (`topicPrerequisiteRepository.findByIdTopicId(currentNodeId)`, т.е. «от чего зависит текущий узел») и идти по цепочке дальше;
   - если в процессе обхода встречается `topicId` — вернуть `true` (цикл), иначе `false`. Использовать `Set<UUID> visited`, чтобы не зациклиться на уже существующих (валидных, ациклических) рёбрах.
3. Метод `TopicGraphResult computeLayers(List<Topic> allTopics, List<TopicPrerequisite> allEdges)` (возвращает внутренний record с `Map<Integer, List<Topic>>` по слоям + `List<Topic> evergreen`):
   - отфильтровать `isEvergreen = true` темы в отдельный список, не участвуют в сортировке;
   - алгоритм Кана: узлы без входящих рёбер (без prerequisite) — слой 0; для остальных — `слой = max(слой всех prerequisiteTopicId) + 1`, вычислять через обработку узлов в порядке, в котором у них «закрываются» все зависимости (topological order через очередь узлов с нулевой in-degree, аналогично классическому алгоритму Кана);
   - если в данных обнаружился цикл (не должно происходить благодаря `wouldCreateCycle`, но это защита на чтение) — бросить `IllegalStateException` с понятным сообщением, не падать молча и не зацикливаться;
   - внутри слоя сортировать по `displayOrder` (null — в конец), затем по `titleRu`.
4. Юнит-тесты: линейная цепочка A→B→C (3 слоя), несколько тем без зависимостей (все слой 0), diamond-граф (A→B, A→C, B→D, C→D — D в слое 2), попытка добавить ребро, создающее цикл (ожидаем `true` от `wouldCreateCycle`), evergreen-темы не попадают ни в один слой.

## Критерии готовности (DoD)
- [ ] Нет рекурсии без ограничения глубины / без защиты от переполнения стека на больших графах (использовать итеративный обход с явным стеком/очередью, не рекурсию)
