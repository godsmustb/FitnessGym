# 03 - Market, Competitors and User Complaints (Fitness / Strength App, as of 2026-10-04)

## 0. Method and honesty notes

- Sources: WebSearch/WebFetch of review sites, vendor blogs, app-store aggregators, Trustpilot, Stronger by Science.
- **Reddit was NOT accessible** (the search tool and fetcher both block reddit.com). Therefore **no Reddit thread URLs or quotes are given**. Section 2 evidence comes from Trustpilot, App Store aggregator pages (justuseapp / appsupports) and review blogs. Reddit-sourced signals are listed as "(unverified, needs manual Reddit pass)" and should be validated by hand in r/fitness, r/weightroom, r/naturalbodybuilding, r/homegym, r/xxfitness, r/bodyweightfitness.
- Many pricing/feature numbers come from competitor-run blogs (sensai.fit, alphaprogression.com, pocket-fit, boostcamp.app/vs). These have commercial bias and sometimes disagree (noted inline). Treat as indicative; re-verify in the stores before pricing decisions.
- Quotes are in quotation marks only where the fetched page returned them as quotes; everything else is paraphrased.

## 1. Competitor matrix

### 1.1 Summary table

| App | Model / price (2026, indicative) | Equipment-awareness | Exercise media | Logging UX | Progression logic | AI | Platforms | Notable strength |
|---|---|---|---|---|---|---|---|---|
| **Hevy** | Freemium: Pro $2.99/mo, $23.99/yr, $74.99 lifetime. Free: unlimited logging, 4 routines, 7 custom exercises, 3 months graph history, no ads ([sensai pricing](https://www.sensai.fit/blog/fitness-app-pricing-free-tier-comparison)) | Equipment filter on library; no real multi-gym profile generation (unverified) | Video/GIF library | Clean, social feed, native supersets, plate calc, warm-up, rest timer, CSV export | Mostly manual; "Hevy Trainer" program builder ([sensai](https://www.sensai.fit/blog/hevy-vs-strong-2026)) | Minimal | iOS, Android, Apple Watch (limited), Wear OS | Price, generous free tier, social, ~4.9 star rating |
| **Strong** | Freemium: Pro $4.99/mo, $29.99/yr, $99.99 lifetime; free caps routines at 3 | Basic | Library with demos | Considered fastest/cleanest barbell logger; best Apple Watch standalone logging ([findyouredge](https://www.findyouredge.app/news/best-strength-training-apps-2026), [sensai](https://www.sensai.fit/blog/hevy-vs-strong-2026)) | Manual only | None | iOS, Android, Apple Watch | Speed, simplicity |
| **Fitbod** | Subscription: $15.99/mo, $95.99/yr (another source: $12.99/$79.99); 3 free workouts / 7-day trial, card required | Strong: equipment sets, generates around what you have | Video demos, muscle-recovery map | Auto-generated sets/reps/weight, tap to log | Algorithmic muscle-fatigue/recovery model | ML ("AI") | iOS, Android, Apple Watch | Zero-planning workouts, variety |
| **JEFIT** | Free w/ ads; Elite $6.99-$12.99/mo, $39.99-$69.99/yr (sources disagree) | Equipment filters; community routines | Large library (1,400+), animated images, muscle diagrams | Dense, dated UI per reviewers | Community templates; limited auto | Light | iOS, Android, watch (paywalled) | Biggest library, community routines |
| **Alpha Progression** | Free tier + Pro $12.99/mo, $79.99/yr; 14-day trial ([alphaprogression](https://alphaprogression.com/en/blog/best-ai-strength-training-apps)) | Yes: equipment-aware suggestions, muscle priorities | 795 curated exercises with videos | Data-dense, steep learning curve | Set-level progression, periodization, auto deloads | Rule-based (explicitly not generative AI) | iOS, Android | Best-in-class hypertrophy progression logic |
| **Boostcamp** | Free + Pro $14.99/mo or $59.99/yr (7-day trial); another list says $14.99-$39.99/yr | Limited | Videos for programs | Tracks program prescriptions, shows next-session targets | Coach program templates (Nippard, Nuckols, GZCL) | No | iOS, Android | Free library of famous programs |
| **StrongLifts 5x5** | $4.99/wk, $11.99/mo, $59.99/yr, $199.99 lifetime; 7-day trial on yearly only | None | Videos | Single-program simplicity | Linear; plateaus after 3-6 months | No | iOS, Android | Beginner clarity |
| **RP Hypertrophy** | $34.99/mo list ($24.99 sale), ~$225-300/yr; 30-day guarantee; no free tier | Equipment setup, exercise swaps | 250+ technique videos | Set tracking, soreness/pump feedback | Auto-regulated mesocycles, volume by recovery feedback | Rules/algorithm | iOS, Android | Israetel brand; science-based volume; expensive |
| **Nippard (Jeff Nippard Fitness App / programs)** | Programs free on Boostcamp (e.g., [Powerbuilding 1.0](https://www.boostcamp.app/users/vqyooY-jeff-nippard-powerbuilding-system-10)); search results also describe an app tracking weight, reps, RIR, partial reps, supersets, dropsets with 3-angle demo videos (details/pricing unverified) | Unverified | Multi-angle video | RIR + partials | Adjusts to performance/fatigue (per listing, unverified) | Unverified | iOS (unverified) | Creator credibility; his technique videos also power MacroFactor Workouts |
| **MacroFactor Workouts** | $11.99/mo, $71.99/yr, 7-day trial | Multiple gym profiles | Nippard technique videos | RIR tracker | "Smart Progression", rule-based | Explicitly rule-based | iOS (Android unverified) | Adaptive targets from the nutrition-app team |
| **Ladder** | PRO $29.99/mo-$179.99/yr up to ELITE+ $49.99/mo-$479.99/yr ([sensai](https://www.sensai.fit/blog/ladder-app-review-2026)) | Program per equipment | Video | Coach-led team programs | Coach-written | Some personalization | iOS, Android | Polished programs, coach teams |
| **Future** | $199/mo ($149/mo prepaid yearly; $50 first month) | Per coach | Video | Coach-delivered | Human coach | Light | iOS (Watch centric) | Human accountability |
| **Caliber** | Free; Plus ~$12/mo; Pro ~$19/mo; coaching from ~$200/mo ([garagegymreviews](https://www.garagegymreviews.com/best-workout-apps)) | Yes (gym/home) | Video | Logging with coach feedback | Coach/algorithm | Light | iOS, Android | Value hybrid of human + app |
| **Gymshark Training** | Free content + premium (price unverified) | Home/gym plans | Video | Basic | Fixed plans | No | iOS, Android | Athlete-led programs, challenges |
| **Nike Training Club** | Free (190+ workouts per [aggregator](https://aitoolsbakery.com/blog/best-free-workout-apps/)) | Bodyweight/minimal | Coach-led video | Not a logger | No overload tracking ([findyouredge](https://www.findyouredge.app/news/best-strength-training-apps-2026)) | No | iOS, Android | Free, polished video |
| **Apple Fitness+** | $10/mo or $80/yr | Minimal | Studio video | Via Watch | None | No | Apple only | Ecosystem |
| **Freeletics** | ~$94.99/yr, $549.99 lifetime; weekly-billing plans exist | Bodyweight focus | Video; AI-coach workouts | Guided | Adaptive plan | "AI Coach" | iOS, Android | Bodyweight/HIIT |
| **MuscleWiki** | Free core (library offline); Premium $4.99/mo, $49.99/yr ([App Store listing](https://apps.apple.com/uy/app/musclewiki-workout-fitness/id1096827640)) | Equipment filter | **Video + interactive body map (muscle highlighting)**; 1,700+ exercises | Basic log | Basic | "AI personal trainer" claim | iOS, Android, web | Closest to the muscle-highlight form-demo idea |
| **Liftosaur** | Free, open-source; optional $4.99/mo, $39.99/yr, $99.99 lifetime | Manual | Basic | Scriptable programs | Fully programmable | No | iOS, Android, web | Power users, transparency |
| **Gravl** (rising) | $14.99/mo, ~$60-90/yr; 3 free workouts | Yes: rounds to your plates/dumbbells/stack, custom equipment, multiple gym profiles, camera equipment recognition ([sensai](https://www.sensai.fit/blog/gravl-app-review-2026)) | 300+ offline trainer videos | Fast; plate calculator | Adaptive; weekly volume capped by session duration | **AI form check (Feb 2026), import programs from link/PDF/photo** | iOS, Android, Apple Watch, Wear OS, Garmin | Most complete 2026 AI strength app; ~4.9 star (5.5K iOS) |
| **JuggernautAI** (niche) | $34.99/mo, $349.99/yr | Powerlifting | 300+ videos | RPE + readiness | Autoregulated powerlifting | Algorithmic | iOS, Android | Meet prep |
| **BodBot** (AI) | Unverified | Yes | Unverified | **Camera mode (beta): AI counts reps, coaches form** | Adaptive | Yes | Android/iOS | Hands-free camera logging |
| **Demotu x VASA Fitness** (Apr 2026) | Gym membership bundle | Gym-specific | - | Trainer-programmed | AI-assisted | Yes | iOS/Android | Gym chains embedding AI ([VASA](https://vasafitness.com/press/vasa-fitness-launches-industry-first-personal-training-app-in-hvlp-category-redefining-coaching-at-scale/)) |

### 1.2 "OpenGym" - what it is

"OpenGym" is **not one commercial brand**; several small indie/open-source projects share the name:

1. **openGym (self-hosted)** by Duarte Santos: AGPL-3.0, `docker compose up`, web app installable as PWA. Weekly plans, searchable exercise library with **animated demos**, preloaded previous weights, PR detection, rest timers, configurable progression rules (linear, Greyskull LP, double progression), supersets, timed/cardio exercises, body-weight tracking and e1RM, passkey auth, **offline + sync**. Sources: [alexjenkins.tech](https://alexjenkins.tech/apps/opengym/), [GitLab](https://gitlab.com/DuarteSantos8/opengym).
2. **OpenGym: Cross-platform Gym** (norrdev): exercises, planner, workout mode with rest timer, log, powerlifting calculators; no registration, no tracking/telemetry ([Google Play](https://play.google.com/store/apps/details?id=pro.filonov.npng&hl=en), [GitHub](https://github.com/norrdev/OpenGym)).

Takeaway: the privacy-first / self-hosted / offline lane exists but is hobbyist-grade; it validates demand for local-first and no-account use.

### 1.3 Matrix takeaways

- Pure loggers (Hevy, Strong) win on speed and price; they do not program.
- Generators (Fitbod, Gravl) win on zero-effort programming but draw the harshest complaints about logic quality and billing.
- Science programmers (Alpha Progression, RP, MacroFactor) win on credibility; cost/complexity is their weak point.
- Muscle-highlight media exists (MuscleWiki, Fitbod recovery map) but is not integrated with logging and equipment profiles in one flow.

## 2. Top 10 user complaints (ranked by frequency x severity)

Evidence limits: Trustpilot samples are small (Fitbod 37 reviews, ~3.0-3.1; JEFIT 5 reviews, 2.5). App-store ratings are high for Hevy/Gravl (4.8-4.9), so complaints there are feature gaps, not outrage. Ranking is my synthesis; Reddit frequency was not measured.

### 1. Trial-to-paid billing traps, hard cancellation, no refunds
- Apps: Fitbod, JEFIT, Freeletics-style weekly billing.
- Evidence: Trustpilot Fitbod: "My free trial rolled straight into a A$149.99 annual charge ... No reminder before it hit" (Matthew Armstrong, Sep 2026); another reviewer says they tried "literally everything to cancel" (Jun 2026); users note the Apple store prevents refunds ([Trustpilot Fitbod](https://www.trustpilot.com/review/www.fitbod.me)). JEFIT Trustpilot: a reviewer says they had to cancel their payment card to stop VIP charges ([Trustpilot JEFIT](https://www.trustpilot.com/review/www.jefit.com), via search summary). The FTC acted against fitness apps MadMuscles, Harna, Unimeal ([consumerfinancemonitor](https://www.consumerfinancemonitor.com/2026/07/01/ftc-takes-action-to-halt-allegedly-deceptive-subscription-schemes/)).
- Our answer: no-card trial; reminder 48h before any renewal; in-app one-tap cancel/manage; real free tier for logging.

### 2. Paywalled core features and history gating
- Apps: Hevy (3-month graph history, 4 routines, 7 custom exercises), Strong (3 routines), Fitbod (read-only after trial), JEFIT (watch app paywalled), Gravl (3 workouts).
- Evidence: Hevy App Store reviewers: "Free version locks progress history beyond a certain time period"; "need to pay 80 dollars for the full version" ([justuseapp Hevy](https://justuseapp.com/en/app/1458862350/hevy-workout-tracker-gym-log/reviews)). Free-tier specs: [sensai](https://www.sensai.fit/blog/fitness-app-pricing-free-tier-comparison).
- Our answer: never lock the user's own data; paywall conveniences (AI generation, advanced analytics), not history, custom exercises, or export.

### 3. Dumb / repetitive / illogical auto-generated workouts
- Apps: Fitbod mainly; generative apps broadly.
- Evidence: Trustpilot Fitbod: "exercises are repetitive no matter how you change the settings" (Jul 2026); "Getting the same exact exercises" from a 5-year user (Apr 2025). Store negatives: "It obsesses over certain exercises that are non sensical, even when you ask it to suggest it less"; a warm-up recommended as "5 x 30kg ... That's not a warmup set!" ([appsupports Fitbod](https://appsupports.co/1041517543/fitbod-workout-fitness-plans/negative-reviews)).
- Our answer: deterministic, explainable rules (rank by target muscle / equipment / fatigue with variation constraints); never-show / pin / swap controls; show "why this exercise / this weight".

### 4. Data loss, crashes, sync failures (incl. watch sync)
- Apps: Fitbod, MuscleWiki (white screen), Gravl Android (freezes), Hevy (occasional sync).
- Evidence: Fitbod store reviewer claims the app "will at least once a week forgot whole exercises, whole sets"; un-clearable error; Apple Watch sync failures ([appsupports](https://appsupports.co/1041517543/fitbod-workout-fitness-plans/negative-reviews)). Trustpilot: "keeps crashing" (May 2025). MuscleWiki: "just doesn't work in my phone... white screen" ([justuseapp MuscleWiki](https://justuseapp.com/en/app/1096827640/musclewiki-workout-fitness/reviews)). Gravl freezes on Android ([sensai](https://www.sensai.fit/blog/gravl-app-review-2026), search summary).
- Our answer: local-first SQLite as source of truth, persist each set immediately, crash-safe in-progress workout, background sync with conflict resolution.

### 5. Logging friction (too many taps, no "next" cursor, cannot update routine mid-workout)
- Apps: Hevy, JEFIT, Fitbod.
- Evidence: Hevy reviewers: "no 'Next' button to automatically move cursor to succeeding input box"; "no option to save & update routine if you changed an exercise mid workout" ([justuseapp Hevy](https://justuseapp.com/en/app/1458862350/hevy-workout-tracker-gym-log/reviews)). JEFIT: constant UI changes, more steps to change muscle groups/exercises ([etechshout](https://etechshout.com/jefit-app-review/)).
- Our answer: prefilled previous set, one-tap "same as last", stepper chips, auto-advance, "update template with changes?" at finish.

### 6. Rest timer / watch reliability
- Apps: Hevy (timer does not sound outside the app; Watch needs the phone for most entry), Fitbod watch sync.
- Evidence: [prpath Hevy review](https://prpath.app/blog/hevy-app-review-2026.html) (search summary); Strong's watch app is fuller ([sensai](https://www.sensai.fit/blog/hevy-vs-strong-2026)); a report that Garmin rejected Hevy's API access is (unverified, single blog).
- Our answer: native notification / Live Activity / foreground-service timer; standalone watch logging (Apple Watch + Wear OS) in a later phase.

### 7. Overpriced AI / coaching apps for perceived value
- Apps: Fitbod ("Price is WAAAAY too high... Huge rip off"), RP Hypertrophy ($34.99/mo), Ladder, Future ($199/mo), JuggernautAI ($349/yr).
- Evidence: appsupports Fitbod review above; pricing in section 1.
- Our answer: price the logger low (Hevy/Strong anchor $24-30/yr, lifetime $75-100); AI tier only where it demonstrably saves effort.

### 8. Poor or absent customer support
- Apps: Fitbod chiefly.
- Evidence: Trustpilot "I issued a ticket... they never replied" (May 2026); store reviewer: "Paid for pro version but am still locked out. No reply from support after 7 days" ([appsupports](https://appsupports.co/1041517543/fitbod-workout-fitness-plans/negative-reviews)). One reviewer also alleges some positive store reviews are written by employees (unverified, single reviewer).
- Our answer: in-app support with a stated SLA, working restore-purchases, honest review prompts (after a completed session, no sentiment gating).

### 9. Equipment / exercise gaps and weak customization
- Apps: Fitbod (hard to exclude exercises, settings reset), MuscleWiki (bodyweight filter, missing muscle regions), Nike TC (no overload), Gravl (limited cardio/variety).
- Evidence: appsupports Fitbod ("Settings reset unexpectedly", difficult to exclude exercises); MuscleWiki reviewer: "I want to do workouts with my body weight but the don't show up" ([justuseapp](https://justuseapp.com/en/app/1096827640/musclewiki-workout-fitness/reviews)); [Gravl summary](https://www.sensai.fit/blog/gravl-app-review-2026).
- Our answer: first-class equipment profiles (Home, Garage, Commercial, Hotel), per-item increments (plate, dumbbell, stack steps), user-created equipment and exercises, "busy gym" swap button.

### 10. No recovery/readiness awareness; adaptation only to logged sets; linear plateaus
- Apps: Hevy, Strong, Gravl, StrongLifts.
- Evidence: neither Hevy nor Strong "reads recovery data ... HRV, sleep, soreness, or training load" ([sensai](https://www.sensai.fit/blog/hevy-vs-strong-2026)); Gravl adapts only to logged sets/effort; StrongLifts linear plateaus after 3-6 months ([findyouredge](https://www.findyouredge.app/news/best-strength-training-apps-2026)).
- Our answer: optional soreness/readiness check-in, HealthKit/Health Connect ingestion, deload suggestions.

**Complaint signals NOT verified from primary sources (check Reddit manually):** unwanted social feeds (Hevy store reviews say the social part feels unnecessary), ad intrusion in free JEFIT, inability to export data, inconsistent form-video quality, women-specific programming and equipment-light options (r/xxfitness, r/homegym; unverified), bodyweight progression chains handled poorly (r/bodyweightfitness; unverified).

## 3. Practitioner perspective

Limitation: I could not retrieve direct, quotable statements from Nippard, Israetel, Ethier, Nuckols, Natacha Oceane, Nalewanyj, Athlean-X, Hybrid Calisthenics or Squat University about "what makes a good app". Verifiable signals first, then labelled synthesis.

**Verified signals**
- **Nippard**: programs distributed free on Boostcamp ([example](https://www.boostcamp.app/users/vqyooY-jeff-nippard-powerbuilding-system-10)); his technique videos power MacroFactor Workouts including RIR tracking ([App Store](https://apps.apple.com/us/app/macrofactor-workouts-tracker/id6737156524)). Signal: credible creators want logging + RIR + multi-angle demo video, delivered inside existing apps.
- **Israetel / RP**: RP Hypertrophy bakes in volume landmarks, mesocycles, soreness/pump feedback, exercise swaps, 250+ technique videos ([Boostcamp comparison](https://www.boostcamp.app/vs/rp-hypertrophy)). Signal: autoregulation via recovery feedback is the "expert" feature set.
- **Nuckols / Stronger by Science**: autoregulation does not require RPE; lifters can pick loads for a target RPE with good accuracy ([autoregulation article](https://www.strongerbyscience.com/autoregulation/)). In one study, 12 of 20 preferred predetermined rep targets and 8 preferred RIR-based; reasons paraphrased: "I knew what I was supposed to do" vs "It felt more tailored to my body" ([SBS spotlight](https://www.strongerbyscience.com/research-spotlight-autoregulation/)). Implication: offer both fixed and autoregulated modes.
- **Squat University**: no app-specific statements found. Inference: form cues should be injury-aware, short, and focus on common faults.

**Practitioner-derived checklist (my synthesis; standard across top apps and reviewers, not attributed quotes)**

| Need | Why it matters | Who does it well |
|---|---|---|
| Fewest taps: prefill last session, auto-advance | Logging in a busy gym | Strong, Hevy |
| Rest timer with background notification, per-exercise defaults | Core ritual | Strong, Hevy (background gap) |
| Supersets / drop sets | Hypertrophy programming | Hevy native |
| Warm-up set generator | Safe ramp | Hevy, Strong; Fitbod criticized for bad warm-ups |
| Plate calculator, custom bar/plate inventory | Barbell work | Hevy, Strong, Gravl |
| RPE/RIR + partial reps | Autoregulation | MacroFactor, Alpha, Nippard app |
| Progressive overload suggestions with reasoning | Core AI value | Alpha, Gravl, MacroFactor |
| PR tracking (weight, reps, e1RM, volume) | Motivation | Most |
| Busy-gym substitution (same muscle, available equipment) | Real-world need | Fitbod, Gravl (partial) |
| Home vs commercial equipment profiles | Home-gym users | Gravl, MacroFactor, Alpha |
| Form cues + multi-angle video | Safety | Nippard-style 3-angle, MuscleWiki |
| Offline use | Basement gyms | MuscleWiki, openGym |
| Apple Watch / Wear OS | Phone-free sets | Strong (best Apple), Gravl (both) |
| Data export / import (CSV) | Trust, no lock-in | Hevy, Strong |
| Recovery inputs (HRV/sleep/soreness) | Better load decisions | Gap (section 5) |

## 4. Monetization norms and dark patterns

**Price bands (indicative)**
- Logger tier: $24-30/yr or ~$75-100 lifetime (Hevy, Strong, Liftosaur). Users happily pay here (Hevy ~4.9 stars with a lifetime option).
- AI/programming tier: $60-96/yr ($12-16/mo) (Fitbod, Alpha, Gravl, MacroFactor).
- Expert-brand programming: $25-35/mo (RP), $350/yr (Juggernaut).
- Human coaching: $150-200+/mo (Future, Caliber premium).
- Free video: Nike Training Club; Apple Fitness+ $80/yr.

**What users happily pay for:** low-priced lifetime unlocks; a free tier with unlimited logging; ad removal; analytics and programming that visibly save time; human accountability where the service is real.

**Dark patterns to avoid**
1. Card-required trial auto-converting to annual with no reminder (Fitbod Trustpilot).
2. Paywall after 3 workouts with no persistent free value.
3. Locking existing history behind a subscription.
4. Cancel-flow friction; weekly billing disguised as low price ([Reviewed summary](https://www.reviewed.com/health/best-right-now/the-best-workout-apps)).
5. Ads interrupting active sets (free JEFIT).
6. Review manipulation or sentiment gating.
7. Regulatory: the FTC click-to-cancel rule was vacated in July 2025, but enforcement under other laws continues ([consumerfinancemonitor](https://www.consumerfinancemonitor.com/2026/07/01/ftc-takes-action-to-halt-allegedly-deceptive-subscription-schemes/), [cookie-script](https://cookie-script.com/privacy-laws/dark-patterns-2026-the-ftc-new-click-to-cancel-rule)). Treat health data as sensitive ([summary](https://newagesysit.com/blog/ftc-guidelines-app-store-health-data-rules-for-fitness-platforms-in-the-united-states/)).

**Suggested for us:** Free forever = unlimited logging, custom exercises, full history, export. Plus (~$3-4/mo, ~$25-30/yr, ~$75 lifetime) = unlimited routines, analytics. Coach/AI tier (~$6-10/mo) = generation, form analysis. Trial: 7 days, no card, reminder before any conversion.

## 5. Gaps / white space (2026)

1. **Animated form demo + targeted-muscle highlighting in the same view as the logging row.** MuscleWiki has body maps, Nippard has multi-angle video, but nobody fuses an animated/3D model, primary/secondary muscle highlight, cues, common faults and the set logger on one screen (verify by hands-on test).
2. **Equipment awareness at the increment level** (dumbbell steps, plate inventory, cable stack increments, fractional plates, bands) across several named locations. Gravl is closest.
3. **Transparent, explainable progression** ("why 62.5 kg x 8") with user override and a choice of fixed targets vs RIR autoregulation (the SBS study shows a split preference).
4. **Reliable offline-first, no-account mode with export.** Only hobby projects (openGym) offer it; big apps have sync bugs.
5. **Busy-gym live swap** respecting available equipment, remaining time and weekly volume balance.
6. **Recovery-informed load decisions without wearable lock-in** (soreness check-in + optional HealthKit/Health Connect); Hevy/Strong ignore it.
7. **Home-gym, women-specific and bodyweight progression** (themes from r/homegym, r/xxfitness, r/bodyweightfitness; unverified here).
8. **Honest billing as a differentiator:** "no card trial, one-tap cancel, you keep your data".
9. **Trustworthy camera form check.** Gravl (video analysis) and BodBot (camera mode, beta) are early; accuracy unverified. Opportunity: coach-written rule-based cues first, CV later.
10. **Wear OS parity.** Gravl is the only major app with Wear OS; Strong lacks it.

## 6. Key sources

- Pricing/free tiers: https://www.sensai.fit/blog/fitness-app-pricing-free-tier-comparison
- Hevy vs Strong: https://www.sensai.fit/blog/hevy-vs-strong-2026
- AI strength app comparison: https://alphaprogression.com/en/blog/best-ai-strength-training-apps
- Gravl: https://www.sensai.fit/blog/gravl-app-review-2026
- Fitbod Trustpilot: https://www.trustpilot.com/review/www.fitbod.me
- Fitbod negative store reviews: https://appsupports.co/1041517543/fitbod-workout-fitness-plans/negative-reviews
- Hevy store reviews: https://justuseapp.com/en/app/1458862350/hevy-workout-tracker-gym-log/reviews
- MuscleWiki reviews: https://justuseapp.com/en/app/1096827640/musclewiki-workout-fitness/reviews
- openGym: https://alexjenkins.tech/apps/opengym/
- SBS autoregulation study: https://www.strongerbyscience.com/research-spotlight-autoregulation/
- FTC actions: https://www.consumerfinancemonitor.com/2026/07/01/ftc-takes-action-to-halt-allegedly-deceptive-subscription-schemes/

## 7. Follow-ups needed

- Manual Reddit pass (subreddits above) to rank complaints by real frequency and add thread URLs.
- Verify Nippard app existence/pricing, Gymshark Training pricing, Future/Caliber from first-party pages.
- Hands-on test of Gravl, MuscleWiki and Alpha Progression (media quality, taps per set).
