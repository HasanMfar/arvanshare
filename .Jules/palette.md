## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.

## 2026-09-22 - Mobile Keyboard Submission UX
**Learning:** For mobile text inputs like comment fields, relying solely on an adjacent on-screen "Send" button forces the user to awkwardly reach out of the keyboard area. The soft keyboard often covers significant screen real estate.
**Action:** When designing or refactoring chat/comment inputs in Compose, always add `KeyboardOptions(imeAction = ImeAction.Send)` and handle `KeyboardActions(onSend = { ... })` directly on the `OutlinedTextField` or `TextField`. This allows users to intuitively complete their action without dismissing the keyboard or reaching.
