# AsterLink

AsterLink is the account-linking system for **Aster MC**. It connects a Minecraft Bedrock account with a Discord account using a short-lived 6-digit code.

## Architecture

```text
Bedrock Player
     ↓
Geyser + Floodgate
     ↓
Velocity
     ├── AsterLink
     ↓
Paper

AsterLink
     ↓ HTTPS
Aster MC Link API (Render)
     ↓
Supabase PostgreSQL
     ↑
Discord Bot
```

**Important:** Geyser and Floodgate are on Velocity. AsterLink is a **Velocity plugin**, not a Paper plugin.

## Repository Structure

```text
Aster-Link-/
├── pom.xml
├── src/
│   └── main/java/me/astermc/link/
│       └── AsterLink.java
├── api/
│   ├── package.json
│   └── index.js
├── bot/
│   ├── package.json
│   └── index.js
├── .github/workflows/
│   └── build.yml
└── aster-link-plugin/
    └── pom.xml
```

The **root `pom.xml`** is the current Maven project. The `aster-link-plugin/pom.xml` is an older project file and is not used for the current build.

---

## 1. Velocity Plugin

File:

```text
src/main/java/me/astermc/link/AsterLink.java
```

The plugin:

- Registers `/link` on Velocity.
- Checks whether a player is a Floodgate/Bedrock player.
- Generates a 6-digit code.
- Sends the Minecraft UUID, username and code to the API.
- Tells the player to enter the code in Discord.
- Uses HTTPS to communicate with the API.

### Plugin JAR

GitHub Actions produces:

```text
aster-link-1.0.0.jar
```

Install it here:

```text
Velocity/plugins/aster-link-1.0.0.jar
```

**Do not install it in Paper's `plugins/` folder.**

---

## 2. Velocity Environment Variables

Add these to the **Velocity server**.

### API URL

```text
ASTERLINK_API_URL=https://aster-link.onrender.com
```

### API Secret

```text
ASTERLINK_API_SECRET=YOUR_EXISTING_API_SECRET
```

The value must be the same secret used by the Aster MC Link API.

**Never put the real secret in GitHub, this README, Discord, or source code.**

---

## 3. Aster MC Link API

The API is hosted on Render.

API URL:

```text
https://aster-link.onrender.com
```

### Health Check

```text
GET /
```

Expected response:

```json
{
  "service": "Aster MC Link API",
  "status": "online"
}
```

### Create Link Code

```text
POST /api/link/create
```

Used by AsterLink.

Example data:

```json
{
  "minecraftUuid": "PLAYER_UUID",
  "minecraftUsername": "PlayerName",
  "code": "123456"
}
```

The code is stored with a 5-minute expiration.

### Verify Link Code

```text
POST /api/link/verify
```

Used by the Discord bot.

Example:

```json
{
  "discordId": "DISCORD_USER_ID",
  "code": "123456"
}
```

A valid code creates the Discord ↔ Minecraft link.

### Get Link Information

```text
GET /api/link/:discordId
```

Used by Discord commands such as `/profile`.

---

## 4. Supabase Database

The API uses PostgreSQL through Supabase.

### `link_codes`

Stores temporary linking codes.

Important fields:

```text
id
code
minecraft_uuid
minecraft_username
expires_at
used
created_at
```

### `links`

Stores completed links.

Important fields:

```text
id
discord_id
minecraft_uuid
minecraft_username
linked_at
```

Discord IDs and Minecraft UUIDs are unique, preventing an account from being linked multiple times.

---

## 5. Discord Bot

The Discord bot provides:

```text
/link
/profile
/linkinfo
```

### Linking Flow

Minecraft player runs:

```text
/link
```

They receive a 6-digit code, for example:

```text
583214
```

They then use Discord:

```text
/link code:583214
```

The bot sends the code to the API.

The API checks:

- Code exists.
- Code is not expired.
- Code has not already been used.
- Discord account is not already linked.
- Minecraft account is not already linked.

If valid, the link is saved.

The bot can then assign the configured linked role.

---

## 6. Discord Bot Environment Variables

Required:

```text
DISCORD_TOKEN
DISCORD_CLIENT_ID
DISCORD_GUILD_ID
API_URL
API_SECRET
LINKED_ROLE_ID
```

Optional Aster MC branding:

```text
ASTER_LOGO_URL
ASTER_BANNER_URL
```

`API_SECRET` must match the API secret.

Never commit these values to GitHub.

### Linked Role

The Discord bot needs permission to manage roles, and its highest role must be above the role specified by:

```text
LINKED_ROLE_ID
```

---

## 7. `/profile`

After linking, a user can run:

```text
/profile
```

The bot requests:

```text
GET /api/link/:discordId
```

and displays the linked Minecraft account in the Aster MC embed design.

---

## 8. Building the Plugin

GitHub Actions uses:

```text
.github/workflows/build.yml
```

It:

1. Checks out the repository.
2. Sets up Java 21.
3. Runs Maven.
4. Builds the plugin.
5. Uploads the JAR as an artifact.

Build command:

```bash
mvn clean package -DskipTests
```

Output:

```text
target/aster-link-1.0.0.jar
```

---

## 9. Installing AsterLink

1. Download the `AsterLink-Velocity` GitHub Actions artifact.
2. Extract the ZIP.
3. Find:

```text
aster-link-1.0.0.jar
```

4. Stop Velocity.
5. Upload the JAR to:

```text
Velocity/plugins/
```

6. Add:

```text
ASTERLINK_API_URL=https://aster-link.onrender.com
ASTERLINK_API_SECRET=YOUR_EXISTING_API_SECRET
```

7. Start Velocity.
8. Check the console.

Expected messages include:

```text
AsterLink has been enabled!
Aster MC Discord linking command registered!
```

---

## 10. Troubleshooting

### AsterLink says it is not configured

Check:

```text
ASTERLINK_API_URL
ASTERLINK_API_SECRET
```

### API returns Unauthorized

The API secret does not match.

Make sure the secret on Velocity is exactly the same as the API's `API_SECRET`.

Do not post the secret while troubleshooting.

### `/link` says Bedrock players only

Check that Floodgate is installed and working on Velocity.

### Plugin does not load

Check:

- The JAR is in Velocity's `plugins/` folder.
- You are using the new Velocity JAR.
- Java is compatible.
- Floodgate is installed on Velocity.
- The Velocity console for the exact error.

### Discord rejects the code

Check:

- The code has 6 digits.
- The code has not expired.
- The API is online.
- The Discord bot's `API_URL` is correct.
- The Discord bot's `API_SECRET` matches the API.

---

## 11. Security

AsterLink protects API endpoints using:

```text
Authorization: Bearer YOUR_SECRET
```

Use HTTPS for API communication.

Never store production secrets in:

```text
GitHub source files
pom.xml
AsterLink.java
README.md
Discord messages
```

Use environment variables.

---

## Complete Flow

```text
1. Bedrock player joins Aster MC
        ↓
2. Player runs /link
        ↓
3. AsterLink generates a 6-digit code
        ↓
4. Code is stored by the API
        ↓
5. Player uses /link code:123456 in Discord
        ↓
6. Discord bot verifies the code
        ↓
7. API creates the Discord ↔ Minecraft link
        ↓
8. Discord bot gives the linked role
        ↓
9. Player can use /profile
```

## Aster MC

**Minecraft:** Velocity → Paper  
**Bedrock:** Geyser + Floodgate on Velocity  
**API:** Render  
**Database:** Supabase PostgreSQL  
**Discord:** Discord.js  
**Build:** GitHub Actions + Maven
