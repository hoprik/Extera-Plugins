"""
Автор - @hoprik
Дизайн - @canickk
Идея с dex и хуки - @PESSDES_Plugins
Сурсы Плеера - https://github.com/hoprik/Extera-Plugins/tree/fullscreen_dex
"""

import base64
import hashlib
import sys
import weakref
from android.app import Dialog
from java.lang import Boolean, Integer, String
from java.nio import ByteBuffer
from java.util import HashMap
from dalvik.system import InMemoryDexClassLoader
from org.telegram.messenger.browser import Browser

from base_plugin import BasePlugin, MenuItemData, MenuItemType, MethodHook, HookFilter, hook_filters
from client_utils import get_last_fragment, run_on_queue
from android_utils import run_on_ui_thread
from hook_utils import find_class, get_private_field

from com.exteragram.messenger.plugins import PluginsController
from org.telegram.messenger import MediaController, LocaleController, ApplicationLoader, AndroidUtilities
from org.telegram.ui.ActionBar import ActionBarMenuItem, BaseFragment, Theme
from org.telegram.ui.Components import AudioPlayerAlert, LayoutHelper, UItem
from android.widget import FrameLayout, TextView
from android.view import Gravity
from android.util import TypedValue
from ui.settings import Header, Switch, Text, Input

import hashlib
import sys

# ============ Meta ============
__id__ = "fullscreen_music_player"
__name__ = "Music Player"
__description__ = "Full screen music player. Если вы обновляете плагин, перезапустите Telegram."
__author__ = "@hoprik"
__version__ = "1.2.1"
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
            "settings_proxy": "Прокси",
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
            "settings_proxy": "Proxy",
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


# ============ Header Hook Class ============
class MusicPlayerSettingsHeaderHook:
    def __init__(self, plugin):
        self._plugin_ref = weakref.ref(plugin)

    @hook_filters(HookFilter.Condition("param.thisObject != null"), HookFilter.ArgumentNotNull(0))
    def after_hooked_method(self, param):
        pass
        try:
            global MusicPlayer
            activity = param.thisObject
            items = param.args[0]
            if not items or items.size() == 0:
                return

            plugin_obj = get_private_field(activity, "plugin")
            if not plugin_obj or str(plugin_obj.getId()) != __id__:
                return

            if get_private_field(activity, "createSubFragmentCallback") is not None:
                return

            plugin = self._plugin_ref()
            if not plugin:
                return

            if MusicPlayer:
                header = MusicPlayer.createHeader(get_last_fragment())
            else:
                header = plugin._create_settings_header(activity.getContext())
            print(f"Header created: {header is not None}")
            if header:
                item = UItem.asCustom(header)
                items.add(0, item)
                items.add(1, UItem.asShadow())
        except Exception as e:
            pass


