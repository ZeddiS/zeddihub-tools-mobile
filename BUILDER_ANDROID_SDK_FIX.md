# Úkol: rozšířit detekci Android SDK v ZeddiHub App Builderu tak, aby uměl buildit projekty s vlastním bundle toolchainem

## 0. Kontext

Jsem uživatel ZeddiHub App Builderu. V repozitáři
`C:\Users\12voj\Documents\zeddihub_tools_mobile`
(Android/Kotlin + Jetpack Compose + Gradle) mám `zeddibuild.json` a chci ho
postavit přes builder GUI. Builder ale hlásí, že **Android SDK není nalezeno**,
přestože projekt má kompletní toolchain (JDK 17 + Android SDK + Gradle cache)
zabalený uvnitř sebe v adresáři `tools/`. Build z příkazové řádky (`build.bat`)
funguje perfektně, takže problém je výhradně v **detekci toolchainu v builderu**,
ne v samotném projektu.

Chci, abys rozšířil builder tak, aby tenhle scénář zvládl automaticky — bez
toho, aby uživatel musel nastavovat globální `ANDROID_HOME` / `JAVA_HOME`.

Pracovní adresář builderu:
`C:\Users\12voj\Documents\zeddihub-app-builder\`

---

## 1. Stav dnes — přesně co builder dělá

### 1.1 `android_env.py::check_android_sdk()` (řádky 49–68)

```python
def check_android_sdk() -> ToolStatus:
    home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT", "")
    if not home or not os.path.isdir(home):
        return ToolStatus(
            "ANDROID_HOME", False,
            hint="Nastav ANDROID_HOME na cestu k Android SDK "
                 "(obvykle %LOCALAPPDATA%\\Android\\Sdk).",
        )
    sdk_ok = (
        os.path.isdir(os.path.join(home, "platform-tools")) or
        os.path.isdir(os.path.join(home, "build-tools"))
    )
    return ToolStatus("ANDROID_HOME", available=sdk_ok, ...)
```

**Problém:** funkce čte **pouze globální env vars**. Ignoruje:
- `local.properties` v rootu projektu (standardní Android Studio konvence,
  Gradle ho čte jako zdroj `sdk.dir`)
- bundle toolchain uvnitř repa (`<project_root>/tools/android-sdk`)
- overrides z `zeddibuild.json`

### 1.2 `android_env.py::check_java()` (řádky 40–46)

Volá `shutil.which("java")`. Ignoruje bundle JDK v repu.

### 1.3 `android_build.py::plan_android()` (řádky 40–53)

```python
def plan_android(project_root, module="app", task="assembleDebug"):
    gw = _gradlew(project_root)
    steps = [BuildStep(cmd=gw + [f":{module}:{task}"], cwd=project_root, ...)]
    output = os.path.join(project_root, module, "build", "outputs",
                          "apk", "debug", f"{module}-debug.apk")
    return BuildPlan(steps, output)
```

**Dva problémy:**
1. `BuildStep` nenese žádné env vars → Gradle child proces zdědí prázdné
   `JAVA_HOME` / `ANDROID_HOME` → build selže, i když SDK detekce projde.
2. `expected_output` hádá název APK jako `{module}-debug.apk`, ale reálné
   projekty si přepisují `archivesBaseName` (v našem případě generuje
   `ZeddiHub-App-0.5.2.apk`, `app-debug.apk` je jen fallback).

---

## 2. Jak ten konkrétní projekt vypadá

### 2.1 Strom `zeddihub_tools_mobile/` (zkráceně)

```
zeddihub_tools_mobile/
├── zeddibuild.json          ← konfigurace, kterou rovnou přepíšeme
├── build.bat                ← jak to build ručně — REFERENČNÍ CHOVÁNÍ
├── build.sh                 ← to samé na Linuxu / Git Bash
├── local.properties         ← sdk.dir=…\tools\android-sdk (absolutní)
├── build.gradle.kts         ← root gradle
├── settings.gradle.kts
├── gradlew / gradlew.bat    ← Gradle wrapper (funguje)
├── app/
│   ├── build.gradle.kts     ← versionName = "0.5.2", archivesBaseName
│   └── build/outputs/apk/debug/ZeddiHub-App-0.5.2.apk   ← VÝSTUP
├── scripts/
│   └── upload_apk.py        ← post-build Google Drive upload (stdlib-only)
└── tools/                   ← SELF-CONTAINED TOOLCHAIN (~5 GB)
    ├── jdk17/               ← JDK (JAVA_HOME)
    │   └── bin/java.exe
    ├── android-sdk/         ← Android SDK (ANDROID_HOME)
    │   ├── platform-tools/
    │   ├── build-tools/
    │   └── platforms/
    └── gradle-cache/        ← Gradle user home (izoluje .gradle v %USERPROFILE%)
