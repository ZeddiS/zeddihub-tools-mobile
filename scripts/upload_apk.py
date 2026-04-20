#!/usr/bin/env python3
"""
Upload a built APK to Google Drive at /<root>/<version>/<filename>.

Zero third-party dependencies. Uses Python 3 stdlib only.

Credentials are stored in <project>/tools/gdrive/config.json (git-ignored).
First run will walk the user through the OAuth setup once.
"""

from __future__ import annotations

import argparse
import http.server
import json
import mimetypes
import os
import pathlib
import re
import secrets
import socketserver
import sys
import threading
import time
import urllib.parse
import urllib.request
import webbrowser

ROOT = pathlib.Path(__file__).resolve().parent.parent
CONFIG_PATH = ROOT / "tools" / "gdrive" / "config.json"
SCOPE = "https://www.googleapis.com/auth/drive"
AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
TOKEN_URL = "https://oauth2.googleapis.com/token"
DRIVE_API = "https://www.googleapis.com/drive/v3"
UPLOAD_API = "https://www.googleapis.com/upload/drive/v3/files"
FOLDER_MIME = "application/vnd.google-apps.folder"
LOOPBACK_PORT = 53682


def _die(msg: str, code: int = 1) -> None:
    print(f"[upload_apk] ERROR: {msg}", file=sys.stderr)
    sys.exit(code)


def _log(msg: str) -> None:
    print(f"[upload_apk] {msg}")


def _read_version_name() -> str:
    gradle = ROOT / "app" / "build.gradle.kts"
    text = gradle.read_text(encoding="utf-8")
    match = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    if not match:
        _die("versionName not found in app/build.gradle.kts")
    return match.group(1)


def _load_config() -> dict:
    if not CONFIG_PATH.exists():
        return {}
    return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))


def _save_config(cfg: dict) -> None:
    CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
    CONFIG_PATH.write_text(json.dumps(cfg, indent=2), encoding="utf-8")
    try:
        os.chmod(CONFIG_PATH, 0o600)
    except OSError:
        pass


def _http_post_form(url: str, data: dict) -> dict:
    body = urllib.parse.urlencode(data).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _http_json(method: str, url: str, token: str, body: dict | None = None) -> dict:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Authorization": f"Bearer {token}"}
    if data is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=60) as resp:
        raw = resp.read().decode("utf-8")
        return json.loads(raw) if raw else {}


def _prompt_credentials(cfg: dict) -> dict:
    print()
    print("=== Jednorazove nastaveni Google Drive OAuth ===")
    print("1. V Google Cloud Console vytvor projekt (nebo pouzij stavajici).")
    print("2. Povol Google Drive API v knihovne 'APIs & Services'.")
    print("3. Vytvor OAuth 2.0 Client ID typu 'Desktop app'.")
    print("   (OAuth consent screen -> External / Testing; pridej sebe do Test users.)")
    print(f"4. Pridej do 'Authorized redirect URIs' hodnotu http://127.0.0.1:{LOOPBACK_PORT}/")
    print("5. Zkopiruj Client ID a Client Secret a vloz je nize.")
    print()
    client_id = input("Client ID: ").strip()
    client_secret = input("Client Secret: ").strip()
    if not client_id or not client_secret:
        _die("Client ID i Client Secret jsou povinne.")
    cfg["client_id"] = client_id
    cfg["client_secret"] = client_secret
    cfg.setdefault("root_folder", "ZeddiHub App")
    return cfg


class _OAuthHandler(http.server.BaseHTTPRequestHandler):
    server_state: dict = {}

    def log_message(self, *_args, **_kwargs) -> None:
        pass

    def do_GET(self) -> None:  # noqa: N802
        qs = urllib.parse.urlparse(self.path).query
        params = urllib.parse.parse_qs(qs)
        self.server_state["params"] = {k: v[0] for k, v in params.items()}
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(
            b"<!doctype html><meta charset=utf-8>"
            b"<body style='font-family:sans-serif;background:#0b0f14;color:#e6edf5;"
            b"display:flex;align-items:center;justify-content:center;height:100vh'>"
            b"<div><h2 style='color:#f99a1c'>ZeddiHub uploader</h2>"
            b"<p>Autorizace hotova. Muzes zavrit toto okno.</p></div></body>"
        )


