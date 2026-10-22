## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.

## 2024-05-20 - Android Compose Keyboard UX
**Learning:** In Android Jetpack Compose, the default text field behavior doesn't auto-capitalize sentences or trigger submission via the keyboard's return key. Users expect these features on mobile.
**Action:** Use `KeyboardOptions` for auto-capitalization and `KeyboardActions` for handling the IME Send action to instantly submit input without reaching for an explicit button.
