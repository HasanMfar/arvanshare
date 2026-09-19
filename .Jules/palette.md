## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.

## 2026-08-26 - Compose Textfield UX Learnings
**Learning:** Implementing the 'Send' IME action using KeyboardOptions and KeyboardActions in Jetpack Compose dramatically improves the user experience by enabling users to submit text directly from the software keyboard without tapping an external send button.
**Action:** Whenever a single-line or multi-line text input serves as a direct input for a fast-action like sending a message or comment, configure the IME action to 'Send' and handle the submission logic inside 'onSend' of KeyboardActions.
