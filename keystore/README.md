# keystore/

The `release.jks` file is the production signing keystore for ArvanShare Android.

> **Never commit `release.jks` or any `.jks` / `.p12` / `.keystore` file.  
> These are listed in `.gitignore`.**

---

## Keystore details

| Field          | Value            |
|----------------|------------------|
| File           | `release.jks`    |
| Alias          | `arvanshare`     |
| Algorithm      | RSA 2048-bit     |
| Validity       | 10 000 days      |
| Store password | *(see below)*    |
| Key password   | *(same as store)*|

---

## Setting up GitHub Actions secrets

The release workflow (`release.yml`) reads four repository secrets.
Add them at **GitHub → repo → Settings → Secrets and variables → Actions**.

| Secret name        | How to get the value                               |
|--------------------|----------------------------------------------------|
| `KEYSTORE_BASE64`  | Base64-encode `release.jks` (see command below)   |
| `STORE_PASSWORD`   | The keystore store password                        |
| `KEY_ALIAS`        | `arvanshare`                                       |
| `KEY_PASSWORD`     | The key password (same as store password)          |

### Encode the keystore to base64 (Windows PowerShell)

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore\release.jks")) | Set-Clipboard
# The base64 string is now in your clipboard — paste it into KEYSTORE_BASE64
```

### Encode on Linux / macOS

```bash
base64 -w 0 keystore/release.jks | pbcopy   # macOS (pbcopy)
base64 -w 0 keystore/release.jks | xclip    # Linux (xclip)
```

---

## Local release build (without CI)

```powershell
$env:KEYSTORE_PATH  = "keystore\release.jks"
$env:STORE_PASSWORD = "YOUR_STORE_PASSWORD"
$env:KEY_ALIAS      = "arvanshare"
$env:KEY_PASSWORD   = "YOUR_KEY_PASSWORD"

.\gradlew assembleRelease
# APK: app\build\outputs\apk\release\app-release.apk
```

---

## Regenerating the keystore

If you ever need to regenerate (e.g. key compromise):

```powershell
& "C:\Program Files\Java\jdk-25.0.2\bin\keytool.exe" `
  -genkeypair -v `
  -keystore keystore\release.jks `
  -alias arvanshare `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -storepass YOUR_STORE_PASSWORD `
  -keypass   YOUR_KEY_PASSWORD `
  -dname "CN=ArvanShare,OU=Mobile,O=ArvanShare,L=Tehran,ST=Tehran,C=IR"
```

> ⚠ Regenerating means existing APKs from the old key can no longer be updated
> over the same install. All users must uninstall first.
