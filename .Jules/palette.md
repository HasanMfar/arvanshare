## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.
## 2026-08-26 - Compose TextField Keyboard Actions
**Learning:** Adding KeyboardOptions (like ImeAction.Send) and mapping them with KeyboardActions (onSend) to an OutlinedTextField allows users to trigger a form submission directly from the keyboard without breaking interaction flow.
**Action:** When implementing single-line or short text inputs meant for immediate submission (like a comment box), utilize ImeAction.Send and tie the form submission logic to the KeyboardActions onSend callback for a more seamless UX.