```

### 2.2 `build.bat` — to, co builder musí replikovat v child procesu

```bat
set PROJECT_DIR=%~dp0
set JAVA_HOME=%PROJECT_DIR%tools\jdk17
set ANDROID_HOME=%PROJECT_DIR%tools\android-sdk
set ANDROID_SDK_ROOT=%ANDROID_HOME%
set GRADLE_USER_HOME=%PROJECT_DIR%tools\gradle-cache
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%

call gradlew.bat :app:assembleDebug

REM APK nalezen přes glob "ZeddiHub-App-*.apk" v app/build/outputs/apk/debug/
REM Fallback: app-debug.apk
REM Pak volitelně: python scripts\upload_apk.py --apk "<path>"
```

### 2.3 `local.properties`

```
sdk.dir=C\:\\Users\\12voj\\Documents\\zeddihub_tools_mobile\\tools\\android-sdk
```

(Java properties escape: `\:` → `:`, `\\` → `\`.)

### 2.4 Aktuální `zeddibuild.json` v projektu

Momentálně popisuje *Python CLI nástroj* `scripts/upload_apk.py`, protože
stávající schema nezná Android pole. **To je špatně** — přepíšeme ho na
Android variantu, jakmile rozšíříš schema (viz §5).

---

## 3. Co chci implementovat (shrnutí)

Čtyři změny v `zeddihub-app-builder/`:

1. **`android_env.py`** — víceúrovňová detekce SDK + JDK s prioritním stromem.
2. **`zeddibuild.schema.json`** — nová sekce `"android"` s podpoli pro toolchain paths, module, task, APK glob a post-build hook.
3. **`android_build.py`** — `BuildStep.env`, injekce resolvnutých cest do Gradle child procesu, glob pro APK output, volitelný post-build step.
4. **`build_gui.py` UX** — hint „bundle toolchain detected from `tools/`" místo generického errora, když detekce proběhla přes bundle cestu.

Detaily v §4–§7.

---

## 4. `android_env.py` — nová detekce

### 4.1 Signatura

Změň `check_android_sdk()` tak, aby brala `project_root` a volitelně `spec`
(načtený `zeddibuild.json`):

```python
def check_android_sdk(project_root: str = "", spec: dict | None = None) -> ToolStatus:
    """Hledá Android SDK v tomto pořadí (vrátí první, který projde sanity checkem
    "existuje platform-tools NEBO build-tools"):

    1. spec["android"]["sdk_dir"]         — relativní k project_root
    2. <project_root>/local.properties    — klíč sdk.dir (Android Studio konvence)
    3. <project_root>/tools/android-sdk   — bundle toolchain (heuristika)
    4. $ANDROID_HOME / $ANDROID_SDK_ROOT  — globální env
    5. %LOCALAPPDATA%\Android\Sdk (Windows)
       ~/Library/Android/sdk      (macOS)
       ~/Android/Sdk              (Linux)
    """
