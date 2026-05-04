# PokeCollect — Pokémon TCG Collection App

## Стек
- Kotlin + Jetpack Compose
- Min SDK 26
- Material 3
- Архитектура: MVVM + Clean Architecture lite
- DI: Hilt
- БД: Room
- Сеть: Retrofit + Kotlin Serialization
- Изображения: Coil
- Камера: CameraX
- OCR: Google ML Kit Text Recognition (on-device)

## Структура пакетов
com.yeromich.pokecollect/
├── ui/screens/        # Compose-экраны
├── ui/components/     # Переиспользуемые композаблы
├── ui/theme/          # Material 3 темы
├── data/local/        # Room: entities, DAO, database
├── data/remote/       # Retrofit: API, DTOs
├── data/repository/   # Репозитории
├── domain/model/      # Доменные модели
├── domain/usecase/    # Use cases
└── di/                # Hilt-модули

## Конвенции
- Composable-функции: PascalCase
- Файлы экранов: <Name>Screen.kt
- ViewModels: <Name>ViewModel.kt
- Не используй deprecated API
- Все строки в strings.xml
- Не пиши комментарии к очевидному коду
- Используй последние стабильные версии библиотек

## API
Pokémon TCG API: https://api.pokemontcg.io/v2
Документация: https://docs.pokemontcg.io/
