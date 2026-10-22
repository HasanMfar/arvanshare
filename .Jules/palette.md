## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.
## 2026-09-11 - Improve text inputs with sentence capitalization
**Learning:** Text input fields in Compose (OutlinedTextField) need explicit `keyboardOptions` to provide the expected mobile keyboard behavior, such as automatically capitalizing the first letter of sentences.
**Action:** Always check `OutlinedTextField` elements to ensure they have appropriate `KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)` when used for long-form text (like posts or comments).
