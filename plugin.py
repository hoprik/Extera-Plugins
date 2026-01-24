"""
Message Queue Plugin для ExteraGram
Автоматически ставит сообщения в очередь при наличии задержки (cooldown) в чате
Allows sending multiple messages without waiting for cooldown
"""

import time
from threading import Thread, Lock
from collections import deque
from java.lang import Runnable

from base_plugin import BasePlugin
from android_utils import run_on_ui_thread
from client_utils import run_on_queue
from org.telegram.messenger import LocaleController, SendMessagesHelper
from ui.settings import Header, Switch, TextDetail

# ============ Meta ============
__id__ = "qmsg"
__name__ = "Message Queue"
__description__ = "Автоматически ставит сообщения в очередь при cooldown в чате / Automatically queues messages when chat has cooldown"
__author__ = "@hoprik"
__version__ = "1.0"
__icon__ = "msg_timer"
__min_version__ = "11.12.0"


# ============ Localization ============
class LocalizationManager:
    strings = {
        "ru": {
            "settings_header": "Настройки очереди сообщений",
            "enable_queue": "Включить очередь сообщений",
            "enable_queue_desc": "Автоматически ставить сообщения в очередь при cooldown",
            "queue_status": "Статус очереди",
            "queue_empty": "Очередь пуста",
            "messages_queued": "Сообщений в очереди: {}",
            "retry_interval": "Интервал повтора (сек)",
            "retry_interval_desc": "Время между попытками отправки (1-60 секунд)",
            "clear_queue": "Очистить очередь",
            "clear_queue_desc": "Удалить все сообщения из очереди",
            "queue_cleared": "Очередь очищена",
            "message_queued": "Сообщение добавлено в очередь",
            "cooldown_detected": "Обнаружен cooldown, сообщение в очереди"
        },
        "en": {
            "settings_header": "Message Queue Settings",
            "enable_queue": "Enable Message Queue",
            "enable_queue_desc": "Automatically queue messages when cooldown is active",
            "queue_status": "Queue Status",
            "queue_empty": "Queue is empty",
            "messages_queued": "Messages in queue: {}",
            "retry_interval": "Retry Interval (sec)",
            "retry_interval_desc": "Time between send attempts (1-60 seconds)",
            "clear_queue": "Clear Queue",
            "clear_queue_desc": "Remove all messages from queue",
            "queue_cleared": "Queue cleared",
            "message_queued": "Message added to queue",
            "cooldown_detected": "Cooldown detected, message queued"
        }
    }

    def __init__(self):
        self.current_language = LocaleController.getInstance().getCurrentLocale().getLanguage()
        if '_' in self.current_language:
            self.current_language = self.current_language.split('_')[0]
        if self.current_language not in self.strings:
            self.current_language = "en"

    def get_string(self, key):
        return self.strings[self.current_language].get(
            key, self.strings["en"].get(key, key)
        )


localizer = LocalizationManager()


