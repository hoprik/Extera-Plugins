"""
Message Queue Plugin для ExteraGram
Автоматически ставит сообщения в очередь при наличии задержки (cooldown) в чате
Allows sending multiple messages without waiting for cooldown
"""

import time
from threading import Thread, Lock
from collections import deque, defaultdict
from java.lang import Runnable, Integer, Boolean

from base_plugin import BasePlugin, MethodHook
from android_utils import run_on_ui_thread
from client_utils import run_on_queue
from hook_utils import find_class, get_private_field, set_private_field
from org.telegram.messenger import (
    LocaleController, 
    SendMessagesHelper, 
    NotificationCenter,
    AndroidUtilities,
    ChatObject
)
from org.telegram.tgnet import TLRPC
from ui.settings import Header, Switch, TextDetail, TextButton

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
        self.queues = defaultdict(deque)  # Per-chat queues
        self.lock = Lock()
        self.processing_chats = set()
        self.cooldown_until = {}  # Chat ID -> timestamp when cooldown ends

    def add_message(self, chat_id, message_data, cooldown_seconds=0):
        """Добавить сообщение в очередь для конкретного чата"""
        with self.lock:
            self.queues[chat_id].append({
                'data': message_data,
                'timestamp': time.time(),
                'retry_count': 0
            })
            
            # Установить время окончания cooldown
            if cooldown_seconds > 0:
                self.cooldown_until[chat_id] = time.time() + cooldown_seconds
            
            queue_size = len(self.queues[chat_id])
            self.plugin.log(f"Message added to queue for chat {chat_id}. Queue size: {queue_size}")
        
        # Запустить обработчик очереди для этого чата
        if chat_id not in self.processing_chats:
            self.start_processing(chat_id)

    def start_processing(self, chat_id):
        """Запустить обработку очереди для конкретного чата"""
        with self.lock:
            if chat_id in self.processing_chats:
                return
            self.processing_chats.add(chat_id)
        
        thread = Thread(target=self._process_queue, args=(chat_id,))
        thread.daemon = True
        thread.start()
        self.plugin.log(f"Queue processing started for chat {chat_id}")

    def _process_queue(self, chat_id):
        """Обработчик очереди для конкретного чата (работает в отдельном потоке)"""
        retry_interval = self.plugin.get_setting("retry_interval", 5)
        
        while True:
            try:
                # Проверить cooldown
                with self.lock:
                    cooldown_end = self.cooldown_until.get(chat_id, 0)
                    now = time.time()
                    
                    if now < cooldown_end:
                        # Еще есть cooldown, подождать
                        wait_time = cooldown_end - now
                        self.plugin.log(f"Chat {chat_id} has cooldown, waiting {wait_time:.1f}s")
                        time.sleep(min(wait_time, retry_interval))
                        continue
                    
                    # Проверить очередь
                    if not self.queues[chat_id]:
                        # Очередь пуста, остановить обработку
                        self.processing_chats.discard(chat_id)
                        self.plugin.log(f"Queue empty for chat {chat_id}, stopping processing")
                        break
                    
                    # Получить первое сообщение
                    message_item = self.queues[chat_id][0]
                
                # Попытаться отправить
                success, new_cooldown = self._try_send_message(chat_id, message_item['data'])
                
                if success:
                    # Успешно отправлено, удалить из очереди
                    with self.lock:
                        self.queues[chat_id].popleft()
                        queue_size = len(self.queues[chat_id])
                        self.plugin.log(f"Message sent successfully for chat {chat_id}. Remaining: {queue_size}")
                elif new_cooldown > 0:
                    # Обнаружен новый cooldown, обновить время
                    with self.lock:
                        self.cooldown_until[chat_id] = time.time() + new_cooldown
                        message_item['retry_count'] += 1
                    self.plugin.log(f"Cooldown detected for chat {chat_id}: {new_cooldown}s")
                else:
                    # Ошибка, но не cooldown
                    with self.lock:
                        message_item['retry_count'] += 1
                        if message_item['retry_count'] > 10:
                            # Слишком много попыток, удалить
                            self.queues[chat_id].popleft()
                            self.plugin.log(f"Message removed after too many retries for chat {chat_id}")
                
                # Подождать перед следующей попыткой
                time.sleep(retry_interval)
                
            except Exception as e:
                self.plugin.log(f"Error processing queue for chat {chat_id}: {e}")
                time.sleep(retry_interval)

    def _try_send_message(self, chat_id, message_data):
        """
        Попытка отправить сообщение
        Возвращает (success: bool, cooldown_seconds: int)
        """
        try:
            # TODO: Реализовать реальную отправку через SendMessagesHelper
            # Это заглушка для базовой функциональности
            self.plugin.log(f"Attempting to send message from queue for chat {chat_id}")
            
            # В реальной реализации здесь должен быть вызов SendMessagesHelper
            # и обработка FLOOD_WAIT ошибок
            # Например:
            # try:
            #     SendMessagesHelper.getInstance(account).sendMessage(...)
            #     return (True, 0)
            # except TLRPC.TL_error as error:
            #     if error.text.startswith("FLOOD_WAIT_"):
            #         seconds = int(error.text.split("_")[-1])
            #         return (False, seconds)
            #     return (False, 0)
            
            return (True, 0)  # Временная заглушка
            
        except Exception as e:
            self.plugin.log(f"Failed to send message: {e}")
            return (False, 0)

    def clear_queue(self, chat_id=None):
        """Очистить очередь (для конкретного чата или всех)"""
        with self.lock:
            if chat_id is None:
                # Очистить все очереди
                total = sum(len(q) for q in self.queues.values())
                self.queues.clear()
                self.cooldown_until.clear()
                self.plugin.log(f"All queues cleared. Removed {total} messages")
                return total
            else:
                # Очистить очередь для конкретного чата
                count = len(self.queues[chat_id])
                self.queues[chat_id].clear()
                self.cooldown_until.pop(chat_id, None)
                self.plugin.log(f"Queue cleared for chat {chat_id}. Removed {count} messages")
                return count

    def get_queue_size(self, chat_id=None):
        """Получить размер очереди"""
        with self.lock:
            if chat_id is None:
                return sum(len(q) for q in self.queues.values())
            else:
                return len(self.queues[chat_id])

    def get_queue_status(self):
        """Получить статус всех очередей"""
        with self.lock:
            status = {}
            for chat_id, queue in self.queues.items():
                if queue:
                    cooldown = max(0, self.cooldown_until.get(chat_id, 0) - time.time())
                    status[chat_id] = {
                        'size': len(queue),
                        'cooldown': cooldown
                    }
            return status


