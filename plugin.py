"""
Автор - @hoprik
Дизайн - @canickk
"""

import base64
from java.lang import Boolean, String
from java.nio import ByteBuffer
from java.util import HashMap
from dalvik.system import InMemoryDexClassLoader

from base_plugin import BasePlugin, MenuItemData, MenuItemType
from client_utils import get_last_fragment, run_on_queue
from android_utils import run_on_ui_thread
from hook_utils import find_class
from org.telegram.messenger import MediaController, LocaleController, ApplicationLoader
from ui.settings import Input, Switch

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
MKSTATS_POW_SOLVE_SECONDS = int(os.getenv("MKSTATS_POW_SOLVE_SECONDS", "6"))

def generate_user_hash(device_id: str, plugin_id: str) -> str:
    payload = f"{device_id}:{plugin_id}:mkstats:v1"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()

def generate_device_fingerprint(device_id: str) -> str:
    payload = f"{device_id}:mkstats:device:v1"
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

def _pow_valid(challenge: str, nonce: str, difficulty: int) -> bool:
    if not challenge or not nonce or difficulty <= 0:
        return False
    prefix = "0" * max(1, int(difficulty))
    digest = hashlib.sha256(f"{challenge}:{nonce}".encode("utf-8")).hexdigest()
    return digest.startswith(prefix)

def _solve_pow(challenge: str, difficulty: int, max_seconds: int = MKSTATS_POW_SOLVE_SECONDS) -> str | None:
    difficulty = max(1, int(difficulty or 0))
    deadline = time.time() + max(1, int(max_seconds or 0))
    nonce = 0
    prefix = "0" * difficulty
    while time.time() < deadline:
        candidate = format(nonce, "x")
        digest = hashlib.sha256(f"{challenge}:{candidate}".encode("utf-8")).hexdigest()
        if digest.startswith(prefix):
            return candidate
        nonce += 1
    return None

class MkStatsCoreClient:
    def __init__(self, api_url: str, plugin_id: str, plugin_version: str, user_hash: str, device_fingerprint: str, client_version: str | None = None, client_name: str | None = None) -> None:
        self.api_base = _normalize_api_base(api_url)
        self.plugin_id = plugin_id
        self.plugin_version = plugin_version
        self.client_version = client_version
        self.client_name = client_name
        self.user_hash = user_hash
        self.device_fingerprint = device_fingerprint

    def handshake(self) -> dict:
        payload = {
            "plugin_id": self.plugin_id,
            "version": self.plugin_version,
            "client_name": self.client_name,
            "client_version": self.client_version,
            "user_hash": self.user_hash,
            "device_fingerprint": self.device_fingerprint,
        }
        response = _post_json(f"{self.api_base}/handshake", payload)
        token = (response or {}).get("install_token", "")
        if token:
            return response
        pow_required = bool((response or {}).get("pow_required"))
        pow_challenge = (response or {}).get("pow_challenge")
        if pow_required and pow_challenge:
            difficulty = int((response or {}).get("pow_difficulty") or 0)
            nonce = _solve_pow(pow_challenge, difficulty)
            if nonce and _pow_valid(pow_challenge, nonce, difficulty):
                payload["pow_challenge"] = pow_challenge
                payload["pow_nonce"] = nonce
                response = _post_json(f"{self.api_base}/handshake", payload)
        return response

    def send_ping(self, install_token: str, timestamp=None) -> dict:
        payload = {
            "plugin_id": self.plugin_id,
            "version": self.plugin_version,
            "client_name": self.client_name,
            "client_version": self.client_version,
            "user_hash": self.user_hash,
            "device_fingerprint": self.device_fingerprint,
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
            "device_fingerprint": self.device_fingerprint,
            "install_token": install_token,
            "event": event,
            "count": count,
            "timestamp": timestamp or int(time.time()),
        }
        return _post_json(f"{self.api_base}/event", payload)
# === mkStats: embed client end ===



# ============ Meta ============
__id__ = "pill_kstati"
__name__ = "Pill Кстати"
__description__ = "Виджет которые показывает информацию о кстати...."
__author__ = "@hoprik"
__version__ = "1.4.8.8"
__icon__ = "hopriks_extera/0"
__min_version__ = "12.5.1"

# ============ Global Vars ============
PLAYER_CLASS_NAME = "ru.hoprik.pillkstati.PillMusic"
PillStatsFm = None
dex_data = # DEX_DATA_HERE #

# ============ Localization ============
class LocalizationManager:
    strings = {
        "ru": {
            "plugin_settings": "Настройки плагина",
            "turn_on_player": "Включите плеер",
            "stats_fm_nickname": "Текст",
            "cooldown": "Задержка между обновлениями (сек)",
            "enable_pill": "Включить виджет"
        },
        "en": {
            "plugin_settings": "Plugin settings",
            "turn_on_player": "Turn on the player",
            "stats_fm_nickname": "Text",
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

# ============ Plugin Class ============
class PillStatsFMPlugin(BasePlugin):

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
            device_fingerprint = generate_device_fingerprint(device_id)
            client_name = self._mkstats_get_client_name()
            client_version = self._mkstats_get_client_version()
            self._mkstats_client = MkStatsCoreClient(MKSTATS_API_URL, __id__, __version__, user_hash, device_fingerprint, client_version, client_name)
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

    def on_plugin_load(self):

        # === mkStats: integration start ===
        self._mkstats_start()
        # === mkStats: integration end ===

        self.add_settings_menu_items()
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
            Input(key="username", text=localizer.get_string(key="stats_fm_nickname"), default="", icon="msg_text"),
            Switch(
                key="enable_pill",
                text=localizer.get_string(key="enable_pill"),
                default=True,
                icon="msg_settings"
            ),
        ]

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