# ============ Plugin Class ============
class PlayerPlugin(BasePlugin):

    def __init__(self):
        super().__init__()
        self.chat_settings_item = None
        self.profile_settings_item = None
        self.drawer_menu_item = None
        self.hook_settings_header_ref = None

    def on_plugin_load(self):
        self.add_settings_menu_items()

        self.hook_method(AudioPlayerAlert.getClass().getDeclaredConstructors()[0], AudioPlayerAlertHook(self))
        self.hook_method(AudioPlayerAlert.getClass().getDeclaredMethod("updateTitle", Boolean.TYPE), UpdateHook(self))
        self.hook_method(AudioPlayerAlert.getClass().getDeclaredMethod("onSubItemClick", Integer.TYPE),
                         SubItemClickHook(self))
        # self.hook_method(BaseFragment.getClass().getDeclaredMethod("showDialog", Dialog),
        #                  InterceptStandardPlayerHook(self))

        self._setup_settings_header_hook()
        run_on_queue(self.dex_load)

    def on_plugin_unload(self):
        self.remove_settings_menu_items()
        if MusicPlayer:
            MusicPlayer.destroy()
        if self.hook_settings_header_ref:
            self.unhook_method(self.hook_settings_header_ref)
            self.hook_settings_header_ref = None

    # ---------- Header creation ----------
    def _setup_settings_header_hook(self):
        try:
            PSA = find_class("com.exteragram.messenger.plugins.ui.PluginSettingsActivity")
            if not PSA:
                return
            method = PSA.getClass().getDeclaredMethod("fillItems",
                                                      find_class("java.util.ArrayList"),
                                                      find_class("org.telegram.ui.Components.UniversalAdapter"))
            method.setAccessible(True)
            self.hook_settings_header_ref = self.hook_method(method, MusicPlayerSettingsHeaderHook(self))
        except Exception as e:
            self.log(f"Failed to setup header hook: {e}")

    def _create_settings_header(self, context):
        try:
            container = FrameLayout(context)

            title = TextView(context)
            title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            title.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM))
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22)
            title.setText(f"Music Player {__version__}")
            title.setSingleLine(True)
            title.setGravity(Gravity.CENTER)
            container.addView(title, LayoutHelper.createFrame(-2, -2, Gravity.CENTER, 0, 20, 0, 0))

            subtitle = TextView(context)
            subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14)
            subtitle.setText("Full‑screen music player with extra features")
            subtitle.setGravity(Gravity.CENTER)
            container.addView(subtitle, LayoutHelper.createFrame(-2, -2, Gravity.CENTER, 0, 55, 0, 20))

            return container
        except Exception as e:
            self.log(f"Error creating header: {e}")
            return None

    # ---------- DEX loading ----------
    def dex_load(self):
        global MusicPlayer
        try:
            dex_bytes = base64.b64decode(dex_data)
        except Exception as e:
            self.log(f"Failed to decode dex_data: {e}")
            return

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

        try:
            clazz = find_class(PLAYER_CLASS_NAME).getClass()
            self.log(f"Found existing class: {PLAYER_CLASS_NAME}")
        except:
            self.log(f"Class not found, loading DEX...")
            loader = InMemoryDexClassLoader(
                ByteBuffer.wrap(dex_bytes),
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

    # ---------- Settings UI ----------
    def create_settings(self):
        return [
            Text(
                text=localizer.get_string("settings_elements"),
                icon="msg_arrow_forward",
                create_sub_fragment=self.sub_settings_element,
                link_alias="sub_settings_element",
            ),
            Text(
                text=localizer.get_string("settings_enter"),
                icon="msg_arrow_forward",
                create_sub_fragment=self.sub_settings_enter,
                link_alias="sub_settings_enter",
            ),
            Text(
                text=localizer.get_string("settings_proxy"),
                icon="msg_arrow_forward",
                create_sub_fragment=self.sub_settings_proxy,
                link_alias="sub_settings_enter",
            ),
            Text(
                text=localizer.get_string("settings_addons"),
                icon="msg_arrow_forward",
                create_sub_fragment=self.sub_settings_extensions,
                link_alias="sub_settings_enter",
            )
        ]

    def sub_settings_element(self):
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
                   icon="msg_settings")
        ]

    def sub_settings_enter(self):
        return [
            Header(localizer.get_string("settings_enter")),
            Switch(key="enable_audioplayer", text=localizer.get_string("settings_enable_enter_audioplayer"),
                   default=True, icon="msg_settings"),
            Switch(key="enable_subitem", text=localizer.get_string("settings_enable_enter_subitem"),
                   subtext=localizer.get_string("settings_enable_enter_subitem"), default=True, icon="msg_settings"),
            Switch(key="enable_enter_profile", text=localizer.get_string("settings_enable_enter_profile"),
                   subtext=localizer.get_string("settings_enable_enter_profile"), default=True, icon="msg_settings",
                   on_change=self._reload_menu_items),
            Switch(key="enable_enter_chat", text=localizer.get_string("settings_enable_enter_chat"),
                   subtext=localizer.get_string("settings_enable_enter_chat"), default=True, icon="msg_settings",
                   on_change=self._reload_menu_items),
            Switch(key="enable_enter_sidebar", text=localizer.get_string("settings_enable_enter_sidebar"),
                   subtext=localizer.get_string("settings_enable_enter_sidebar"), default=True, icon="msg_settings",
                   on_change=self._reload_menu_items)
        ]

    def sub_settings_extensions(self):
        return [
            Header(localizer.get_string("settings_addons")),
            Switch(key="enable_lyrics", text=localizer.get_string("settings_enable_addon_lyrics"),
                   subtext=localizer.get_string("settings_enable_addon_lyrics"), default=False, icon="msg_settings"),
            Text(text="Скачать плагин lyrics", on_click=self._open_plugin_lyrics)
        ]

    def sub_settings_proxy(self):
        settings = [
            Header(localizer.get_string("settings_proxy")),
        ]

        for i in range(1, 5):
            settings.append(Input(
                key=f"proxy_settings_{i}",
                text=f"Proxy {i}",
                subtext="host:port",
                default="",
                icon="msg_settings"
            ))

        return settings

    def _open_plugin_lyrics(self, view):
        if MusicPlayer:
            MusicPlayer.openBrowser(get_last_fragment())

    def _reload_menu_items(self, *args):
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
        if MusicPlayer is None:
            return
        java_settings = HashMap()
        keys = [
            "enable_feature_shuffle",
            "enable_feature_download",
            "enable_feature_share",
            "enable_feature_save",
            "enable_background_dominant"
        ]
        for key in keys:
            py_val = self.get_setting(key, True)
            java_settings.put(String(key), Boolean(py_val))
        try:
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
            if MusicPlayer:
                MusicPlayer.setLyricsClass(None)
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