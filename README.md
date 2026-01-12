# GemGuard 
### Stop scrolling. Start walking.

![GemGuard Banner](https://github.com/ItayLuria/GemGuard/blob/968fcd9296d570a2990f0afaabacb1cd4004aea2/GemGuardBanner.png)

GemGuard is an Android app that limits screen time by requiring physical activity to unlock apps. Instead of just blocking apps, it turns your daily steps into "Gems" which you use to buy time packages for specific apps.

---

## How it works
The app locks chosen applications behind a paywall of Gems. To get Gems, you have to move.
If you want to spend 15 minutes on a social media app, you’ll need to complete step milestones or timed challenges first.

### Main Features
* **Step Milestones:** Earn Gems at 1k, 2.5k, 5k, 7.5k, and 10k steps. 
* **Dynamic Economy:** The reward for a milestone drops if you completed it yesterday, and rises if you haven't—forcing you to stay consistent.
* **Flash Quests:** Random timed challenges (e.g., walk 2000 steps in an hour) for bonus Gems.
* **Usage-Based Pricing:** The shop automatically sorts apps by usage. Apps you use the most cost more Gems to unlock.
* **Strict Blocking:** Once your purchased time is up, the app closes automatically and redirects you to the GemGuard dashboard.

---

## App Structure

* **Home:** Dashboard with a central step counter ring that glows green when a goal is reached.
* **Tasks:** Where you claim Gems from your daily steps and view active Flash Quests.
* **Shop:** Purchase 5, 15, 30, or 60-minute sessions for your blocked apps.
* **Settings:** PIN-protected area to manage the Whitelist and app permissions.

---

## Setup & Tech Stack
To function correctly, GemGuard requires specific Android permissions:
* **Activity Recognition** (Step tracking)
* **Usage Stats** (Monitoring app time)
* **Display Over Other Apps** (Overlay for the blocking mechanism)
* **Accessibility Service** (Preventing bypass and ensuring the lock stays active)

### Initial Setup
1. Select Language & Theme.
2. Set a 4-digit security PIN.
3. Grant required system permissions.
4. Select which apps to block/whitelist.

---

## Note on Development
This project was developed with **AI-assisted coding**. AI was used for architectural brainstorming, UI/UX logic, and documentation.

---

### Developed by Itay Luria
