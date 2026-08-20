## 2024-05-18 - Jetpack Compose Icon Accessibility
**Learning:** In Android Jetpack Compose, when an `Icon` is placed immediately adjacent to a descriptive `Text` element, the `Icon` should have `contentDescription = null`. Providing a description for the icon in this scenario causes redundant and annoying screen reader announcements.
**Action:** Always check the context of an icon. Only provide a `contentDescription` if the icon is standalone (e.g., in an `IconButton` without text) and conveys unique meaning.
