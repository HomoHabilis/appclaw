# AppClaw Vault — Architecture Plan

AppClaw turns your Android device into a Hardware Security Module (HSM) and authorization gateway for a cloud-hosted OpenClaw instance. Instead of keeping credentials and sensitive tokens in the cloud, they live in the Android Keystore on the user's device and are released only after explicit biometric approval.

## Problems Solved

**Problem 1 — Server setup friction.** Today you must provision and maintain a server. AppClaw removes this by automatically spawning an isolated cloud container when the app is first launched. The user never touches a terminal.

**Problem 2 — Data exposure.** Credentials currently live in the cloud environment. AppClaw keeps every sensitive secret on the device, protected by hardware-backed biometric authentication. The cloud container is intentionally credential-less and must ask the phone before accessing anything sensitive.

## Execution Loop

```
Cloud OpenClaw detects it needs a credential
          ↓
Sends CloudAuthRequest payload to the Android app
(via WebSocket over NodeForegroundService or FCM)
          ↓
App checks PermissionLeaseManager
  ┌─ GREEN tier → auto-approve, return result
  ├─ YELLOW tier + valid lease → auto-approve, return result
  └─ RED tier or no lease → show VaultApprovalScreen
          ↓
User sees rich context: "OpenClaw wants to charge $200 on Delta Airlines"
          ↓
User authenticates via BiometricVault (BiometricPrompt)
          ↓
App releases a time-bound token / executes the call locally
          ↓
Result returned to cloud; lease recorded; approval count incremented
```

## Tiered Security Model

| Tier | Risk | Examples | Default Behavior |
|------|------|----------|-----------------|
| 🔴 RED | High | Financial transactions, bulk email sends, file deletions | Always requires immediate biometric. No leases. |
| 🟡 YELLOW | Medium | Reading email, calendar access, DM reads | Lease-based. First approval shows prompt; user can grant 1 h – 30 d lease. |
| 🟢 GREEN | Low | Web browsing, drafting notes, reading news | Always granted, runs autonomously. |

## Progressive Trust

`PermissionLeaseManager` tracks the approval count for every action key. When the count crosses a threshold the app surfaces a suggestion in `AgentAnalyticsDashboard`:

> "You've manually approved email access 50 times without incident. Grant a 30-day lease?"

This lets users build confidence incrementally instead of being asked to trust everything upfront or being buried in prompts.

## Alert Fatigue Mitigation

- **Batching** — YELLOW requests that arrive while the phone is idle are queued and bundled into a single morning briefing card.
- **Quiet Hours** — configurable window during which only RED-tier requests break through.
- **Lease Upgrade** — once the user trusts an action, they can expand the lease duration step by step (1 h → 4 h → 1 d → 7 d → 30 d).

## Cloud Architecture

- One Docker container per user, spawned on first app launch via a REST call to the AppClaw backend.
- Containers "sleep" when idle; woken via serverless trigger when the user opens the app or sends a message.
- State-management: long-running workflows checkpoint their state before any permission pause so they can resume seamlessly after the user provides their thumbprint.

## Cookie / Token Injection

When the cloud OpenClaw needs to browse a site that requires authentication:
1. The phone holds the session cookie in the Android Keystore.
2. On biometric approval the phone passes a **short-lived, single-use token** to the cloud (not the raw cookie).
3. The cloud destroys the token after use or after the lease expires.

## Key New Source Files

```
apps/android/app/src/main/java/ai/openclaw/app/
  vault/
    PermissionTier.kt          — Red / Yellow / Green enum
    PermissionLease.kt         — Serializable lease record
    CloudAuthRequest.kt        — Incoming cloud request model
    PermissionLeaseManager.kt  — Lease lifecycle + progressive trust
    BiometricVault.kt          — BiometricPrompt wrapper
  ui/
    vault/
      VaultApprovalScreen.kt   — Rich approval prompt UI
      AgentAnalyticsDashboard.kt — Trust analytics + upgrade suggestions
```

## Prototype Milestones

1. **M0** (this PR) — Scaffold: data models, lease manager, biometric wrapper, approval UI, analytics dashboard.
2. **M1** — Wire `NodeForegroundService` to forward incoming `cloud_auth_request` WebSocket frames to `PermissionLeaseManager`.
3. **M2** — Backend service: `/spawn` endpoint that provisions a Docker container and returns a WebSocket URL + bootstrap token.
4. **M3** — Cookie-injection flow: phone extracts and short-lives a session token on biometric approval.
5. **M4** — Quiet Hours + batching briefing card.
6. **M5** — Progressive trust suggestion UI in `AgentAnalyticsDashboard`.
