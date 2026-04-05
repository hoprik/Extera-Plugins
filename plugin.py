"""
Автор - @hoprik
Дизайн - @canickk
Идея с dex и хуки - @PESSDES_Plugins
Сурсы Плеера - https://github.com/hoprik/Extera-Plugins/tree/fullscreen_dex
"""

import base64
import hashlib
import sys
from android.app import Dialog
from java.lang import Boolean, Integer, String
from java.nio import ByteBuffer
from java.util import HashMap  # <--- Импортируем Java HashMap
from dalvik.system import InMemoryDexClassLoader
from org.telegram.messenger.browser import Browser

from base_plugin import BasePlugin, MenuItemData, MenuItemType, MethodHook
from client_utils import get_last_fragment, run_on_queue
from android_utils import run_on_ui_thread
from hook_utils import find_class, get_private_field

from com.exteragram.messenger.plugins import PluginsController
from org.telegram.messenger import MediaController, LocaleController, ApplicationLoader
from org.telegram.ui.ActionBar import ActionBarMenuItem, BaseFragment
from org.telegram.ui.Components import AudioPlayerAlert
from ui.settings import Header, Switch, Text

import hashlib
import json
import os
import threading
import time
import urllib.request
import uuid

# === mkStats: embed client start ===
MKSTATS_API_URL = os.getenv("MKSTATS_API_URL", "https://mkstats.mk69.su/api")
MKSTATS_PING_INTERVAL = int(os.getenv("MKSTATS_PING_INTERVAL", "1500"))

def generate_user_hash(device_id: str, plugin_id: str) -> str:
    payload = f"{device_id}:{plugin_id}:mkstats:v1"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()

def _normalize_api_base(api_url: str) -> str:
    base = api_url.rstrip("/")
    if base.endswith("/api"):
        return f"{base}/v1"
    return base

def _post_json(url: str, payload: dict) -> dict:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url, data=data, headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        body = response.read().decode("utf-8")
    return json.loads(body)

class MkStatsCoreClient:
    def __init__(self, api_url: str, plugin_id: str, plugin_version: str, user_hash: str, client_version: str | None = None, client_name: str | None = None) -> None:
        self.api_base = _normalize_api_base(api_url)
        self.plugin_id = plugin_id
        self.plugin_version = plugin_version
        self.client_version = client_version
        self.client_name = client_name
        self.user_hash = user_hash

    def handshake(self) -> dict:
        payload = {
            "plugin_id": self.plugin_id,
            "version": self.plugin_version,
            "client_name": self.client_name,
            "client_version": self.client_version,
            "user_hash": self.user_hash,
        }
        return _post_json(f"{self.api_base}/handshake", payload)

    def send_ping(self, install_token: str, timestamp=None) -> dict:
        payload = {
            "plugin_id": self.plugin_id,
            "version": self.plugin_version,
            "client_name": self.client_name,
            "client_version": self.client_version,
            "user_hash": self.user_hash,
            "install_token": install_token,
            "timestamp": timestamp or int(time.time()),
        }
        return _post_json(f"{self.api_base}/data", payload)

    def send_event(self, install_token: str, event: str, count: int = 1, timestamp=None) -> dict:
        payload = {
            "plugin_id": self.plugin_id,
            "version": self.plugin_version,
            "client_name": self.client_name,
            "client_version": self.client_version,
            "user_hash": self.user_hash,
            "install_token": install_token,
            "event": event,
            "count": count,
            "timestamp": timestamp or int(time.time()),
        }
        return _post_json(f"{self.api_base}/event", payload)
# === mkStats: embed client end ===



# ============ Meta ============
__id__ = "fullscreen_music_player"
__name__ = "Music Player"
__description__ = "Full screen music player. Если вы обновляете плагин, перезапустите Telegram."
__author__ = "@hoprik"
__version__ = "1.2"
__icon__ = "rottenprince_by_FStikBot/0"
__min_version__ = "11.12.0"

# ============ Global Vars ============
SHOW_PLAYER_ITEM_ID = 1001
PLAYER_CLASS_NAME = "ru.hoprik.player.MusicPlayer"
MusicPlayer = None
dex_data =  # DEX_DATA_HERE #


