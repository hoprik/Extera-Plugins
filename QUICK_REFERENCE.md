# Message Queue Plugin (qmsg) - Краткая справка / Quick Reference

## Быстрый старт / Quick Start

### Название плагина / Plugin Name
**ID:** `qmsg`  
**Название / Name:** Message Queue  
**Автор / Author:** @hoprik

### Что делает плагин / What It Does
🇷🇺 Автоматически ставит сообщения в очередь когда в чате есть cooldown (задержка отправки), и отправляет их по таймеру без необходимости ждать.

🇬🇧 Automatically queues messages when a chat has cooldown (send delay), and sends them on a timer without needing to wait.

## Основные функции / Key Features

✅ **Автоматическая очередь** - Пишите сразу, плагин сам подождет  
✅ **Умная обработка** - Определяет cooldown из ошибки Telegram  
✅ **Пересылка** - Работает и с пересылкой сообщений  
✅ **Статус** - Видно сколько сообщений в очереди  
✅ **Настройки** - Интервал повтора, очистка очереди  

✅ **Automatic Queue** - Write immediately, plugin waits for you  
✅ **Smart Handling** - Detects cooldown from Telegram error  
✅ **Forwarding** - Works with message forwarding too  
✅ **Status** - See how many messages are queued  
✅ **Settings** - Retry interval, clear queue  

## Установка / Installation

1. Скопировать `plugin.py` в папку плагинов ExteraGram
2. Перезапустить ExteraGram
3. Включить плагин в настройках

1. Copy `plugin.py` to ExteraGram plugins folder
2. Restart ExteraGram
3. Enable plugin in settings

## Использование / Usage

### Обычное использование / Normal Use
Просто пишите сообщения как обычно! Если есть cooldown, плагин автоматически:
1. Поставит сообщение в очередь
2. Подождет нужное время
3. Отправит сообщение
4. Продолжит со следующими сообщениями

Just write messages as usual! If there's cooldown, the plugin automatically:
1. Queues the message
2. Waits the required time
3. Sends the message
4. Continues with next messages

### Проверка статуса / Check Status
Настройки плагина → Статус очереди  
Plugin settings → Queue status

### Очистка очереди / Clear Queue
Настройки плагина → Очистить очередь  
Plugin settings → Clear queue

## Настройки / Settings

| Настройка / Setting | По умолчанию / Default | Описание / Description |
|---------------------|----------------------|----------------------|
| Включить очередь / Enable queue | ✅ Включено / Enabled | Включает/выключает плагин |
| Интервал повтора / Retry interval | 5 секунд / seconds | Время между попытками отправки |

## Технические детали / Technical Details

### Как работает / How It Works
1. **Перехват ошибки** - Ловит `FLOOD_WAIT_X` от Telegram API
2. **Извлечение времени** - Определяет X секунд cooldown
3. **Постановка в очередь** - Добавляет сообщение в очередь чата
4. **Ожидание** - Ждет X секунд
5. **Повторная отправка** - Пытается отправить снова

1. **Intercept error** - Catches `FLOOD_WAIT_X` from Telegram API
2. **Extract time** - Determines X seconds cooldown
3. **Queue** - Adds message to chat queue
4. **Wait** - Waits X seconds
5. **Retry** - Tries to send again

### Ограничения / Limitations
- Максимум 10 попыток на сообщение / Maximum 10 retries per message
- Очередь не сохраняется при перезапуске / Queue doesn't persist on restart
- Интервал 5 сек фиксированный (пока) / 5 sec interval is fixed (for now)

## FAQ

### 🇷🇺 Русский

**Q: Плагин не работает, что делать?**  
A: Проверьте что плагин включен в настройках. Проверьте логи плагина.

**Q: Сообщения не отправляются из очереди**  
A: Это нормально для текущей версии - требуется доработка интеграции с Telegram API. См. IMPLEMENTATION_GUIDE.md

**Q: Как настроить время ожидания?**  
A: Пока фиксировано 5 секунд, настройка будет добавлена в следующей версии.

**Q: Можно ли сохранить очередь при перезапуске?**  
A: Пока нет, эта функция планируется в будущем.

### 🇬🇧 English

**Q: Plugin doesn't work, what to do?**  
A: Check that plugin is enabled in settings. Check plugin logs.

**Q: Messages not sending from queue**  
A: This is expected in current version - requires integration work with Telegram API. See IMPLEMENTATION_GUIDE.md

**Q: How to configure wait time?**  
A: Currently fixed at 5 seconds, configuration will be added in next version.

**Q: Can queue persist on restart?**  
A: Not yet, this feature is planned for the future.

## Файлы / Files

- `plugin.py` - Основной файл плагина / Main plugin file
- `README_PLUGIN.md` - Подробная документация / Detailed documentation
- `IMPLEMENTATION_GUIDE.md` - Руководство для разработчиков / Developer guide
- `QUICK_REFERENCE.md` - Этот файл / This file

## Поддержка / Support

**Telegram:** @hoprik  
**GitHub:** https://github.com/hoprik/Extera-Plugins

## Версия / Version

**1.0** - Первый релиз / Initial release  
**Дата / Date:** 2026-01-24

---

**Важно / Important:**  
Плагин предоставляет полную инфраструктуру для очередей, но требует доработки интеграции с Telegram API для полной функциональности. См. IMPLEMENTATION_GUIDE.md для деталей.

The plugin provides complete queue infrastructure but requires integration work with Telegram API for full functionality. See IMPLEMENTATION_GUIDE.md for details.
