"""
Автор - @hoprik
Дизайн - @canickk
"""

import base64
from java.lang import Boolean, String, Integer, Long
from java.nio import ByteBuffer
from java.util import HashMap
from dalvik.system import InMemoryDexClassLoader

from base_plugin import BasePlugin, MenuItemData, MenuItemType, MethodHook
from client_utils import get_last_fragment, run_on_queue
from android_utils import run_on_ui_thread
from hook_utils import find_class
from org.telegram.messenger import MediaController, LocaleController, ApplicationLoader
from ui.settings import Input, Switch
from com.exteragram.messenger.pillstack.core import PillStackConfig


# ============ Meta ============
__id__ = "pill_stats_fm"
__name__ = "Pill Stats.FM"
__description__ = "Виджет которые показывает что вы сейчас слушаете на stats.fm"
__author__ = "@hoprik"
__version__ = "1.0"
__icon__ = "hopriks_extera/0"
__min_version__ = "12.5.1"

# ============ Global Vars ============
PLAYER_CLASS_NAME = "ru.hoprik.pillmusic.PillMusic"
PillStatsFm = None
dex_data = # DEX_DATA_HERE #

# ============ Localization ============
class LocalizationManager:
    strings = {
        "ru": {
            "plugin_settings": "Настройки плагина",
            "turn_on_player": "Включите плеер",
            "stats_fm_nickname": "stats.fm никнейм",
            "cooldown": "Задержка между обновлениями (сек)",
            "enable_pill": "Включить виджет"
        },
        "en": {
            "plugin_settings": "Plugin settings",
            "turn_on_player": "Turn on the player",
            "stats_fm_nickname": "stats.fm username",
            "cooldown": "Cooldown between updates (sec)",
            "enable_pill": "Enable the widget"
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


class PillstackOnClickHook(MethodHook):
    def __init__(self, plugin):
        self.plugin = plugin

    def before_hooked_method(self, param):
        # Scaffold: add your custom logic here.
        try:
            args = param.args
            uItem = args[0]
            i2 = Integer.valueOf(uItem.id)
            self.plugin.log(f"Clicked item ID: {i2}")
            if i2 == 71369790:
                self.plugin.log("PillStack toggle clicked")
                self.plugin.log(PillStackConfig.activePills.contains(uItem.id))
                self.plugin.log(self.contains(PillStackConfig.activePills, i2))
                self.plugin.log(PillStackConfig.activePills.toString())
                if self.contains(PillStackConfig.activePills, i2):
                    self.plugin.set_setting("enable_pill", False)
                else:
                    self.plugin.set_setting("enable_pill", True)

        except Exception as e:
            self.plugin.log(f"[Hook] before onClick error: {e}")

    def contains(self, collection, target):
        for i in range(collection.size()):
            if collection.get(i) == target:
                return True
        return False


class OpenPluginSettingsHook(MethodHook):
    def __init__(self, plugin):
        self.plugin = plugin

    def before_hooked_method(self, param):
        try:
            self.plugin.log(f"[Hook] before openPluginSettings: this={param.thisObject}")
            PillStatsFm.openSettings(get_last_fragment() )
        except Exception as e:
            self.plugin.log(f"[Hook] before openPluginSettings error: {e}")


# ============ Plugin Class ============
class PillStatsFMPlugin(BasePlugin):

    def __init__(self):
        super().__init__()
        self._on_click_unhooks = []
        self._open_plugin_settings_unhooks = []

    def _hook_pillstack_onclick(self):
        try:
            target_class = find_class("com.exteragram.messenger.pillstack.ui.PillStackPreferencesActivity")
            if not target_class:
                self.log("onClick hook: class com.exteragram.messenger.pillstack.ui not found")
                return

            handler = PillstackOnClickHook(self)
            unhooks = self.hook_all_methods(target_class, "onClick", handler)
            if unhooks:
                self._on_click_unhooks = unhooks
                self.log(f"onClick hook: attached {len(unhooks)} method(s)")
            else:
                self.log("onClick hook: no methods were hooked")
        except Exception as e:
            self.log(f"onClick hook setup failed: {e}")

    def _hook_open_plugin_settings(self):
        try:
            target_class = find_class("com.exteragram.messenger.plugins.PythonPluginsEngine")
            if not target_class:
                self.log("openPluginSettings hook: class com.exteragram.messenger.plugins.PythonPluginsEngine not found")
                return

            handler = OpenPluginSettingsHook(self)
            unhooks = self.hook_all_methods(target_class, "openPluginSettings", handler)
            if unhooks:
                self._open_plugin_settings_unhooks = unhooks
                self.log(f"openPluginSettings hook: attached {len(unhooks)} method(s)")
            else:
                self.log("openPluginSettings hook: no methods were hooked")
        except Exception as e:
            self.log(f"openPluginSettings hook setup failed: {e}")

    def on_plugin_load(self):
        self._hook_pillstack_onclick()
        self._hook_open_plugin_settings()
        self.add_settings_menu_items()
        run_on_queue(self.dex_load)

    def on_plugin_unload(self):
        for unhook in getattr(self, "_on_click_unhooks", []):
            try:
                self.unhook_method(unhook)
            except Exception:
                pass
        self._on_click_unhooks = []

        for unhook in getattr(self, "_open_plugin_settings_unhooks", []):
            try:
                self.unhook_method(unhook)
            except Exception:
                pass
        self._open_plugin_settings_unhooks = []

        self.remove_settings_menu_items()
        run_on_ui_thread(self._call_unregister())

    def dex_load(self):
        global PillStatsFm
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
            PillStatsFm = clazz.getDeclaredMethod("getInstance").invoke(None)
            self.log("PillStatsFm instance created")
            try:
                java_translations = python_dict_to_java_map(localizer.strings)
                PillStatsFm.setLocalizations(java_translations)
                self.log("Translations passed successfully")
            except Exception as e:
                self.log(f"Warning: Could not pass translations: {e}")

            # Вызов register в UI потоке
            run_on_ui_thread(lambda: self._call_register())
            self.log("PillStatsFm.register() scheduled on UI thread")
        except Exception as e:
            self.log(f"FATAL ERROR getting instance: {e}")

    def _call_register(self):
        try:
            PillStatsFm.register()
            self.log("register() executed successfully")
        except Exception as e:
            self.log(f"register() threw exception: {e}")

    def _call_unregister(self):
        try:
            PillStatsFm.unregister()
            self.log("unregister() executed successfully")
        except Exception as e:
            self.log(f"unregister() threw exception: {e}")

    def create_settings(self):
        return [
            Input(key="accounts", text=localizer.get_string(key="stats_fm_nickname"), default="", icon="msg_text"),
            Input(key="cooldown", text=localizer.get_string(key="cooldown"), default="10", icon="msg_text", on_change=self._check_is_int),
            Switch(
                key="enable_pill",
                text=localizer.get_string(key="enable_pill"),
                default=False,
                icon="msg_settings"
            ),
        ]

    def _check_is_int(self, new_value: str):
        cooldown = str(new_value).strip()

        if cooldown.isdigit():
            self.set_setting("cooldown", cooldown)
            return

        # on_change is a change notification callback; keep storage sane on invalid input.
        self.set_setting("cooldown", "10", reload_settings=True)

    def _reload_menu_items(self, *args):
        self.remove_settings_menu_items()
        self.add_settings_menu_items()

    def add_settings_menu_items(self):
        pass

    def remove_settings_menu_items(self):
        pass

    def sync_settings_to_java(self):
        """Собирает настройки Python и отправляет их в Java класс"""
        if PillStatsFm is None:
            return

        java_settings = HashMap()
        keys = [
            "username",
        ]

        for key in keys:
            py_val = self.get_setting(key, True)
            java_settings.put(String(key), Boolean(py_val))

        try:
            PillStatsFm.setSettings(java_settings)
            self.log("Settings synced to Java successfully")
        except Exception as e:
            self.log(f"Error syncing settings: {e}")