```

### 4.2 `ToolStatus` rozšíření

Přidej pole `source: str` s hodnotami:
`"spec"`, `"local.properties"`, `"bundle"`, `"env"`, `"default"`, `""` (nenalezeno).

UI to musí ukázat uživateli, aby v diagnostice jasně viděl, odkud builder SDK
vzal. Nejčastější debug scénář bude: „vybral si globální env místo bundle v repu
— proč?"

### 4.3 Parser `local.properties`

Java properties formát — pozor na escape:
- `\:` → `:`
- `\\` → `\`
- `\=` → `=`
- zahoď prázdné řádky a komentáře `#` / `!`

Příklad ze skutečnosti:
```
sdk.dir=C\:\\Users\\12voj\\Documents\\zeddihub_tools_mobile\\tools\\android-sdk
```
→ `C:\Users\12voj\Documents\zeddihub_tools_mobile\tools\android-sdk`

Nepoužívej `configparser` (ten neumí Java escapes). Napiš ruční mini-parser —
stačí `str.replace` řetězení v bezpečném pořadí (nejdřív `\\` na sentinel, pak
ostatní, pak sentinel zpět).

### 4.4 `check_java()` — stejná strategie

```python
def check_java(project_root: str = "", spec: dict | None = None) -> ToolStatus:
    """Priorita:
    1. spec["android"]["java_home"]   → <that>/bin/java(.exe)
    2. <project_root>/tools/jdk17/bin/java(.exe)   — bundle
    3. $JAVA_HOME/bin/java
    4. shutil.which("java")
    """
```

Sanity check: soubor `java` / `java.exe` existuje **a** `java -version` vrátí
returncode 0 (s child env `JAVA_HOME` nastaveným na kandidátní JDK, ať
netestuješ globální Javu).

---

## 5. `zeddibuild.schema.json` — nová sekce `android`

Přidej **volitelnou** sekci `"android"` do root objektu schématu:

```json
{
  "android": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "module":            { "type": "string", "default": "app" },
      "task":              { "type": "string", "default": "assembleDebug" },
      "sdk_dir":           { "type": "string" },
      "java_home":         { "type": "string" },
      "gradle_user_home":  { "type": "string" },
      "apk_name_pattern":  { "type": "string", "default": "{module}-debug.apk" },
      "apk_output_dir":    { "type": "string",
                             "default": "{module}/build/outputs/apk/debug" },
      "post_build_script": { "type": "string" },
      "post_build_args":   { "type": "array", "items": { "type": "string" } }
    }
  }
}
```

**Všechny cesty v sekci `android` jsou relativní k `project_root`.** Resolver
je převede na absolutní před předáním do build planu.

**Template placeholders** v `apk_name_pattern` a `post_build_args`:
- `{module}` → hodnota z `android.module`
- `{version}` → přečteno z `app/build.gradle.kts` přes regex `versionName\s*=\s*"([^"]+)"`
- `{apk_path}` → absolutní cesta nalezeného APK (nahrazuje se ve `post_build_args`, ne v `apk_name_pattern`)

Zároveň doplň ve schématu chybějící `project_type`:

```json
{
  "project_type": {
    "type": "string",
    "enum": ["python", "android", "flutter", "rn"],
    "description": "Explicitní typ; pokud chybí, detekuje se z obsahu projektu."
  }
}
```

Pole se už dnes čte přes `project_type.read_type_from_spec()`, ale chybí
ve schematu — validátor ho hlásí jako neznámé.

---

## 6. `android_build.py` — env injection + glob APK + post-build

### 6.1 Rozšíř `BuildStep`

```python
@dataclass
class BuildStep:
    cmd: list[str]
    label: str
    cwd: str
    env: dict[str, str] | None = None   # ← NOVÉ
```

`build_gui._run_cmd` wrapper musí `env` předat do `subprocess.Popen(env=...)`.
Pokud `env is None`, zachová se stávající chování (dědí os.environ).

### 6.2 Nový `plan_android()`

