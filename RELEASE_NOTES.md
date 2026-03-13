# Release notes

## 1.0.2 (versionCode 5) — 2026-03

### Optional cloud persona voices

- **Google Cloud Text-to-Speech (TTS)**  
  You can optionally use Google Cloud TTS for alarm briefings so the voice better matches the selected persona. Add your own API key in **Settings → Intelligence → API Credentials**.

- **Persona Voice (Cloud TTS) card**  
  - Toggle **Use cloud-quality voices** (requires a valid Cloud TTS key).  
  - **Preview Persona Voice** plays a short test phrase only (no full briefing).  
  - If Cloud TTS is unavailable, the app falls back to the on-device voice so alarms still work.

- **Cloud TTS key setup**  
  - **Test API Key** checks your key without enabling the feature (works even when the toggle is off).  
  - **Get Key** opens the Google Cloud Console; use **Create credentials → API key**, then restrict the key to **Cloud Text-to-Speech API**.  
  - Billing must be enabled on the project that owns the key (free tier is available).

### Intelligence section (Settings)

- **Briefing Brain (Gemini)**  
  - Toggle to use Gemini for briefings; **Quick Gemini Test** runs a lean health check.

- **Intelligence Health**  
  - Status for Weather, Calendar, AI Brain, and AI Voice.

- **API Credentials**  
  - **Gemini API Key** (AI Studio) for the briefing brain.  
  - **Google Cloud TTS API Key** for persona voice.  
  - Test API Key and Get Key buttons for both.  
  - Short explanation that both keys stay on device and are only sent to Google for those requests.

### Other

- Alarm briefing can be precomputed with Cloud TTS when enabled, then played after the ringtone for a smooth experience.

---

*For testers: after updating, go to **Settings → Intelligence** to try cloud persona voices and the new layout.*
