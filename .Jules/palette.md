## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.

## 2026-09-20 - Compose Text Field IME Action Learning
**Learning:** For single-line or multi-line text fields that immediately trigger an action (like posting a comment), configuring `KeyboardActions(onSend = { ... })` paired with `ImeAction.Send` prevents the user from having to dismiss the keyboard to find the submit button, vastly improving the flow of quick interactions. Adding sentence capitalization (`KeyboardCapitalization.Sentences`) is a highly expected baseline UX for conversational inputs.
**Action:** Audit text inputs that trigger a primary action (comments, searches, quick replies) and ensure they leverage IME actions to submit directly from the keyboard.