```python
def plan_android(project_root: str, spec: dict | None = None,
                 module: str | None = None, task: str | None = None) -> BuildPlan:
    android_cfg = (spec or {}).get("android", {}) or {}
    module = module or android_cfg.get("module", "app")
    task = task or android_cfg.get("task", "assembleDebug")

    sdk_status = android_env.check_android_sdk(project_root, spec)
    jdk_status = android_env.check_java(project_root, spec)
    if not sdk_status.available:
        raise BuildConfigError(
            f"Android SDK nenalezeno. Zkusil jsem: spec, local.properties, "
            f"bundle tools/android-sdk, $ANDROID_HOME, OS default. "
            f"Hint: {sdk_status.hint}"
        )
    if not jdk_status.available:
        raise BuildConfigError(f"JDK nenalezeno. Hint: {jdk_status.hint}")

    # Sestav env pro child proces: zděd current, pak overlay toolchain
    env = os.environ.copy()
    env["JAVA_HOME"] = jdk_status.path
    env["ANDROID_HOME"] = sdk_status.path
    env["ANDROID_SDK_ROOT"] = sdk_status.path
    gradle_cache = _resolve_gradle_cache(project_root, android_cfg)
    if gradle_cache:
        env["GRADLE_USER_HOME"] = gradle_cache
    # Prepend tool dirs na PATH
    bins = [os.path.join(jdk_status.path, "bin"),
            os.path.join(sdk_status.path, "platform-tools")]
    env["PATH"] = os.pathsep.join(bins + [env.get("PATH", "")])

    gw = _gradlew(project_root)
    steps = [BuildStep(cmd=gw + [f":{module}:{task}"],
                       label=f"gradle {module}:{task}",
                       cwd=project_root, env=env)]

    # APK output: nejdřív zkusí glob pattern, fallback na {module}-debug.apk
    apk_output_dir = os.path.join(
        project_root,
        android_cfg.get("apk_output_dir",
                        f"{module}/build/outputs/apk/debug")
    )
    pattern = android_cfg.get("apk_name_pattern", f"{module}-debug.apk")
    output = _resolve_apk_path(apk_output_dir, pattern, module, project_root)

    # Volitelný post-build krok
    post_script = android_cfg.get("post_build_script")
    if post_script:
        steps.append(_make_post_build_step(
            project_root, post_script,
            android_cfg.get("post_build_args", []),
            output, env,
        ))

    return BuildPlan(steps, output)
```

### 6.3 `_resolve_apk_path()`

1. Expanduj `{version}` placeholder: přečti `app/build.gradle.kts`,
   regex `versionName\s*=\s*"([^"]+)"`.
2. Zkus `glob.glob(os.path.join(apk_output_dir, pattern))`.
3. Pokud glob vrátí výsledek, vezmi `max(paths, key=os.path.getmtime)` (nejnovější).
4. Pokud glob vrátí 0 výsledků, vrať literal `{apk_output_dir}/{module}-debug.apk`
   (fallback; soubor třeba ještě neexistuje, resolvuje se po buildu).

**Důležité:** tato funkce se volá **po build stepu**, ne před ním. Buď změň
`BuildPlan.expected_output` na `Callable[[], str]` (lazy), nebo nech post-step,
ať si APK najde sám před uploadem. Druhá varianta je čistější.

### 6.4 `_make_post_build_step()`

```python
def _make_post_build_step(project_root, script, args, apk_path, env) -> BuildStep:
    py = _resolve_python()   # sys.executable pokud frozen, jinak shutil.which
    resolved_args = [
        str(a).replace("{apk_path}", apk_path)
               .replace("{project_root}", project_root)
        for a in args
    ]
    return BuildStep(
        cmd=[py, os.path.join(project_root, script)] + resolved_args,
        label=f"post-build: {os.path.basename(script)}",
        cwd=project_root,
        env=env,
    )
```

**`_resolve_python()`:** když builder běží jako PyInstaller exe (frozen), nelze
použít `sys.executable` (ukazuje na sebe). Použij `shutil.which("python")` →
`shutil.which("python3")` → varování.

---

