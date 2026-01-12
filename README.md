# GemGuard  
### Stop scrolling. Start Walking.

![GemGuard Banner](https://github.com/ItayLuria/GemGuard/blob/968fcd9296d570a2990f0afaabacb1cd4004aea2/GemGuardBanner.png)

**GemGuard** is a digital wellbeing app that bridges the gap between physical activity and screen time. By gamifying app usage limits, GemGuard motivates users to stay active by turning daily steps into the only currency that can unlock their favorite apps.

---

##  The Concept
Most app blockers just shut you out. **GemGuard** makes you earn your way back in. 
In a world of mindless scrolling, GemGuard rewards your movement. Want 15 minutes on Instagram? You’ll need to "buy" it with Gems earned through walking.

---

##  Key Features

### Gamified Economy (The Gems)
- **Step Milestones:** Earn Gems by reaching daily targets: 1k, 2.5k, 5k, 7.5k, and 10k steps.
- **Dynamic Rewards:** An intelligent algorithm adjusts Gem values. If you nailed a task yesterday, it's worth less today—pushing you to go further.
- **Flash Quests:** Random daily challenges! Walk 1k-3k steps within an hour to win a tiered Gem reward.

###  Smart App Blocker
- **Time Packages:** Purchase app access in blocks: 5, 15, 30, or 60 minutes.
- **Dynamic Pricing:** The more you use an app, the more it costs in Gems. Your habits dictate the price.
- **Automatic Enforcement:** Once time is up, the app is blocked, and you're redirected back to GemGuard to choose: Walk more or stay locked out.

###  Full Control & Security
- **Secure Setup:** Protected by a 4-digit PIN to prevent unauthorized setting changes.
- **Custom Whitelist:** Choose essential apps (Maps, Phone, etc.) that are always accessible.
- **Dark/Light Mode:** Full UI customization for the best user experience.

---

##  App Structure

| Page | Description |
| :--- | :--- |
| **Home** | The central hub. Features the **Glowing Step Ring** that lights up green when tasks are completed. |
| **Tasks** | Claim your hard-earned Gems from daily milestones and participate in Flash Quests. |
| **Shop** | Spend Gems on app time. Apps are sorted by usage—addictive apps cost more! |
| **Settings** | PIN-protected area for Whitelist management, system permissions, and UI toggles. |

---

##  Technical Implementation
To provide a seamless and unblockable experience, GemGuard utilizes Android permissions:
* **Activity Recognition:** For real-time step tracking.
* **Usage Stats:** To monitor app time and identify active packages.
* **Display Over Other Apps:** To trigger the block overlay immediately when time expires.
* **Accessibility Service:** Ensures the app cannot be easily bypassed or force-closed during protection.

---

##  Installation & Setup
1. Choose your Language (Hebrew/English).
2. Choose desired theme (Light/Dark).
3. Set your 4-digit security PIN.
4. Grant necessary permissions (Activity, Usage, Overlay, Accessibility).
---
## Note
This project was developed with **AI-assisted coding**. AI was used for brainstorming, UI/UX logic, and documentation.

### Developed by [Itay Luria] 
