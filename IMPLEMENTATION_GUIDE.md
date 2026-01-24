# Message Queue Plugin - Implementation Guide

## Архитектура / Architecture

### Обзор / Overview

Плагин "qmsg" предоставляет систему очередей для автоматической обработки сообщений в чатах с cooldown (задержкой отправки). Когда Telegram возвращает ошибку FLOOD_WAIT, плагин автоматически ставит сообщение в очередь и повторяет отправку после окончания cooldown.

The "qmsg" plugin provides a queueing system for automatically handling messages in chats with cooldown (send delay). When Telegram returns a FLOOD_WAIT error, the plugin automatically queues the message and retries sending after the cooldown ends.

### Компоненты / Components

#### 1. MessageQueueManager
Управляет очередями сообщений для каждого чата:
- **Per-chat queues**: Отдельная очередь для каждого чата
- **Thread safety**: Потокобезопасные операции с использованием Lock
- **Cooldown tracking**: Отслеживание времени окончания cooldown для каждого чата
- **Background processing**: Фоновые потоки для обработки очередей

#### 2. LocalizationManager
Поддержка русского и английского языков для всех UI элементов

#### 3. MessageQueuePlugin
Главный класс плагина, наследуется от BasePlugin

## Текущая реализация / Current Implementation

### ✅ Реализовано / Implemented

1. **Система очередей / Queue System**
   - Отдельная очередь для каждого чата
   - Потокобезопасные операции
   - Автоматическая обработка в фоновых потоках

2. **Управление Cooldown / Cooldown Management**
   - Отслеживание времени окончания cooldown
   - Автоматическое ожидание перед повторной попыткой
   - Динамическое обновление времени cooldown

3. **UI и настройки / UI and Settings**
   - Переключатель включения/выключения
   - Статус очереди по чатам
   - Кнопка очистки очереди
   - Настройка интервала повтора

4. **Обработка ошибок / Error Handling**
   - Метод `handle_send_error()` для обработки FLOOD_WAIT
   - Автоматическое извлечение времени cooldown из ошибки
   - Логирование всех операций

### ⚠️ Требует доработки / Needs Implementation

#### Критические задачи / Critical Tasks

1. **Перехват отправки сообщений / Message Send Interception**

**Что нужно сделать / What needs to be done:**

Необходимо хукнуть метод отправки сообщений в SendMessagesHelper для перехвата попыток отправки и ошибок FLOOD_WAIT.

Need to hook the message sending method in SendMessagesHelper to intercept send attempts and FLOOD_WAIT errors.

**Пример реализации / Implementation Example:**

```python
def on_plugin_load(self):
    self.log("Message Queue Plugin loaded")
    
    # Импортировать необходимые классы
    from hook_utils import find_class
    
    try:
        # Найти класс SendMessagesHelper
        SendMessagesHelper_class = find_class("org.telegram.messenger.SendMessagesHelper").getClass()
        
        # Найти метод отправки (примеры возможных методов):
        # - sendMessage
        # - performSendMessageRequest
        # - performSendDelayedMessage
        
        # Пример хука для метода sendMessage:
        send_method = SendMessagesHelper_class.getDeclaredMethod(
            "sendMessage",
            # Параметры метода (нужно определить точные типы)
            # Например: String, long, boolean, ...
        )
        
        self.hook_method(send_method, MessageSendHook(self))
        self.log("Successfully hooked sendMessage method")
        
    except Exception as e:
        self.log(f"Failed to hook SendMessagesHelper: {e}")


class MessageSendHook(MethodHook):
    """Хук для перехвата отправки сообщений"""
    
    def __init__(self, plugin):
        self.plugin = plugin
    
    def before_hooked_method(self, param):
        """Вызывается перед отправкой сообщения"""
        # Можно проверить, есть ли активный cooldown для этого чата
        # и отменить отправку, поставив сообщение в очередь
        pass
    
    def after_hooked_method(self, param):
        """Вызывается после попытки отправки"""
        # Проверить результат отправки
        # Если была ошибка FLOOD_WAIT, добавить в очередь
        pass
    
    def on_error(self, param, error):
        """Вызывается при ошибке"""
        # Обработать FLOOD_WAIT ошибку
        if isinstance(error, TLRPC.TL_error):
            if error.text.startswith("FLOOD_WAIT_"):
                # Извлечь данные сообщения
                chat_id = # извлечь из param.args
                message_data = # извлечь из param.args
                
                # Добавить в очередь
                self.plugin.handle_send_error(chat_id, message_data, error)
                
                # Отменить показ ошибки пользователю
                param.setResult(None)
```

