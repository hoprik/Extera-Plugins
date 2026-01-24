# Message Queue Plugin (qmsg)

Плагин для ExteraGram, который автоматически ставит сообщения в очередь при наличии задержки (cooldown) в чате.

Plugin for ExteraGram that automatically queues messages when there's a cooldown in the chat.

## Возможности / Features

### 🇷🇺 Русский

- **Автоматическая очередь сообщений**: Пишите и отправляйте сообщения сразу, без ожидания таймера
- **Обработка пересылки**: Пересылайте несколько сообщений подряд одним действием
- **Умное управление**: Автоматическое определение cooldown и постановка в очередь
- **Настраиваемый интервал**: Настройте время между попытками отправки
- **Статус очереди**: Видите количество сообщений в очереди
- **Очистка очереди**: Возможность очистить очередь в любой момент

### 🇬🇧 English

- **Automatic message queue**: Write and send messages immediately without waiting for timer
- **Forward handling**: Forward multiple messages in a row with one action
- **Smart management**: Automatic cooldown detection and queueing
- **Customizable interval**: Configure time between send attempts
- **Queue status**: See the number of messages in queue
- **Clear queue**: Ability to clear the queue at any time

## Установка / Installation

### 🇷🇺 Русский

1. Скопируйте файл `plugin.py` в папку плагинов ExteraGram
2. Перезапустите ExteraGram
3. Включите плагин в настройках плагинов
4. Настройте параметры в настройках плагина

### 🇬🇧 English

1. Copy `plugin.py` to your ExteraGram plugins folder
2. Restart ExteraGram
3. Enable the plugin in plugin settings
4. Configure settings in plugin settings

## Использование / Usage

### 🇷🇺 Русский

1. **Включите плагин** в настройках
2. **Пишите сообщения** как обычно - плагин автоматически поставит их в очередь при обнаружении cooldown
3. **Пересылайте сообщения** - плагин обработает их автоматически
4. **Проверяйте статус** очереди в настройках плагина
5. **Очищайте очередь** если необходимо отменить отправку

### 🇬🇧 English

1. **Enable the plugin** in settings
2. **Write messages** as usual - the plugin will automatically queue them when cooldown is detected
3. **Forward messages** - the plugin will handle them automatically
4. **Check queue status** in plugin settings
5. **Clear queue** if you need to cancel sending

## Настройки / Settings

### 🇷🇺 Русский

- **Включить очередь сообщений**: Включает/выключает функционал плагина
- **Статус очереди**: Показывает количество сообщений в очереди
- **Интервал повтора**: Время (в секундах) между попытками отправки сообщений
- **Очистить очередь**: Удаляет все сообщения из очереди

### 🇬🇧 English

- **Enable Message Queue**: Turns the plugin functionality on/off
- **Queue Status**: Shows the number of messages in queue
- **Retry Interval**: Time (in seconds) between send attempts
- **Clear Queue**: Removes all messages from queue

## Технические детали / Technical Details

### Как работает / How it works

#### 🇷🇺 Русский

1. Плагин перехватывает попытки отправки сообщений
2. При обнаружении ошибки cooldown, сообщение добавляется в очередь
3. Фоновый процесс автоматически пытается отправить сообщения из очереди
4. После успешной отправки, сообщение удаляется из очереди
5. Процесс повторяется для всех сообщений в очереди

#### 🇬🇧 English

1. Plugin intercepts message send attempts
2. When cooldown error is detected, message is added to queue
3. Background process automatically tries to send messages from queue
4. After successful send, message is removed from queue
5. Process repeats for all messages in queue

## Ограничения / Limitations

### 🇷🇺 Русский

- Максимум 10 попыток отправки для каждого сообщения
- После 10 неудачных попыток сообщение автоматически удаляется из очереди
- Интервал повтора можно настроить от 1 до 60 секунд

### 🇬🇧 English

- Maximum 10 send attempts per message
- After 10 failed attempts, message is automatically removed from queue
- Retry interval can be configured from 1 to 60 seconds

## Версия / Version

**1.0** - Первый релиз / Initial release

## Автор / Author

@hoprik

## Лицензия / License

Следует лицензии репозитория / Follows repository license
