## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.
## 2026-09-17 - Compose Keyboard Actions
**Learning:** Users expect standard mobile keyboard behavior (like a Send button instead of Enter) for chat/comment inputs, which requires explicit wiring in Compose.
**Action:** Always configure KeyboardOptions and KeyboardActions for text fields to match their context (e.g., Send for comments, Done for standard forms, Next for multi-step).