# ============ Localization ============
class LocalizationManager:
    strings = {
        "ru": {
            "player": "Плеер",
            "no_music": "Включите музыку",
            "now_playing": "Сейчас играет",
            "start_error": "Ошибка запуска",
            "saved": "Музыка сохранена в избранное",
            "downloaded": "Музыка была скачана",
            "settings_name": "Настройки плеера",
            "settings_elements": "Настройка элементов",
            "settings_background": "Настройка фона",
            "settings_enter": "Настройки запуска",
            "settings_addons": "Дополнения",
            "settings_enable_feature_shuffle": "Repeat и shuffle трэков",
            "settings_enable_feature_download": "Скачивание трэков",
            "settings_enable_feature_share": "Поделиться трэком",
            "settings_enable_feature_save": "Сохранение трэка",
            "settings_enable_feature_save_profile": "Сохранение трэка в профиль",
            "settings_enable_enter_audioplayer": "Заменить мини-плеер",
            "settings_enable_enter_subitem": "Добавить элемент в мини-плеер",
            "settings_enable_enter_profile": "Добавить элемент в профиль",
            "settings_enable_enter_chat": "Добавить элемент в чат",
            "settings_enable_enter_sidebar": "Добавить элемент в sidebar (Работает только если вы добавить элемент плагины в sidebar)",
            "settings_enable_addon_lyrics": "Включить поддержку плагина lyrics (Работает только если его поставили)"
        },
        "en": {
            "player": "Player",
            "no_music": "Play some music",
            "now_playing": "Now playing",
            "start_error": "Launch error",
            "saved": "Added to favorites",
            "downloaded": "Track downloaded",
            "settings_name": "Player Settings",
            "settings_elements": "Element Settings",
            "settings_background": "Background Settings",
            "settings_enter": "Launch Settings",
            "settings_addons": "Addons",
            "settings_enable_feature_shuffle": "Repeat and shuffle tracks",
            "settings_enable_feature_download": "Track downloading",
            "settings_enable_feature_share": "Share track",
            "settings_enable_feature_save": "Save track",
            "settings_enable_enter_audioplayer": "Replace mini-player",
            "settings_enable_enter_subitem": "Add element to mini-player",
            "settings_enable_enter_profile": "Add element to profile",
            "settings_enable_enter_chat": "Add element to chat",
            "settings_enable_enter_sidebar": "Add element to sidebar (Works only if Plugins are added to sidebar)",
            "settings_enable_addon_lyrics": "Enable lyrics plugin support (Requires plugin to be installed)"
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


# ============ Utils ============
def is_music() -> bool:
    playing_obj = MediaController.getInstance().getPlayingMessageObject()
    return playing_obj and playing_obj.isMusic()


def get_icon_id(name: str) -> int:
    context = get_last_fragment().getContext()
    return context.getResources().getIdentifier(name, "drawable", context.getPackageName())


def python_dict_to_java_map(py_dict):
    outer_map = HashMap()
    for lang_code, translations in py_dict.items():
        inner_map = HashMap()
        for key, value in translations.items():
            inner_map.put(String(key), String(value))
        outer_map.put(String(lang_code), inner_map)
    return outer_map


# ============ Plugin Class ============
class PlayerPlugin(BasePlugin):

    # === mkStats: integration start ===
    def _mkstats_get_setting(self, key: str, default):
        try:
            if hasattr(self, "get_setting"):
                return self.get_setting(key, default)
            if hasattr(self, "getsetting"):
                return self.getsetting(key, default)
        except Exception:
            pass
        return default

    def _mkstats_set_setting(self, key: str, value, reload_settings: bool = False):
        try:
            if hasattr(self, "set_setting"):
                return self.set_setting(key, value, reload_settings=reload_settings)
            if hasattr(self, "setsetting"):
                return self.setsetting(key, value, reloadsettings=reload_settings)
        except Exception:
            pass
        return None

    def _mkstats_get_device_id(self) -> str:
        device_id = self._mkstats_get_setting("mkstats_device_id", "")
        if not device_id:
            device_id = uuid.uuid4().hex
            self._mkstats_set_setting("mkstats_device_id", device_id, reload_settings=False)
        return device_id

    def _mkstats_get_client_version(self) -> str:
        try:
            from org.telegram.messenger import BuildVars
            version = getattr(BuildVars, "BUILD_VERSION_STRING", None) or getattr(BuildVars, "BUILD_VERSION", None)
            if version:
                return str(version)
        except Exception:
            pass
        try:
            from org.telegram.messenger import BuildConfig as TgBuildConfig
            version = getattr(TgBuildConfig, "VERSION_NAME", None)
            if version:
                return str(version)
        except Exception:
            pass
        try:
            from com.radolyn.ayugram import BuildConfig as AyuBuildConfig
            version = getattr(AyuBuildConfig, "VERSION_NAME", None)
            if version:
                return str(version)
        except Exception:
            pass
        try:
            from com.exteragram.messenger import BuildConfig as ExBuildConfig
            version = getattr(ExBuildConfig, "VERSION_NAME", None)
            if version:
                return str(version)
        except Exception:
            pass
        try:
            from org.telegram.messenger import ApplicationLoader
            ctx = ApplicationLoader.applicationContext
            if ctx:
                pm = ctx.getPackageManager()
                pkg = ctx.getPackageName()
                info = pm.getPackageInfo(pkg, 0)
                version = getattr(info, "versionName", None) or getattr(info, "versionCode", None)
                if version:
                    return str(version)
        except Exception:
            pass
        return "unknown"

    def _mkstats_get_client_name(self) -> str:
        try:
            from org.telegram.messenger import ApplicationLoader
            ctx = ApplicationLoader.applicationContext
            if ctx:
                pkg = ctx.getPackageName()
                if pkg == "com.radolyn.ayugram":
                    return "AyuGram"
                if pkg == "com.exteragram.messenger":
                    return "exteraGram"
                if pkg == "org.telegram.messenger":
                    return "Telegram"
                if pkg:
                    return str(pkg)
        except Exception:
            pass
        try:
            from com.radolyn.ayugram import BuildConfig as AyuBuildConfig
            _ = AyuBuildConfig.VERSION_NAME
            return "AyuGram"
        except Exception:
            pass
        try:
            from com.exteragram.messenger import BuildConfig as ExBuildConfig
            _ = ExBuildConfig.VERSION_NAME
            return "exteraGram"
        except Exception:
            pass
        return "unknown"

    def _mkstats_log(self, message: str) -> None:
        if hasattr(self, "log"):
            try:
                self.log(message)
            except Exception:
                pass

    def _mkstats_event(self, event: str, count: int = 1) -> None:
        if not event:
            return

        def _send():
            try:
                if not hasattr(self, "_mkstats_client"):
                    return
                if not getattr(self, "_mkstats_token", ""):
                    data = self._mkstats_client.handshake()
                    self._mkstats_token = data.get("install_token", "")
                    if self._mkstats_token:
                        self._mkstats_set_setting("mkstats_install_token", self._mkstats_token, reload_settings=False)
                if self._mkstats_token:
                    self._mkstats_client.send_event(self._mkstats_token, event, count=count)
            except Exception as exc:
                self._mkstats_log(f"mkStats: event error {exc}")
                self._mkstats_token = ""
                self._mkstats_set_setting("mkstats_install_token", "", reload_settings=False)

        try:
            threading.Thread(target=_send, daemon=True).start()
        except Exception:
            pass

    def _mkstats_loop(self):
        while not self._mkstats_stop.is_set():
            try:
                if not self._mkstats_token:
                    self._mkstats_log("mkStats: handshake start")
                    data = self._mkstats_client.handshake()
                    self._mkstats_token = data.get("install_token", "")
                    if self._mkstats_token:
                        self._mkstats_set_setting("mkstats_install_token", self._mkstats_token, reload_settings=False)
                        self._mkstats_log("mkStats: handshake ok, token stored")
                    else:
                        self._mkstats_log("mkStats: handshake response missing token")

                if self._mkstats_token:
                    self._mkstats_log("mkStats: sending ping")
                    self._mkstats_client.send_ping(self._mkstats_token)
                    self._mkstats_log("mkStats: ping sent")
            except Exception as exc:
                self._mkstats_log(f"mkStats: error {exc}")
                self._mkstats_token = ""
                self._mkstats_set_setting("mkstats_install_token", "", reload_settings=False)
            self._mkstats_stop.wait(MKSTATS_PING_INTERVAL)

    def _mkstats_start(self):
        try:
            device_id = self._mkstats_get_device_id()
            user_hash = generate_user_hash(device_id, __id__)
            client_name = self._mkstats_get_client_name()
            client_version = self._mkstats_get_client_version()
            self._mkstats_client = MkStatsCoreClient(MKSTATS_API_URL, __id__, __version__, user_hash, client_version, client_name)
            self._mkstats_stop = threading.Event()
            self._mkstats_token = self._mkstats_get_setting("mkstats_install_token", "")
            self._mkstats_thread = threading.Thread(target=self._mkstats_loop, daemon=True)
            self._mkstats_thread.start()
            self._mkstats_log(f"mkStats: client started ({self._mkstats_client.api_base})")
        except Exception:
            pass
    # === mkStats: integration end ===

    def __init__(self):
        super().__init__()
        self.chat_settings_item = None
        self.profile_settings_item = None
        self.drawer_menu_item = None

    def on_plugin_load(self):

        # === mkStats: integration start ===
        self._mkstats_start()
        # === mkStats: integration end ===

        self.add_settings_menu_items()

        self.hook_method(AudioPlayerAlert.getClass().getDeclaredConstructors()[0], AudioPlayerAlertHook(self))
        self.hook_method(AudioPlayerAlert.getClass().getDeclaredMethod("updateTitle", Boolean.TYPE), UpdateHook(self))
        self.hook_method(AudioPlayerAlert.getClass().getDeclaredMethod("onSubItemClick", Integer.TYPE),
                         SubItemClickHook(self))
        self.hook_method(BaseFragment.getClass().getDeclaredMethod("showDialog", Dialog),
                         InterceptStandardPlayerHook(self))

        run_on_queue(self.dex_load)

    def on_plugin_unload(self):

        # === mkStats: integration start ===
        if hasattr(self, "_mkstats_stop"):
            self._mkstats_stop.set()
            self._mkstats_log("mkStats: stop requested")
            try:
                if hasattr(self, "_mkstats_thread") and self._mkstats_thread is not None:
                    self._mkstats_thread.join(timeout=1.0)
            except Exception:
                pass
        # === mkStats: integration end ===

        self.remove_settings_menu_items()

    def dex_load(self):
        global MusicPlayer
        try:
            # Декодируем один раз и сохраняем байты
            dex_bytes = base64.b64decode(dex_data)
        except Exception as e:
            self.log(f"Failed to decode dex_data: {e}")
            return

        # Проверяем хеш
        try:
            computed_hash = hashlib.sha256(dex_bytes).hexdigest()
            if computed_hash != dex_hash:
                self.log(f"FATAL: dex hash mismatch! Expected {dex_hash}, got {computed_hash}. Plugin will not load.")
                return
            else:
                self.log("dex hash check passed")
        except Exception as e:
            self.log(f"Error computing dex hash: {e}")
            return

        # Дальше загрузка с уже имеющимися байтами
        try:
            clazz = find_class(PLAYER_CLASS_NAME).getClass()
            self.log(f"Found existing class: {PLAYER_CLASS_NAME}")
        except:
            self.log(f"Class not found, loading DEX...")
            loader = InMemoryDexClassLoader(
                ByteBuffer.wrap(dex_bytes),   # используем dex_bytes
                ApplicationLoader.applicationContext.getClassLoader()
            )
            clazz = loader.loadClass(PLAYER_CLASS_NAME)

        try:
            MusicPlayer = clazz.getDeclaredMethod("getInstance").invoke(None)
            self.log("MusicPlayer instance created")
            try:
                java_translations = python_dict_to_java_map(localizer.strings)
                MusicPlayer.setLocalizations(java_translations)
                self.log("Translations passed successfully")
            except Exception as e:
                self.log(f"Warning: Could not pass translations (update DEX?): {e}")

        except Exception as e:
            self.log(f"FATAL ERROR getting instance: {e}")

    def create_settings(self):
        return [
            Header(localizer.get_string("settings_elements")),
            Switch(key="enable_feature_shuffle", text=localizer.get_string("settings_enable_feature_shuffle"),
                   default=True, icon="msg_settings"),
            Switch(key="enable_feature_download", text=localizer.get_string("settings_enable_feature_download"),
                   default=True, icon="msg_settings"),
            Switch(key="enable_feature_share", text=localizer.get_string("settings_enable_feature_share"), default=True,
                   icon="msg_settings"),
            Switch(key="enable_feature_save", text=localizer.get_string("settings_enable_feature_save"), default=True,
                   icon="msg_settings"),
            Switch(key="enable_feature_save_profile", text=localizer.get_string("settings_enable_feature_save_profile"), default=True,
                   icon="msg_settings"),
            Header(localizer.get_string("settings_enter")),
            Switch(key="enable_audioplayer", text=localizer.get_string("settings_enable_enter_audioplayer"),
                   default=False, icon="msg_settings"),
            Switch(key="enable_subitem", text=localizer.get_string("settings_enable_enter_subitem"),
                   subtext=localizer.get_string("settings_enable_enter_subitem"), default=True, icon="msg_settings"),
            Switch(key="enable_enter_profile", text=localizer.get_string("settings_enable_enter_profile"),
                   subtext=localizer.get_string("settings_enable_enter_profile"), default=True, icon="msg_settings",
                   on_change=self._reload_menu_items()),
            Switch(key="enable_enter_chat", text=localizer.get_string("settings_enable_enter_chat"),
                   subtext=localizer.get_string("settings_enable_enter_chat"), default=True, icon="msg_settings",
                   on_change=self._reload_menu_items()),
            Switch(key="enable_enter_sidebar", text=localizer.get_string("settings_enable_enter_sidebar"),
                   subtext=localizer.get_string("settings_enable_enter_sidebar"), default=True, icon="msg_settings",
                   on_change=self._reload_menu_items()),
            Header(localizer.get_string("settings_addons")),
            Switch(key="enable_lyrics", text=localizer.get_string("settings_enable_addon_lyrics"),
                   subtext=localizer.get_string("settings_enable_addon_lyrics"), default=False, icon="msg_settings"),
            Text(text="Скачать плагин lyrics", on_click=self._open_plugin_lyrics)
        ]

    def _open_plugin_lyrics(self, view):
        if MusicPlayer:
            MusicPlayer.openBrowser(get_last_fragment())

    def _reload_menu_items(self):
        self.remove_settings_menu_items()
        self.add_settings_menu_items()

    def add_settings_menu_items(self):

        if not self.chat_settings_item and self.get_setting("enable_enter_chat", True):
            self.chat_settings_item = self.add_menu_item(
                MenuItemData(
                    menu_type=MenuItemType.CHAT_ACTION_MENU,
                    text=localizer.get_string("player"),
                    icon="player",
                    priority=1,
                    on_click=lambda ctx: run_on_ui_thread(
                        lambda: self.setup_player_ui()
                    )
                )
            )
        if not self.profile_settings_item and self.get_setting("enable_enter_profile", True):
            self.profile_settings_item = self.add_menu_item(
                MenuItemData(
                    menu_type=MenuItemType.PROFILE_ACTION_MENU,
                    text=localizer.get_string("player"),
                    icon="player",
                    priority=1,
                    on_click=lambda ctx: run_on_ui_thread(
                        lambda: self.setup_player_ui()
                    )
                )
            )
        if not self.drawer_menu_item and self.get_setting("enable_enter_sidebar", True):
            self.drawer_menu_item = self.add_menu_item(
                MenuItemData(
                    menu_type=MenuItemType.DRAWER_MENU,
                    text=localizer.get_string("player"),
                    icon="player",
                    priority=1,
                    on_click=lambda ctx: run_on_ui_thread(
                        lambda: self.setup_player_ui()
                    )
                )
            )

    def remove_settings_menu_items(self):
        if self.chat_settings_item:
            self.remove_menu_item(self.chat_settings_item)
            self.chat_settings_item = None
        if self.profile_settings_item:
            self.remove_menu_item(self.profile_settings_item)
            self.profile_settings_item = None
        if self.drawer_menu_item:
            self.remove_menu_item(self.drawer_menu_item)
            self.drawer_menu_item = None

    def sync_settings_to_java(self):
        """Собирает настройки Python и отправляет их в Java класс"""
        if MusicPlayer is None:
            return

        # Создаем Java HashMap
        java_settings = HashMap()

        # Список ключей настроек (должны совпадать с теми, что в create_settings)
        keys = [
            "enable_feature_shuffle",
            "enable_feature_download",
            "enable_feature_share",
            "enable_feature_save",
            "enable_background_dominant"
        ]

        for key in keys:
            # self.get_setting(ключ, значение_по_умолчанию)
            # Важно: значение по умолчанию должно совпадать с логикой в Java
            py_val = self.get_setting(key, True)

            # Кладем в Map: Ключ (String) -> Значение (Boolean)
            java_settings.put(String(key), Boolean(py_val))

        try:
            # Вызываем Java метод setSettings
            MusicPlayer.setSettings(java_settings)
            self.log("Settings synced to Java successfully")
        except Exception as e:
            self.log(f"Error syncing settings: {e}")

    def setup_player_ui(self):
        self._link_lyrics_now()
        self.sync_settings_to_java()
        if MusicPlayer:
            MusicPlayer.startPlayerUI(get_last_fragment())
        else:
            self.log("MusicPlayer is None")

    def _link_lyrics_now(self):
        global MusicPlayer
        lyrics = PluginsController.getInstance().plugins.get("lyrics")

        if not self.get_setting("enable_lyrics", False) or not lyrics or not lyrics.isEnabled():
            if MusicPlayer: MusicPlayer.setLyricsClass(None)
            return

        for _, module in list(sys.modules.items()):
            try:
                if getattr(module, "__id__", "") == "lyrics":
                    controller = getattr(module, "LyricsController", None)
                    if controller:
                        loader = controller.getClass().getClassLoader()
                        target_class = loader.loadClass("com.pessdes.lyrics.ui.LyricsActivity")
                        if MusicPlayer:
                            MusicPlayer.setLyricsClass(target_class)
                        return
            except:
                continue


# ============ Hooks ============

class AudioPlayerAlertHook(MethodHook):
    def __init__(self, plugin):
        self.plugin = plugin

    def after_hooked_method(self, param):
        if self.plugin.get_setting("enable_subitem", True):
            optionsButton = get_private_field(param.thisObject, "optionsButton")
            optionsButton.addSubItem(SHOW_PLAYER_ITEM_ID, get_icon_id("player"), localizer.get_string("player"))
            optionsButton.setSubItemShown(SHOW_PLAYER_ITEM_ID, is_music())


class UpdateHook(MethodHook):
    def __init__(self, plugin):
        self.plugin = plugin

    def after_hooked_method(self, param):
        if self.plugin.get_setting("enable_subitem", True):
            optionsButton = get_private_field(param.thisObject, "optionsButton")
            optionsButton.setSubItemShown(SHOW_PLAYER_ITEM_ID, is_music())


class SubItemClickHook(MethodHook):
    def __init__(self, plugin):
        self.plugin = plugin

    def before_hooked_method(self, param):
        if param.args[0] == SHOW_PLAYER_ITEM_ID and self.plugin.get_setting("enable_subitem", True):
            param.setResult(None)
            self.plugin.setup_player_ui()
            param.thisObject.dismiss()


class InterceptStandardPlayerHook(MethodHook):
    def __init__(self, plugin: BasePlugin):
        self.plugin = plugin

    def before_hooked_method(self, param):
        dialog = param.args[0]
        if isinstance(dialog, AudioPlayerAlert) and self.plugin.get_setting("enable_audioplayer", False):
            param.setResult(None)
            self.plugin.setup_player_ui()