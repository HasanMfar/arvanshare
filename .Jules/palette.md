## 2026-08-16 - Optimize mobile keyboard flow
**Learning:** In compose-based mobile apps, specifying `KeyboardOptions` for capitalization (Sentences/Words) and imeAction (Next/Done) drastically improves form-fill speed and removes user friction when typing on touch keyboards.
**Action:** Always verify if text inputs are part of a form or expect sentences, and attach the corresponding KeyboardOptions to minimize manual typing effort.
