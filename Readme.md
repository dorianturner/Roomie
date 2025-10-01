# Roomie - Finding flatmates made easy

This is a mobile Android app that helps students find compatible flatmates and suitable properties using Tinder-style swiping, group matching and relevance-ranked listings.

## Summary

1. Prevents bad flatshare matches by matching users on preferences before property discovery.
2. Supports group formation and merging between groups - merges are deliberate and block discovery of other groups while in progress to encourage careful decisions.
3. Once a group is finalised, the group sees property listings ranked by the group’s combined preference score.
4. If a group opens a chat with a landlord, Roomie counts that as a generated lead for the landlord.

## Key features

1. Preference scoring: Preferences are combined into a simple numerical score to rank listings by fit.
2. Swipe & match: Individual users swipe on potential flatmates / groups of flatmates and form groups based on mutual matches.
3. Group merging: Two or more groups can merge; during a merge participants cannot discover other groups so as to prevent "two-timing".
4. Finalised-group listings: Finalised groups unlock property listings shown in order of relevance to the group’s aggregate preferences.
5. Landlord chat / lead generation: Opening chat with a landlord produces a lead; the app’s role ends after lead creation.

## How it works

- Sign up and set preferences (budget, location, habits, non-negotiables).
- Swipe on candidate  group profiles
- Propose or accept merges with other groups; merges are atomic - while merging you cannot browse other groups.
- When the group finalises members, property listings become available, ordered by the group’s preference score.
- Group reviews listings; opening chat with a landlord creates a lead and starts landlord communication.

## Developer notes

Platform: Android (Kotlin/Java/Typescript) using Firebase.
Typical steps to run locally:

1. Open the project in Android Studio.
2. Configure an Android SDK and an emulator or device.
3. Build & run the app module.
