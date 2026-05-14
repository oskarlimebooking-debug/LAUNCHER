# Platform Signing for Retro Launcher (v0.3)

The `system` flavor runs under `android.uid.system` and enables features that
require a platform certificate — embedding, HiddenApi access, and input
injection. This document describes how to extract the platform keys from a
head unit ROM and provision the keystore.

## Prerequisites

- PC with `adb`, `openssl`, and `keytool` (JDK)
- Head unit with USB debugging enabled
- ROM image for the head unit (OTA zip or system image dump), or root shell
  on the device

## Step 1 — Locate the platform keys

### Option A — Extract from the device (root required)

```bash
adb root
adb pull /system/etc/security/platform.x509.pem .
adb pull /system/etc/security/platform.pk8 .
```

On some ROMs the files live under `/system/etc/security/`, on others under
`/system/vendor/etc/security/`.

If the device is not rooted, use Option B.

### Option B — Extract from a ROM image

1. Download the OTA update zip or system image for your head unit model.
2. Extract `system.img` (or `system.new.dat` + `system.transfer.list`)
   using `sdat2img.py` or `unpackbootimg`.
3. Mount the image:

```bash
mkdir /tmp/system
sudo mount -o ro system.img /tmp/system
cp /tmp/system/etc/security/platform.x509.pem .
cp /tmp/system/etc/security/platform.pk8 .
sudo umount /tmp/system
```

## Step 2 — Convert to a PKCS#12 keystore

Android platform keys are stored as a PEM certificate + PKCS#8 private key.
`keytool` cannot read PKCS#8 directly, so convert via `openssl`:

```bash
# Convert PKCS#8 → PEM key pair (PKCS#12-compatible)
openssl pkcs8 -in platform.pk8 -inform DER -outform PEM -out platform.key -nocrypt

# Bundle the key + certificate into a PKCS#12
openssl pkcs12 -export \
  -in platform.x509.pem \
  -inkey platform.key \
  -out platform.p12 \
  -name platform \
  -password pass:android
```

## Step 3 — Import into a JKS keystore

```bash
keytool -importkeystore \
  -destkeystore platform.keystore \
  -deststorepass android \
  -destkeypass android \
  -srckeystore platform.p12 \
  -srcstoretype PKCS12 \
  -srcstorepass android \
  -alias platform
```

Place the resulting `platform.keystore` at the repository root (next to
`settings.gradle.kts`). The Gradle build reads it from
`rootProject.file("platform.keystore")`.

## Step 4 — Verify the keystore

```bash
keytool -list -v -keystore platform.keystore -storepass android
```

You should see a single entry with alias `platform`. The certificate subject
should include `EMAILADDRESS=android@android.com` or similar (varies by ROM).

## Step 5 — Configure passwords (optional)

The build defaults to `android` for the keystore password, key password, and
alias. If your keystore uses different values, set them in
`local.properties`:

```properties
PLATFORM_STORE_PASSWORD=your_keystore_password
PLATFORM_KEY_ALIAS=your_alias
PLATFORM_KEY_PASSWORD=your_key_password
```

## Step 6 — Build

```bash
./gradlew :app:assembleSystemDebug
```

The APK is at `app/build/outputs/apk/system/debug/app-system-debug.apk`.

### Signing verification

```bash
apksigner verify --print-certs \
  app/build/outputs/apk/system/debug/app-system-debug.apk
```

The signer certificate DN should match the platform certificate.

## Troubleshooting

### "Keystore was tampered with, or password was incorrect"

Re-create the keystore from Step 2 onward. `keytool` is sensitive to the JKS
version — if the head unit runs an older Android, add `-deststoretype JKS` to
the `keytool` command.

### Build prints "platform.keystore not found — system flavor skipped"

The keystore file is not at the repository root. Verify with:

```bash
ls -la platform.keystore
```

The standard flavor continues to build without it.

### "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE"

The APK declares `sharedUserId="android.uid.system"` but is not signed with
the platform certificate. Re-sign with the correct keystore.

## Security

The `platform.keystore` and intermediate `.pk8`/`.p12`/`.key` files grant
system-level privileges on the head unit. They are `.gitignore`'d by the
project — never commit them to the repository.