# ============ Message Queue Manager ============
class MessageQueueManager:
    def __init__(self, plugin):
        self.plugin = plugin
        self.queue = deque()
        self.lock = Lock()
        self.is_processing = False
        self.worker_thread = None

    def add_message(self, message_data):
        """Добавить сообщение в очередь"""
        with self.lock:
            self.queue.append({
                'data': message_data,
                'timestamp': time.time(),
                'retry_count': 0
            })
            self.plugin.log(f"Message added to queue. Queue size: {len(self.queue)}")
        
        # Запустить обработчик очереди если он не работает
        if not self.is_processing:
            self.start_processing()

    def start_processing(self):
        """Запустить обработку очереди"""
        if self.is_processing:
            return
        
        self.is_processing = True
        self.worker_thread = Thread(target=self._process_queue)
        self.worker_thread.daemon = True
        self.worker_thread.start()
        self.plugin.log("Queue processing started")

    def _process_queue(self):
        """Обработчик очереди (работает в отдельном потоке)"""
        retry_interval = self.plugin.get_setting("retry_interval", 5)
        
        while self.is_processing:
            try:
                with self.lock:
                    if not self.queue:
                        self.is_processing = False
                        self.plugin.log("Queue is empty, stopping processing")
                        break
                    
                    # Получить первое сообщение
                    message_item = self.queue[0]
                
                # Попытаться отправить
                success = self._try_send_message(message_item['data'])
                
                if success:
                    # Успешно отправлено, удалить из очереди
                    with self.lock:
                        self.queue.popleft()
                        self.plugin.log(f"Message sent successfully. Queue size: {len(self.queue)}")
                else:
                    # Не удалось отправить, обновить счетчик попыток
                    with self.lock:
                        message_item['retry_count'] += 1
                        if message_item['retry_count'] > 10:
                            # Слишком много попыток, удалить из очереди
                            self.queue.popleft()
                            self.plugin.log("Message removed after too many retries")
                
                # Подождать перед следующей попыткой
                time.sleep(retry_interval)
                
            except Exception as e:
                self.plugin.log(f"Error processing queue: {e}")
                time.sleep(retry_interval)

    def _try_send_message(self, message_data):
        """
        Попытка отправить сообщение
        Возвращает True если успешно, False если нужно повторить
        """
        try:
            # Здесь должна быть логика отправки сообщения
            # Пока просто возвращаем True для тестирования
            # TODO: Реализовать реальную отправку через SendMessagesHelper
            self.plugin.log("Attempting to send message from queue")
            return True
        except Exception as e:
            self.plugin.log(f"Failed to send message: {e}")
            return False

    def clear_queue(self):
        """Очистить очередь"""
        with self.lock:
            count = len(self.queue)
            self.queue.clear()
            self.plugin.log(f"Queue cleared. Removed {count} messages")
            return count

    def get_queue_size(self):
        """Получить размер очереди"""
        with self.lock:
            return len(self.queue)

    def stop_processing(self):
        """Остановить обработку очереди"""
        self.is_processing = False
        if self.worker_thread:
            self.worker_thread.join(timeout=2)


# ============ Plugin Class ============
class MessageQueuePlugin(BasePlugin):
    def __init__(self):
        super().__init__()
        self.queue_manager = MessageQueueManager(self)

    def on_plugin_load(self):
        """Вызывается при загрузке плагина"""
        self.log("Message Queue Plugin loaded")
        
        # TODO: Добавить хуки для перехвата отправки сообщений
        # Необходимо хукнуть методы SendMessagesHelper для перехвата ошибок cooldown
        # и автоматического добавления сообщений в очередь

    def on_plugin_unload(self):
        """Вызывается при выгрузке плагина"""
        self.queue_manager.stop_processing()
        self.log("Message Queue Plugin unloaded")

    def create_settings(self):
        """Создать настройки плагина"""
        queue_size = self.queue_manager.get_queue_size()
        status_text = localizer.get_string("messages_queued").format(queue_size) if queue_size > 0 else localizer.get_string("queue_empty")
        
        return [
            Header(localizer.get_string("settings_header")),
            
            Switch(
                key="enable_queue",
                text=localizer.get_string("enable_queue"),
                subtext=localizer.get_string("enable_queue_desc"),
                default=True,
                icon="msg_timer"
            ),
            
            TextDetail(
                text=localizer.get_string("queue_status"),
                subtext=status_text,
                icon="msg_views_14"
            ),
            
            Header(localizer.get_string("retry_interval")),
            
            TextDetail(
                text=localizer.get_string("retry_interval"),
                subtext=localizer.get_string("retry_interval_desc"),
                icon="msg_calendar"
            ),
            
            # TODO: Добавить слайдер для интервала повтора
            # Пока что используем значение по умолчанию 5 секунд
        ]

    def clear_queue_action(self):
        """Действие для очистки очереди"""
        count = self.queue_manager.clear_queue()
        # TODO: Показать toast с сообщением о количестве удаленных сообщений
        self.log(f"Cleared {count} messages from queue")
