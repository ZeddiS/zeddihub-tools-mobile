# Google Drive uploader — jednorázové nastavení

`./build.sh` / `build.bat` po úspěšném buildu spouští `scripts/upload_apk.py`,
který nahraje APK na Google Drive do složky `/ZeddiHub App/<versionName>/`.

Pro **první spuštění** je potřeba jednorázově vytvořit OAuth klienta:

1. Otevři [Google Cloud Console](https://console.cloud.google.com/).
2. Vytvoř (nebo vyber) projekt a povol **Google Drive API**
   (APIs & Services → Library → „Google Drive API" → Enable).
3. Přejdi na **APIs & Services → OAuth consent screen**:
   - User type: **External**, uložit.
   - V **Test users** přidej svůj Google účet.
4. **APIs & Services → Credentials → + CREATE CREDENTIALS → OAuth client ID**:
   - Application type: **Desktop app**.
   - Zkopíruj vygenerované **Client ID** a **Client secret**.
5. Spusť build:
   ```bash
   ./build.sh            # Linux / Git Bash
   build.bat             # Windows cmd
   ```
   Při prvním spuštění tě skript vyzve, abys vložil Client ID a Client secret,
   pak otevře prohlížeč s Google přihlášením.
   Po udělení souhlasu se vrátíš zpět a skript dokončí upload.

Token se uloží do `tools/gdrive/config.json` (složka `/tools/` je v `.gitignore`,
nic se tedy nedostane do repozitáře).

## Vypnout upload pro jeden build

```bash
SKIP_UPLOAD=1 ./build.sh
```

```bat
set SKIP_UPLOAD=1
build.bat
```

## Ruční upload (bez buildu)

```bash
./upload.sh                       # posledni debug APK do <versionName>/
./upload.sh --version 0.3.0-rc1   # vlastni nazev slozky
./upload.sh --apk path/k/file.apk --name ZeddiHub-0.2.0.apk
```
