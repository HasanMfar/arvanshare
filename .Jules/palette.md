## 2025-01-20 - Jetpack Compose Text Field Keyboard Navigation
**Learning:** In Android Jetpack Compose, forms do not automatically navigate to the next field when pressing 'Next' or 'Enter' on the mobile keyboard by default.
**Action:** Always explicitly specify `KeyboardOptions` with `imeAction = ImeAction.Next` for sequential fields and `imeAction = ImeAction.Done` for the final field to ensure smooth mobile keyboard navigation.
