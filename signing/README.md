# App signing key (stable, encrypted)

`release-keystore.jks.enc` is the app's permanent signing keystore, **encrypted** with
AES-256 (`openssl enc -aes-256-cbc -pbkdf2`). It is safe to keep in this public repo —
without the passphrase it is useless.

Every CI build decrypts it and signs the APK with the same key, so each new version
**installs over** the previous one and a classifier's local data/notes are never wiped on
update. (Uninstalling still erases data — use the in-app **Backup & Restore** to keep a
`.zip` copy.)

## One-time setup (already-built releases keep working without this)

Add a single repository secret so GitHub Actions can decrypt the key:

1. GitHub → this repo → **Settings → Secrets and variables → Actions → New repository secret**
2. Name: `SIGNING_PASSPHRASE`
3. Value: the passphrase (kept private; not stored in the repo)

That same passphrase is the keystore's store/key password (alias `iwbf`).

## Cutting a signed release

Bump the version in `app/build.gradle.kts`, then push a tag:

```
git tag v1.6.0
git push origin v1.6.0
```

CI builds the signed APK and publishes it as a GitHub Release automatically.

> ⚠️ If `SIGNING_PASSPHRASE` is ever lost, the key cannot be recovered and the next
> version would need a one-time reinstall. Keep the passphrase backed up somewhere safe.