2. **Реализация отправки из очереди / Queue Send Implementation**

**Текущий код / Current Code:**
```python
def _try_send_message(self, chat_id, message_data):
    # TODO: Реализовать реальную отправку
    return (True, 0)  # Временная заглушка
```

**Что нужно / What's needed:**
```python
def _try_send_message(self, chat_id, message_data):
    try:
        # Получить account ID
        account_id = message_data.get('account', 0)
        
        # Получить SendMessagesHelper
        helper = SendMessagesHelper.getInstance(account_id)
        
        # Извлечь параметры сообщения
        message_text = message_data.get('text', '')
        reply_to_msg_id = message_data.get('reply_to', 0)
        # ... другие параметры
        
        # Отправить сообщение
        helper.sendMessage(
            message_text,
            chat_id,
            reply_to_msg_id,
            # ... другие параметры
        )
        
        return (True, 0)  # Успешно
        
    except TLRPC.TL_error as error:
        if error.text.startswith("FLOOD_WAIT_"):
            # Извлечь секунды
            seconds = int(error.text.split("_")[-1])
            return (False, seconds)
        return (False, 0)
        
    except Exception as e:
        self.plugin.log(f"Failed to send message: {e}")
        return (False, 0)
```

3. **Обработка пересылки / Forward Handling**

Аналогично отправке сообщений, нужно хукнуть метод пересылки:

Similar to message sending, need to hook the forward method:

```python
# В on_plugin_load():
forward_method = SendMessagesHelper_class.getDeclaredMethod(
    "sendMessage",  # или другой метод для пересылки
    # параметры
)
self.hook_method(forward_method, MessageForwardHook(self))
```

## Тестирование / Testing

### Как тестировать / How to Test

1. **Установка плагина / Plugin Installation**
   ```bash
   # Скопировать plugin.py в папку плагинов ExteraGram
   # Copy plugin.py to ExteraGram plugins folder
   ```

2. **Тест базовой функциональности / Basic Functionality Test**
   - Включить плагин в настройках
   - Проверить, что настройки открываются
   - Проверить локализацию (RU/EN)

3. **Тест очередей / Queue Test**
   - Найти чат с cooldown (или создать через @BotFather бота со slowmode)
   - Попытаться отправить несколько сообщений подряд
   - Проверить, что сообщения добавляются в очередь
   - Проверить статус очереди в настройках
   - Дождаться отправки сообщений

4. **Тест очистки / Clear Test**
   - Добавить сообщения в очередь
   - Нажать кнопку "Очистить очередь"
   - Проверить, что очередь пуста

### Логи / Logging

Все операции логируются. Проверить логи можно:
- В настройках плагинов ExteraGram
- Через `adb logcat` (Android Debug Bridge)

All operations are logged. Check logs via:
- ExteraGram plugin settings
- `adb logcat` (Android Debug Bridge)

## Известные ограничения / Known Limitations

1. **Максимум 10 попыток** на сообщение
   - После 10 неудачных попыток сообщение удаляется
   
2. **Интервал повтора** фиксированный (TODO: добавить слайдер в настройках)
   - Сейчас: 5 секунд по умолчанию
   - Планируется: настраиваемый от 1 до 60 секунд

3. **Отсутствует персистентность** очереди
   - При перезапуске приложения очередь сбрасывается
   - TODO: Сохранение очереди в базу данных или SharedPreferences

## Будущие улучшения / Future Improvements

### Приоритет 1 / Priority 1
- [ ] Реализовать перехват отправки сообщений
- [ ] Реализовать отправку из очереди
- [ ] Тестирование с реальным приложением

### Приоритет 2 / Priority 2
- [ ] Добавить слайдер для интервала повтора
- [ ] Сохранение очереди при перезапуске
- [ ] Уведомления о статусе очереди (toast/bulletin)

### Приоритет 3 / Priority 3
- [ ] Настройка максимального количества попыток
- [ ] Приоритеты сообщений в очереди
- [ ] Статистика отправленных сообщений
- [ ] Экспорт/импорт очереди

## Поддержка / Support

Вопросы и предложения:
- GitHub Issues: https://github.com/hoprik/Extera-Plugins/issues
- Telegram: @hoprik

Questions and suggestions:
- GitHub Issues: https://github.com/hoprik/Extera-Plugins/issues
- Telegram: @hoprik

## Лицензия / License

Следует лицензии репозитория / Follows repository license
