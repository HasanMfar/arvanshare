## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.

## 2024-10-24 - Compose Text Field Capitalization UX Learnings
**Learning:** Adding KeyboardOptions(capitalization = KeyboardCapitalization.Sentences) to OutlinedTextFields designed for multiline or long-form content (like posts and comments) provides a smoother mobile typing experience by starting sentences with capital letters automatically.
**Action:** Always ensure appropriate capitalization configurations on text fields that accept unstructured or prose text in Compose apps.
