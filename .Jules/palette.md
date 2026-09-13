## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.
## 2026-09-13 - Enhance Mobile Keyboard UX in Jetpack Compose
**Learning:** Relying solely on physical buttons for form submission can be frustrating on mobile devices, where users expect the soft keyboard's "Send" or "Done" actions to submit the form. Additionally, failing to auto-capitalize the first letter in text fields (like comments or posts) leads to poor UX.
**Action:** Always implement `KeyboardOptions` and `KeyboardActions` for Jetpack Compose `OutlinedTextField` inputs to streamline form flow (e.g., using `ImeAction.Send` or `ImeAction.Done`) and enforce capitalization (`KeyboardCapitalization.Sentences`).
