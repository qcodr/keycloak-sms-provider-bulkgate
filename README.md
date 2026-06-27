# Keycloak BulkGate SMS OTP authenticator

A Keycloak authenticator that sends a one-time code by SMS through
[BulkGate](https://www.bulkgate.com/) and verifies it. Works with **Keycloak
26.6.3**.

- **Realm-configurable** — code length, TTL, attempt/resend limits, message
  text, phone-number attribute, country code and all BulkGate credentials are
  set per authenticator execution in the admin console.
- **First or second factor** — bind the same execution after a username form
  (first factor) or after the password form (second factor).
- **Keycloak owns the security boundary** — the code is generated, hashed and
  verified inside Keycloak; BulkGate is used purely as the SMS transport (its
  Advanced Transactional API). Only a salted SHA-256 hash of the code is kept in
  the authentication session — never the code itself.

> Design note: BulkGate also offers a managed OTP API (send/verify/resend). This
> plugin deliberately does **not** use it — Keycloak generates and verifies the
> code locally and BulkGate only delivers the SMS, which keeps the verification
> logic and its security properties inside Keycloak.

## How it works

```
authenticate()                              action()  (code submitted)
──────────────                              ─────────────────────────
read realm config                           read realm config
read phone from user attribute              load challenge from auth session
  └─ missing → enrol via required action    verify(code, now, maxAttempts)
normalise to E.164 (libphonenumber)           ├─ VALID            → success()
generate N-digit code (SecureRandom)          ├─ INVALID          → ++attempts, re-ask / lock
hash(code, random salt) → auth session        ├─ EXPIRED          → fail
format SMS text (%code%, %ttl%)               ├─ TOO_MANY_ATTEMPTS→ fail
send via BulkGate transactional API           └─ NO_CHALLENGE     → fail
challenge the code-entry form               "Resend" → ResendPolicy (cooldown + cap)
```

### Package layout (SOLID, small focused units)

| Package | Responsibility |
|---|---|
| `otp` | Pure domain: code generation, salted hashing, the immutable `OtpChallenge`, `OtpVerifier`, `ResendPolicy`. No Keycloak dependency, fully unit-tested. |
| `phone` | `PhoneNumber` value object + `PhoneNumberNormalizer` (libphonenumber-backed, correct per-country trunk prefixes). |
| `gateway` | `SmsGateway` abstraction, `BulkGateSmsGateway` (java.net.http), `SimulationSmsGateway`, immutable `BulkGateSettings`. |
| `message` | `SmsTextFormatter` (placeholder substitution). |
| `config` | `SmsAuthenticatorConfig` (typed, validated view of the realm config) + `ConfigProperties` (admin-console fields). |
| `authenticator` | Keycloak SPI: `SmsOtpAuthenticator`, its factory, `OtpChallengeStore` (auth-session persistence), `SmsGatewayResolver`. |
| `requiredaction` | `PhoneNumberRequiredAction` — phone-number enrollment. |

## Configuration

All keys are set per authenticator execution (Admin Console → Authentication →
your flow → the *BulkGate SMS OTP* execution → ⚙ Config).

| Key | Default | Meaning |
|---|---|---|
| `codeLength` | `6` | Digits in the code (4–10). |
| `codeTtlSeconds` | `300` | How long a code is valid. |
| `maxVerifyAttempts` | `3` | Wrong guesses allowed **per login session** (a resend does not reset this). |
| `resendCooldownSeconds` | `30` | Minimum delay between resends. |
| `maxResends` | `3` | Resends allowed per login session. |
| `smsTextTemplate` | `Your verification code is %code%. It is valid for %ttl% minutes.` | SMS body. Placeholders: `%code%`, `%ttl%`. |
| `phoneNumberAttribute` | `mobile_number` | User attribute holding the number. |
| `defaultCountryCode` | `+36` | Dialing code for numbers stored without an international prefix. |
| `simulationMode` | `false` | Log the code instead of sending it (**development only**). |
| `bulkgateApiUrl` | `https://portal.bulkgate.com/api/1.0/advanced/transactional` | Endpoint (http/https only). |
| `bulkgateApplicationId` | — | BulkGate Application ID. |
| `bulkgateApplicationToken` | — | BulkGate Application Token (stored as a secret). |
| `bulkgateSenderId` | `gSystem` | Sender id type: `gSystem`, `gText`, `gOwn`, `gProfile`, `gMobile`, `gPush`. |
| `bulkgateSenderIdValue` | — | Value for the sender id type (e.g. a text sender name). |
| `bulkgateUnicode` | `false` | Send as Unicode. |
| `bulkgateCountry` | — | Optional ISO country hint for BulkGate. |

## Build

Java 21+ is required (Docker only for the e2e tests / demo). Maven is **not**
needed — the Gradle wrapper is bundled. A `Makefile` wraps the common tasks:

```bash
make            # list all targets
make jar        # build the provider jar  → build/libs/*.jar
make build      # compile + unit/integration tests + jar
make test       # fast unit + WireMock tests (no Docker)
make e2e        # Docker-backed end-to-end tests
make up / down  # start / stop the local demo stack
```

`make jar` is equivalent to `./gradlew shadowJar`. The jar bundles only
libphonenumber; all Keycloak/Jackson classes are provided by the server at
runtime.

## Installation

Requirements: Keycloak **26.6.3** and Java 21+ on the server.

1. **Build the provider jar.**

   ```bash
   make jar      # → build/libs/keycloak-bulkgate-sms-authenticator-1.0.0.jar
   ```

2. **Deploy it.** Copy the jar into Keycloak's provider directory:

   ```bash
   cp build/libs/keycloak-bulkgate-sms-authenticator-*.jar "$KEYCLOAK_HOME/providers/"
   ```

   The FreeMarker forms and message bundles are inside the jar
   (`theme-resources/`); no separate theme installation is needed.

3. **Rebuild the server augmentation and start Keycloak.**

   ```bash
   "$KEYCLOAK_HOME/bin/kc.sh" build
   "$KEYCLOAK_HOME/bin/kc.sh" start        # or start-dev for local testing
   ```

   On a container image, mount the jar into `/opt/keycloak/providers/` and let the
   entrypoint run the build (as the [docker-compose.yml](docker-compose.yml) demo
   does).

4. **Verify it loaded.** In the admin console, *Authentication → Flows → Add
   step* should list **BulkGate SMS OTP**, and *Authentication → Required
   actions* should list **Configure BulkGate SMS phone number**.

### Configure it

After deploying, set the plugin up in a realm:

1. **Add the execution to a login flow.** *Authentication → Flows* → duplicate
   *browser* (or build a flow). Add the **BulkGate SMS OTP** step and set it
   **Required** — place it after the password form (second factor) or after a
   username form (first factor).
2. **Enter the BulkGate credentials.** Open the step's **⚙ (gear) → Config** and
   fill in at least `bulkgateApplicationId` and `bulkgateApplicationToken` from
   your BulkGate API application. Adjust code length, TTL, attempt/resend limits,
   message text, the phone-number attribute and default country code as needed —
   see the [Configuration](#configuration) table for every field. (For a dry run
   without real SMS, turn on `simulationMode` — development only.)
3. **Bind the flow.** *Authentication → Flows* → your flow → *Action → Bind flow
   → Browser flow*.
4. **Give users a phone number.** Store it in the configured attribute
   (`mobile_number` by default). Users without one are sent through the
   *Configure BulkGate SMS phone number* required action on their next login. If
   your realm uses the declarative user profile, declare that attribute (or
   enable the unmanaged-attribute policy) so it persists.

## Tests

```bash
./gradlew test       # fast unit + WireMock integration tests (no Docker)
./gradlew e2eTest    # full browser login against real Keycloak + mocked BulkGate (Docker)
```

- **Unit / integration** — the pure OTP/phone/config logic plus the BulkGate
  gateway against an in-process WireMock.
- **E2E** — spins up Keycloak 26.6.3 with the provider deployed and a WireMock
  standing in for BulkGate (Testcontainers), configures a realm via the admin
  client, then drives the real login pages with HtmlUnit: it logs in with
  password, reads the OTP that was "sent" to WireMock, submits it, and asserts
  the authorization code comes back. A second test asserts a wrong code is
  rejected and keeps the user on the form.

## Local demo (Docker Compose)

```bash
./gradlew shadowJar
docker compose up
```

This starts Keycloak (`http://localhost:8080`) with the provider deployed and a
WireMock BulkGate mock, and imports the `bulkgate-demo` realm: a `bulkgate-browser`
flow (password → SMS OTP), a public `demo-client`, and user **alice / password**
with `mobile_number = +36201234567`.

Log in to the `bulkgate-demo` realm as `alice`, then read the code that was
"sent" from the WireMock journal:

```bash
curl -s localhost:8081/__admin/requests | jq -r '.requests[0].request.body'
```

## Security notes & known limitations

- **Code security.** Only a salted SHA-256 hash is stored, comparison is
  constant-time (`MessageDigest.isEqual`), expiry and the attempt budget are
  checked before any comparison, and the attempt budget is **per login session**
  (a resend cannot reset it). `SecureRandom` backs both the code and the salt.
- **Simulation mode** logs the code in cleartext and must **never** be enabled in
  production. It is intended only for local development.
- **Phone enrollment is not ownership-verified.** The required action stores the
  number the user types without sending a confirmation code (a required action
  has no access to the BulkGate config). If a user has no number on file the
  authenticator enrolls them and completes — so **do not deploy this as a sole
  first factor** unless you accept that unenrolled users pass it during
  enrollment. Prefer second-factor use, an admin-provisioned/verified phone
  attribute, or a flow that forces enrollment before access.
- **`phoneNumberAttribute` customization.** Keycloak does not pass the execution
  config to `configuredFor()` or to required actions, so the “is configured”
  check and the enrollment action use the **default** attribute name
  (`mobile_number`) and the **default** country code (`+36`). Keep these at their
  defaults unless you populate the attribute yourself; otherwise enrolled users
  may be re-prompted.
- **SMS pumping.** Resend throttling is per session. To bound cost/abuse across
  sessions, enable Keycloak realm **brute-force detection** and place Keycloak
  behind a rate-limiting reverse proxy.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Portions are derived from
[netzbegruenung/keycloak-mfa-plugins](https://github.com/netzbegruenung/keycloak-mfa-plugins)
(Apache-2.0); see [NOTICE](NOTICE).