def _run_oauth_flow(cfg: dict) -> dict:
    state = secrets.token_urlsafe(16)
    redirect_uri = f"http://127.0.0.1:{LOOPBACK_PORT}/"
    auth_params = {
        "client_id": cfg["client_id"],
        "redirect_uri": redirect_uri,
        "response_type": "code",
        "scope": SCOPE,
        "access_type": "offline",
        "prompt": "consent",
        "state": state,
    }
    auth_url = f"{AUTH_URL}?{urllib.parse.urlencode(auth_params)}"

    handler = _OAuthHandler
    handler.server_state = {}
    httpd = socketserver.TCPServer(("127.0.0.1", LOOPBACK_PORT), handler)
    thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    thread.start()

    _log("Otevirani prohlizece pro autorizaci Google Drive...")
    if not webbrowser.open(auth_url):
        print(f"Otevri rucne: {auth_url}")

    timeout = 300
    start = time.time()
    while "params" not in handler.server_state:
        if time.time() - start > timeout:
            httpd.shutdown()
            _die("OAuth autorizace nedokoncena do 5 minut.")
        time.sleep(0.3)

    httpd.shutdown()
    params = handler.server_state["params"]

    if params.get("state") != state:
        _die("OAuth state mismatch - mozny utok, akce zrusena.")
    if "error" in params:
        _die(f"OAuth chyba: {params['error']}")
    code = params.get("code")
    if not code:
        _die("Z OAuth redirectu chybi 'code'.")

    tokens = _http_post_form(
        TOKEN_URL,
        {
            "code": code,
            "client_id": cfg["client_id"],
            "client_secret": cfg["client_secret"],
            "redirect_uri": redirect_uri,
            "grant_type": "authorization_code",
        },
    )
    refresh = tokens.get("refresh_token")
    if not refresh:
        _die("Google nevratil refresh_token. Odstran prirazeni v https://myaccount.google.com/permissions a zkus znovu.")
    cfg["refresh_token"] = refresh
    return cfg


def _ensure_config() -> dict:
    cfg = _load_config()
    changed = False
    if not cfg.get("client_id") or not cfg.get("client_secret"):
        cfg = _prompt_credentials(cfg)
        changed = True
    if not cfg.get("refresh_token"):
        cfg = _run_oauth_flow(cfg)
        changed = True
    if changed:
        _save_config(cfg)
    return cfg


def _access_token(cfg: dict) -> str:
    resp = _http_post_form(
        TOKEN_URL,
        {
            "client_id": cfg["client_id"],
            "client_secret": cfg["client_secret"],
            "refresh_token": cfg["refresh_token"],
            "grant_type": "refresh_token",
        },
    )
    token = resp.get("access_token")
    if not token:
        _die(f"Refresh access_token selhal: {resp}")
    return token


def _find_folder(token: str, name: str, parent_id: str | None) -> str | None:
    escaped = name.replace("\\", "\\\\").replace("'", "\\'")
    q_parts = [
        f"name = '{escaped}'",
        f"mimeType = '{FOLDER_MIME}'",
        "trashed = false",
    ]
    if parent_id:
        q_parts.append(f"'{parent_id}' in parents")
    q = " and ".join(q_parts)
    url = f"{DRIVE_API}/files?q={urllib.parse.quote(q)}&fields=files(id,name)&pageSize=10"
    resp = _http_json("GET", url, token)
    files = resp.get("files") or []
    return files[0]["id"] if files else None


def _create_folder(token: str, name: str, parent_id: str | None) -> str:
    body: dict = {"name": name, "mimeType": FOLDER_MIME}
    if parent_id:
        body["parents"] = [parent_id]
    resp = _http_json(
        "POST", f"{DRIVE_API}/files?fields=id,name", token, body=body
    )
    return resp["id"]


def _ensure_folder(token: str, name: str, parent_id: str | None) -> str:
    existing = _find_folder(token, name, parent_id)
    if existing:
        return existing
    _log(f"Vytvarim slozku '{name}' na Drive")
    return _create_folder(token, name, parent_id)


