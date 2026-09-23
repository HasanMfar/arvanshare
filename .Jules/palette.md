## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.

## 2026-09-23 - Compose Keyboard Actions
**Learning:** In Jetpack Compose, binding `KeyboardActions(onSend = { ... })` to `ImeAction.Send` provides a much better mobile UX by allowing users to submit forms/comments directly from the keyboard instead of needing to reach for a separate on-screen button. It's crucial to apply the same validation (e.g., checking if the input is not blank and not currently submitting) to the `KeyboardActions` as the main submit button to prevent unexpected duplicate or empty submissions.
**Action:** When adding text inputs for immediate actions like commenting or searching, always configure both `KeyboardOptions(imeAction)` and `KeyboardActions` alongside the submit button to unify the submission paths.
