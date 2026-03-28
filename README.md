# YadduGuard - AI Chat Moderation for Velocity

AI-powered chat moderation plugin for Velocity proxy servers. 
Uses **Gemini** or **Grok** to detect toxic messages, gaaliyan, harassment and auto-punishes.

## Features
- 🤖 AI moderation via **Gemini 1.5 Flash** or **Grok**
- 🔇 **Soft Mute** for mild abuse (timed, auto-expiry)
- 👢 **Kick** for moderate toxicity
- 🔨 **Temp Ban** for severe toxicity (via LiteBans/AdvancedBan command)
- 🌐 **IP Ban** for extreme behavior
- 🏛️ **Jail** support (optional, needs jail plugin)
- 📈 **Violation escalation** - repeat offenders get harsher punishments
- ✅ **Fail open** - if AI API is down, messages pass through
- ⚡ **Async** - uses Velocity's EventTask, zero blocking

## Punishment Escalation

```
Severity 3-4  → Soft Mute (5 min default)
Severity 5-6  → Kick
Severity 7-8  → Jail (first 2 offenses) → Ban
Severity 9-10 → IP Ban
```

After 3 violations in a session → automatically escalates to next tier.

## Installation

1. Build with Maven: `mvn clean package`
2. Drop `YadduGuard-1.0.0.jar` into Velocity's `plugins/` folder
3. Start server once to generate `plugins/yaddguard/config.yml`
4. Add your API key and configure

## Configuration

Edit `plugins/yaddguard/config.yml`:

```yaml
ai:
  provider: "gemini"    # or "grok"
  gemini:
    api-key: "YOUR_KEY_HERE"
  grok:
    api-key: "YOUR_KEY_HERE"
```

### Getting API Keys

**Gemini:** https://aistudio.google.com/apikey (Free tier available!)
**Grok:** https://console.x.ai (Paid)

Gemini Flash recommended - fast & cheap for moderation.

## Commands

| Command | Description |
|---|---|
| `/yg status` | Show plugin status & config |
| `/yg reload` | Reload config.yml |
| `/yg unmute <player>` | Remove soft mute |
| `/yg violations <player>` | Show violation history |
| `/yg resetv <player\|all>` | Reset violations |
| `/yg test <message>` | Test AI on a message |

## Permissions

| Permission | Description |
|---|---|
| `yaddguard.admin` | Access all /yg commands, receive alerts |
| `yaddguard.bypass` | Bypass all AI moderation |

## Ban Commands Compatibility

The plugin executes ban/ipban as console commands. Works with:
- **LiteBans**: `ban {player} 1h {reason}` ✅
- **AdvancedBan**: `tempban {player} 1h {reason}` ✅  
- **VelocityBan**: Native Velocity ban support ✅

Change `punishments.ban.command` in config to match your ban plugin syntax.

## Notes

- For Jail to work, install a jail plugin on your **backend** servers (e.g. EscapeRestrict)
- The plugin uses Velocity's `EventTask` for true async chat handling (no blocking)
- Hindi/Hinglish gaaliyan are detected by Gemini/Grok natively - the AI understands context
- Debug mode logs all AI responses to console - useful for tuning thresholds

## Build Requirements

- Java 17+
- Maven 3.6+
- Velocity API 3.3.0

```bash
mvn clean package
# Output: target/YadduGuard-1.0.0.jar
```
