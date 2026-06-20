# Task 18 Report — LoginView and LayoutView

**Status:** Completed

## Files Created
- `package-manager-web/src/views/LoginView.vue` — Login form with `el-card`, gradient background, form validation, and API call via `@/utils/request`.
- `package-manager-web/src/views/LayoutView.vue` — Layout shell with sidebar navigation (3 items: dashboard, checkin, packages), header with admin label and logout button, and `<router-view>` for child routes.

## Verification
- `npx vue-tsc --noEmit` passed with zero errors.

## Dependencies
- Consumes `@/utils/request` (Task 17) for API calls.
- Consumes `vue-router` for navigation.
- Consumes Element Plus for UI components.
