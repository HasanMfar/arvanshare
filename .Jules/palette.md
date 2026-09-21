## 2026-08-25 - Compose Form UX Learnings
**Learning:** Adding KeyboardOptions (like ImeAction.Next and ImeAction.Done) to sequential OutlinedTextFields dramatically improves the form-filling experience on mobile by allowing users to progress through fields using the keyboard's bottom-right button instead of manually tapping each field. Contextual keyboard types (e.g., KeyboardType.Uri) also help.
**Action:** When auditing forms in Jetpack Compose, always check if fields are logically linked and add proper KeyboardOptions to create a smooth, continuous flow.

## 2026-09-21 - Compose TextField Keyboard Actions
**Learning:** Adding KeyboardActions with ImeAction.Send to comment inputs lets users submit their comments directly from their device's virtual keyboard, skipping the step to manually press an on-screen "Send" button. Additionally, utilizing KeyboardCapitalization.Sentences guarantees that users' messages automatically start with a capitalized letter.
**Action:** When working on text inputs for forms or chat applications in Android, leverage KeyboardActions and capitalization settings to create a smoother mobile typing experience.
