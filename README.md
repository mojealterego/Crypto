# OMEGA Sovereign Swarm — Android 1.1

Android Studio source project: Java 17, Android Gradle Plugin 8.7.3, compileSdk 35, minSdk 33. This repository does NOT contain a compiled or device-validated APK.

## Features

- Mining: HTTPS gateway dashboard and P-256 StrongBox biometric requests for STOP, pool selection and DVFS profile. Hardware gateway is not included.
- GGUF: Storage Access Framework picker, app-private streamed import, GGUF v2/v3 header inspection, SHA-256, model list, selection, export and deletion.
- Hugging Face: list GGUF filenames from owner/repository, resolve commit SHA, secure HTTPS streaming to .part, HTTP Range resume on immutable revisions, optional expected SHA-256, redirect allowlist, 64 GiB cap. The optional read token is not stored.
- Local chat: optional connection to an independently started llama-server on 127.0.0.1:8080 using /v1/chat/completions. There is no embedded inference engine. AI has no mining control permissions.

## Build

Open project in Android Studio with JDK 17 and SDK 35. Run: gradle :app:assembleDebug. If build succeeds, the APK will be at app/build/outputs/apk/debug/app-debug.apk. Gradle wrapper is not bundled. No Android SDK or Gradle build has been run in the available environment.

## Test

Run: bash scripts/test-jvm.sh

JVM tests exercise command encoding, GGUF header validation, HF path restrictions, SHA-256 import, rejection of bad input and HTTP Range decisions. Android UI and actual network transfers remain untested.

## Import and downloading

Open GGUF/HUGGING FACE/MODELE. Pick a local GGUF with the system document picker, or provide a Hugging Face owner/repo and choose a model file from the fetched list. A gated repo needs an authorized read token. Downloaded files are stored app-privately. Supplying a known SHA-256 verifies matching bytes. Models can be selected, exported and deleted.

To chat with a local model, export the GGUF to a Termux-accessible directory and launch a separately installed llama.cpp server, for example:

    ~/llama.cpp/build/bin/llama-server --model ~/storage/downloads/my-model.gguf --host 127.0.0.1 --port 8080 --ctx-size 2048

Then open Local Chat. Model selection in the vault does not automatically load model weights into the server.

## Safety limitations

- No blanket storage permission and no root.
- StrongBox-backed key and per-operation biometric approval are required for remote mining commands; the compatible hardware gateway is NOT provided.
- HF redirect allowlist uses HTTPS only. Token never travels to CDN domains.
- GGUF parsing checks the 24-byte header, not all tensors. Split/multimodal models require companion files; untrusted model files may exploit external inference engines.
- Transfer tasks run while the app activity is active; Android may terminate background work. A pinned revision can resume from .part.
- No native llama.cpp JNI engine, no native Stratum V2/Zenoh integration, no physical ASIC or StrongBox validation, no compiled APK.

References:
https://developer.android.com/guide/topics/providers/document-provider
https://huggingface.co/docs/huggingface_hub/package_reference/file_download
https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md