# ============ Plugin Class ============
class MessageQueuePlugin(BasePlugin):
    def __init__(self):
        super().__init__()
        self.queue_manager = MessageQueueManager(self)

    def on_plugin_load(self):
        """Вызывается при загрузке плагина"""
        self.log("Message Queue Plugin loaded")
        
        # Подписаться на уведомления об ошибках отправки
        # NotificationCenter используется для обработки различных событий в Telegram
        # TODO: Найти правильное событие для перехвата ошибок отправки
        # Например: NotificationCenter.messageSendError или подобное
        
    def on_plugin_unload(self):
        """Вызывается при выгрузке плагина"""
        # Остановить все обработчики очередей
        with self.queue_manager.lock:
            for chat_id in list(self.queue_manager.processing_chats):
                self.queue_manager.processing_chats.discard(chat_id)
        self.log("Message Queue Plugin unloaded")

    def create_settings(self):
        """Создать настройки плагина"""
        queue_size = self.queue_manager.get_queue_size()
        status_text = localizer.get_string("messages_queued").format(queue_size) if queue_size > 0 else localizer.get_string("queue_empty")
        
        # Получить детальный статус очередей
        queue_status = self.queue_manager.get_queue_status()
        if queue_status:
            status_lines = [f"Chat {chat_id}: {info['size']} msg" for chat_id, info in queue_status.items()]
            status_text = "\n".join(status_lines[:5])  # Показать только первые 5
        
        return [
            Header(localizer.get_string("settings_header")),
            
            Switch(
                key="enable_queue",
                text=localizer.get_string("enable_queue"),
                subtext=localizer.get_string("enable_queue_desc"),
                default=True,
                icon="msg_timer"
            ),
            
            Header(localizer.get_string("queue_status")),
            
            TextDetail(
                text=localizer.get_string("queue_status"),
                subtext=status_text,
                icon="msg_views_14"
            ),
            
            TextButton(
                text=localizer.get_string("clear_queue"),
                subtext=localizer.get_string("clear_queue_desc"),
                icon="msg_delete",
                on_click=lambda: self.clear_queue_action()
            ),
            
            Header(localizer.get_string("retry_interval")),
            
            TextDetail(
                text=localizer.get_string("retry_interval"),
                subtext=localizer.get_string("retry_interval_desc") + f" ({self.get_setting('retry_interval', 5)}s)",
                icon="msg_calendar"
            ),
        ]

    def clear_queue_action(self):
        """Действие для очистки очереди"""
        count = self.queue_manager.clear_queue()
        self.log(f"Cleared {count} messages from queue")
        # Обновить настройки, чтобы показать новый статус
        # TODO: Обновить UI настроек
        
    def handle_send_error(self, chat_id, message_data, error):
        """
        Обработать ошибку отправки сообщения
        Вызывается когда обнаружена ошибка FLOOD_WAIT
        """
        if not self.get_setting("enable_queue", True):
            return  # Плагин выключен
        
        # Проверить, является ли это ошибкой cooldown
        if hasattr(error, 'text') and error.text.startswith("FLOOD_WAIT_"):
            # Извлечь количество секунд из ошибки
            try:
                cooldown_seconds = int(error.text.split("_")[-1])
            except:
                cooldown_seconds = 5  # По умолчанию 5 секунд
            
            # Добавить сообщение в очередь
            self.queue_manager.add_message(chat_id, message_data, cooldown_seconds)
            self.log(f"Message queued due to FLOOD_WAIT_{cooldown_seconds} in chat {chat_id}")
