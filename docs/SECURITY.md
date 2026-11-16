# Security and deployment gate

- Treat imported model files as untrusted data. GGUF parser checks only the header; it cannot validate a full model.
- Model storage is private to this app. Every import is staged; a SHA-256 is calculated. Model verification against a known expected digest is optional.
- HF download URLs are constructed from validated repo names and paths. HTTPS redirects are restricted to an allowlist. HF token is not forwarded to CDN hosts.
- Local model API is restricted to 127.0.0.1. AI output never automatically authorizes mining commands.
- Remote mining control still requires an independent authenticated gateway, idempotency, replay protection, per-device safety limits and physical watchdog.
- Full Android builds, device tests, native llama.cpp, Stratum V2 and Zenoh are not included or proven.
