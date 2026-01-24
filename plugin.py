"""
Автор - @hoprik
Дизайн - @canickk
Идея с dex и хуки - @PESSDES_Plugins
Сурсы Плеера - https://github.com/hoprik/Extera-Plugins/tree/fullscreen_dex
"""

import base64
import sys
from android.app import Dialog
from java.lang import Boolean, Integer, String
from java.nio import ByteBuffer
from java.util import HashMap  # <--- Импортируем Java HashMap
from dalvik.system import InMemoryDexClassLoader

from base_plugin import BasePlugin, MenuItemData, MenuItemType, MethodHook
from client_utils import get_last_fragment, run_on_queue
from android_utils import run_on_ui_thread
from hook_utils import find_class, get_private_field

from com.exteragram.messenger.plugins import PluginsController
from org.telegram.messenger import MediaController, LocaleController, ApplicationLoader
from org.telegram.ui.ActionBar import ActionBarMenuItem, BaseFragment
from org.telegram.ui.Components import AudioPlayerAlert
from ui.settings import Header, Switch

# ============ Meta ============
__id__ = "fullscreen_music_player"
__name__ = "Music Player"
__description__ = "Full screen music player. Если вы обновляете плагин, перезапустите Telegram."
__author__ = "@hoprik"
__version__ = "1.1"
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
            "settings_enable_background_dominant": "Доминантные цвета",
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
            "settings_enable_background_dominant": "Dominant colors",
            "settings_enable_enter_audioplayer": "Replace mini-player",
            "settings_enable_enter_subitem": "Add element to mini-player",
            "settings_enable_enter_profile": "Add element to profile",
            "settings_enable_enter_chat": "Add element to chat",
            "settings_enable_enter_sidebar": "Add element to sidebar (Works only if Plugins are added to sidebar)",
            "settings_enable_addon_lyrics": "Enable lyrics plugin support (Requires plugin to be installed)"
        }1/
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
    def __init__(self):
        super().__init__()
        self.chat_settings_item = None
        self.profile_settings_item = None
        self.drawer_menu_item = None

    def on_plugin_load(self):
        self.add_settings_menu_items()

        self.hook_method(AudioPlayerAlert.getClass().getDeclaredConstructors()[0], AudioPlayerAlertHook(self))
        self.hook_method(AudioPlayerAlert.getClass().getDeclaredMethod("updateTitle", Boolean.TYPE), UpdateHook(self))
        self.hook_method(AudioPlayerAlert.getClass().getDeclaredMethod("onSubItemClick", Integer.TYPE),
                         SubItemClickHook(self))
        self.hook_method(BaseFragment.getClass().getDeclaredMethod("showDialog", Dialog),
                         InterceptStandardPlayerHook(self))

        run_on_queue(self.dex_load)

    def on_plugin_unload(self):
        self.remove_settings_menu_items()

    def dex_load(self):
        global MusicPlayer
        try:
            clazz = find_class(PLAYER_CLASS_NAME).getClass()
            self.log(f"Found existing class: {PLAYER_CLASS_NAME}")
        except:
            self.log(f"Class not found, loading DEX...")
            dex_code = base64.b64decode(dex_data)
            loader = InMemoryDexClassLoader(
                ByteBuffer.wrap(dex_code),
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
            Header(localizer.get_string("settings_background")),
            Switch(key="enable_background_dominant", text=localizer.get_string("settings_enable_background_dominant"),
                   default=True, icon="msg_settings"),
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
        ]

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