## 7. UX v `build_gui.py`

### 7.1 Diagnostický panel před buildem

Dnes se zobrazuje jen „Android SDK: ❌ nenalezeno". Změň na multi-řádek:

```
🔧 Toolchain:
   JDK      ✓  17.0.9    (bundle: tools/jdk17)
   SDK      ✓  Android SDK (bundle: tools/android-sdk)
   Gradle   ✓  wrapper v projektu (gradlew.bat)
   Cache    ✓  tools/gradle-cache (izolováno od $HOME)
```

Když něco chybí:

```
   SDK      ✗  nenalezeno
              Zkusil jsem:
                1. zeddibuild.json → android.sdk_dir  (nedefinováno)
                2. local.properties                   (sdk.dir chybí)
                3. tools/android-sdk                  (neexistuje)
                4. $ANDROID_HOME                      (prázdné)
                5. %LOCALAPPDATA%\Android\Sdk         (neexistuje)
              Hint: nainstaluj přes Android Studio nebo
              přidej "android": { "sdk_dir": "…" } do zeddibuild.json.
```

Tenhle trail je **největší value-add** tohoto ticketu — uživatel musí vidět,
proč to padlo, ne generický error.

### 7.2 Přepínače v UI

Pokud builder má v UI formulář pro edit `zeddibuild.json`, přidej pole
pro Android sekci:
- Module (default `app`)
- Task (dropdown: `assembleDebug`, `assembleRelease`, `bundleRelease`, custom)
- SDK override (file picker, volitelné)
- JDK override (file picker, volitelné)
- Post-build script (file picker, volitelné)

---

## 8. `zeddibuild.json`, který budu chtít po dokončení v projektu použít

Po dokončení mám v `zeddihub_tools_mobile/zeddibuild.json` nahradit aktuální
Python-flavored obsah následujícím. Má tedy dávat smysl proti novému schematu:

```json
{
  "$schema": "https://zeddihub.cz/zeddibuild.schema.json",
  "project_type": "android",
  "name": "ZeddiHubMobile",
  "display_name": "ZeddiHub Mobile",
  "version": "0.5.2",
  "description": "ZeddiHub Tools pro Android — Kotlin + Jetpack Compose, sjednocená auth s desktopem (/api/auth/*), Wi-Fi map, speedtest, a další nástroje.",
  "author": "ZeddiHub",
  "entry_point": "app/src/main/java/com/zeddihub/mobile/MainActivity.kt",
  "output_name": "ZeddiHub-App",
  "icon": "app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp",
  "python_min": "3.10",
  "android": {
    "module": "app",
    "task": "assembleDebug",
    "sdk_dir": "tools/android-sdk",
    "java_home": "tools/jdk17",
    "gradle_user_home": "tools/gradle-cache",
    "apk_name_pattern": "ZeddiHub-App-{version}.apk",
    "apk_output_dir": "app/build/outputs/apk/debug",
    "post_build_script": "scripts/upload_apk.py",
    "post_build_args": ["--apk", "{apk_path}"]
  },
  "post_build": {
    "open_folder": true,
    "run_app": false
  }
}
```

**Poznámka:** pole jako `entry_point`, `icon` ztrácejí v Android kontextu
„reálný" význam (builder je nečte pro Gradle), ale schema je drží povinné.
Buď je volitelně nechme (co mám v ukázce), nebo ve schematu označ jako
**nevyžadovaná** pokud `project_type == "android"`. Druhá varianta je čistější.

---

## 9. Akceptační kritéria

Hotovo, když:

1. Spustím builder GUI, otevřu `C:\Users\12voj\Documents\zeddihub_tools_mobile`.
2. Diagnostický panel ukazuje JDK, SDK, Gradle cache **všechno zelené**, se
   zdrojem `bundle: tools/...`.