def _find_file(token: str, name: str, parent_id: str) -> str | None:
    q = (
        f"name = '{name}' and '{parent_id}' in parents "
        f"and mimeType != '{FOLDER_MIME}' and trashed = false"
    )
    url = f"{DRIVE_API}/files?q={urllib.parse.quote(q)}&fields=files(id,name)&pageSize=5"
    resp = _http_json("GET", url, token)
    files = resp.get("files") or []
    return files[0]["id"] if files else None


def _upload_file(token: str, path: pathlib.Path, parent_id: str, name: str) -> str:
    existing_id = _find_file(token, name, parent_id)
    metadata: dict = {"name": name}
    if not existing_id:
        metadata["parents"] = [parent_id]
    mime = mimetypes.guess_type(str(path))[0] or "application/octet-stream"
    size = path.stat().st_size

    if existing_id:
        init_url = (
            f"{UPLOAD_API}/{existing_id}?uploadType=resumable&supportsAllDrives=true"
        )
        init_method = "PATCH"
    else:
        init_url = f"{UPLOAD_API}?uploadType=resumable&supportsAllDrives=true"
        init_method = "POST"

    init_req = urllib.request.Request(
        init_url,
        data=json.dumps(metadata).encode("utf-8"),
        method=init_method,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json; charset=UTF-8",
            "X-Upload-Content-Type": mime,
            "X-Upload-Content-Length": str(size),
        },
    )
    with urllib.request.urlopen(init_req, timeout=60) as resp:
        upload_url = resp.headers.get("Location")
    if not upload_url:
        _die("Nepovedlo se ziskat resumable upload URL.")

    _log(f"Uploading {path.name} ({size/1024/1024:.1f} MB)...")
    with path.open("rb") as f:
        data = f.read()
    put_req = urllib.request.Request(
        upload_url,
        data=data,
        method="PUT",
        headers={"Content-Type": mime, "Content-Length": str(size)},
    )
    with urllib.request.urlopen(put_req, timeout=600) as resp:
        raw = resp.read().decode("utf-8")
    result = json.loads(raw) if raw else {}
    file_id = result.get("id") or existing_id
    if not file_id:
        _die(f"Upload selhal: {result}")
    return file_id


def main() -> None:
    parser = argparse.ArgumentParser(description="Upload APK to Google Drive.")
    default_apk_dir = ROOT / "app" / "build" / "outputs" / "apk" / "debug"
    default_apk = None
    if default_apk_dir.exists():
        matches = sorted(default_apk_dir.glob("ZeddiHub-App-*.apk"))
        if matches:
            default_apk = matches[-1]
        else:
            fallback = default_apk_dir / "app-debug.apk"
            if fallback.exists():
                default_apk = fallback
    parser.add_argument(
        "--apk",
        default=str(default_apk) if default_apk else str(default_apk_dir / "app-debug.apk"),
        help="Cesta k APK (default: posledni ZeddiHub-App-*.apk).",
    )
    parser.add_argument(
        "--version",
        default=None,
        help="Cilova podslozka (default: versionName z build.gradle.kts).",
    )
    parser.add_argument(
        "--name",
        default=None,
        help="Jmeno souboru na Drive (default: stejne jako lokalni).",
    )
    args = parser.parse_args()

    apk_path = pathlib.Path(args.apk)
    if not apk_path.exists():
        _die(f"APK neexistuje: {apk_path}")
    version = args.version or _read_version_name()
    file_name = args.name or apk_path.name

    cfg = _ensure_config()
    token = _access_token(cfg)
    root_name = cfg.get("root_folder", "ZeddiHub App")

    _log(f"Cesta na Drive: /{root_name}/{version}/{file_name}")
    root_id = _ensure_folder(token, root_name, parent_id=None)
    version_id = _ensure_folder(token, version, parent_id=root_id)
    file_id = _upload_file(token, apk_path, version_id, file_name)

    view_url = f"https://drive.google.com/file/d/{file_id}/view"
    _log(f"Hotovo. Soubor: {view_url}")


if __name__ == "__main__":
    main()