3. Kliknu **Build** → builder spustí Gradle child proces s `JAVA_HOME`,
   `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `GRADLE_USER_HOME` nastavenými na bundle
   cesty.
4. `gradlew :app:assembleDebug` doběhne bez errorů.
5. Builder najde `app/build/outputs/apk/debug/ZeddiHub-App-0.5.2.apk` přes glob
   `ZeddiHub-App-{version}.apk`.
6. Post-build step spustí `scripts/upload_apk.py --apk "<path>"`.
7. Pokud smažu `tools/android-sdk`, build **nespadne interně** — ukáže
   diagnostický trail z §7.1 a nevolá Gradle.
8. Schema validátor akceptuje `"project_type": "android"` a novou sekci
   `"android"`.

---

## 10. Soubory, do kterých sáhneš

```
zeddihub-app-builder/
├── android_env.py              ← §4 (major rewrite)
├── android_build.py            ← §6 (env injection, glob, post-build)
├── zeddibuild.schema.json      ← §5 (nová sekce, project_type enum)
├── build_gui.py                ← §7 (diagnostický panel, UI form)
├── spec_loader.py              ← možná propsat nové pole do in-memory
│                                 reprezentace spec, pokud tam je typed class
└── examples/                   ← přidej ukázkový Android `zeddibuild.json`
```

Soubory, kterých se nemáš dotýkat:
- `build_exe.py` — čistě Python/PyInstaller větev
- `version_ops.py`, `git_ops.py`, `github_ops.py` — nesouvisí

---

## 11. Co neudělat

- **Nepřepisuj `local.properties`** v uživatelském projektu. Jen ho čti.
- **Nestahuj** Android SDK / JDK. Když detekce selže, informuj uživatele,
  hotovo. Auto-install je samostatný feature.
- **Nehádej** APK output — vždy ho resolvni přes glob (`apk_name_pattern`),
  ne hardcoded `{module}-debug.apk`.
- **Nezlom** stávající Python/Flutter/RN větve — jen rozšíř, neodebírej.

---

## 12. Otázky, na které chci odpověď v PR description

1. Jaký je fallback plan, když `app/build.gradle.kts` neobsahuje `versionName`?
   (neměl by být blocker, ale `{version}` placeholder v `apk_name_pattern`
   prostě nefunguje — máš ve výstupu nechat literal `"{version}"`? Nebo glob
   bez template rozšíření?)
2. Jak se builder chová při `assembleRelease` / `bundleRelease` — jsou třeba
   keystore env vars (`KEYSTORE_PATH`, …) už čteny z `.env`
   (`android_env.keystore_config_from_env`). Propsat je taky do build env?
3. Má bundle Gradle cache (`GRADLE_USER_HOME` → `tools/gradle-cache`) sdílet
   daemony mezi buildy, nebo invalidovat? Stávající skript spoléhá na `--daemon`
   default; nezasahovat.

---

## 13. Rychlý smoke test (předej zpět, ať ověřím)

Po dokončení napiš do PR description:

```bash
# Předpoklad: builder exe je v PATH nebo ve stejné složce jako projekt.
cd C:\Users\12voj\Documents\zeddihub_tools_mobile
zeddihub-app-builder.exe --build --non-interactive
# Očekávaný výstup:
#   🔧 Toolchain: JDK/SDK/Gradle detekovány z bundle tools/
#   > gradlew :app:assembleDebug
#   BUILD SUCCESSFUL
#   📱 APK: app/build/outputs/apk/debug/ZeddiHub-App-0.5.2.apk
#   (optional) post-build: upload_apk.py → Google Drive
```

Pokud `--non-interactive` CLI mode v builderu zatím neexistuje, preferuji to
taky přidat jako bonus (stačí minimalistický CLI parser, který přeskočí GUI
a rovnou zavolá build plan). Ale není to blocker — GUI stačí.

---

*Předáno: 2026-04-22, repo `zeddihub_tools_mobile` verze 0.5.2,
builder repo `zeddihub-app-builder`.
Reference builderu: `android_env.py`, `android_build.py`, `project_type.py`
ve verzi, kterou jsem právě prolezl.*
