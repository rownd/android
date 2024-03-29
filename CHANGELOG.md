

## [2.6.7](https://github.com/rownd/android/compare/2.6.6...2.6.7) (2024-02-12)


### Bug Fixes

* **users:** prevent refreshing user record when in background ([#46](https://github.com/rownd/android/issues/46)) ([59c82a6](https://github.com/rownd/android/commit/59c82a6ae0d0245723c5c7d472edb5f6b9c2480a))

## [2.6.6](https://github.com/rownd/android/compare/2.6.5...2.6.6) (2024-01-24)


### Bug Fixes

* added a null check for Context ([aa79a1c](https://github.com/rownd/android/commit/aa79a1c2700c12f2a2a72cb06d04e814e8e90e21))
* prevent google sign in hints that are not active gmail accounts ([7181bc6](https://github.com/rownd/android/commit/7181bc60bae93c7bd3738a91b74a143289df7e6c))

## [2.6.5](https://github.com/rownd/android/compare/2.6.4...2.6.5) (2024-01-23)


### Bug Fixes

* removed comma ([bcc0d8f](https://github.com/rownd/android/commit/bcc0d8f3d5f4edb802b4171fc63e3be27ca17af1))


### Features

* pass down google sign-in hint ([3416fb0](https://github.com/rownd/android/commit/3416fb014d0bdd75ee0bca07e731c082ffdcbf73))

## [2.6.4](https://github.com/rownd/android/compare/2.6.3...2.6.4) (2024-01-18)


### Bug Fixes

* **boot:** refresh token request might throw ([345b87e](https://github.com/rownd/android/commit/345b87ec0969c1b1856a3efd9b5528a2e7ffdf7d))

## [2.6.3](https://github.com/rownd/android/compare/2.6.2...2.6.3) (2023-12-13)


### Bug Fixes

* enforce dark/light background from appConfig ([#43](https://github.com/rownd/android/issues/43)) ([62aedf7](https://github.com/rownd/android/commit/62aedf759b046ca419ed3d999a872bd4ae76d41b))
* updated package-lock.json ([b64ea80](https://github.com/rownd/android/commit/b64ea8005271ddaa297355d80d91eef80b5ab930))

## [2.6.2](https://github.com/rownd/android/compare/2.6.1...2.6.2) (2023-09-22)


### Bug Fixes

* **errors:** log vs. throwing errors when ux would be degraded ([#41](https://github.com/rownd/android/issues/41)) ([cd338b4](https://github.com/rownd/android/commit/cd338b47ad1dc413b9383c78b86cb932b8415c16))

## [2.6.1](https://github.com/rownd/android/compare/2.6.0...2.6.1) (2023-09-08)

# [2.6.0](https://github.com/rownd/android/compare/2.5.0...2.6.0) (2023-06-12)


### Bug Fixes

* postSignInRedirect not recommended ([a918130](https://github.com/rownd/android/commit/a918130f51aa09dd632071eef0aa7e62bf47e621))


### Features

* **auth:** support direct sign-in as guest ([7008c47](https://github.com/rownd/android/commit/7008c470fd159816ed065b0b15340dcccb6ed3f7))
* included passkey and intent ([0aeab7d](https://github.com/rownd/android/commit/0aeab7d68e4f0c515881f3a91dae0e5f6e950185))
* **passkeys:** initial support ([#34](https://github.com/rownd/android/issues/34)) ([1fd866e](https://github.com/rownd/android/commit/1fd866e067582c270a8f658c5b656b0758a13708))

# [2.5.0](https://github.com/rownd/android/compare/2.4.2...2.5.0) (2023-02-09)


### Features

* **auth:** support sign up intent ([#31](https://github.com/rownd/android/issues/31)) ([4ddf4b9](https://github.com/rownd/android/commit/4ddf4b9a482bd53a96902dac259b7b2c60e7a522))

## [2.4.2](https://github.com/rownd/android/compare/2.4.1...2.4.2) (2023-02-09)


### Bug Fixes

* clear authenticatedApi client on signout ([#33](https://github.com/rownd/android/issues/33)) ([3efbc1c](https://github.com/rownd/android/commit/3efbc1c6a718cb43e89d419ab47599f23fdb823e))

## [2.4.1](https://github.com/rownd/android/compare/2.4.0...2.4.1) (2023-01-26)


### Bug Fixes

* added a default to the postSignInRedirect matching ios ([af0003d](https://github.com/rownd/android/commit/af0003de29f13c12bd4447ce7bf767fcd9bbf942))

# [2.4.0](https://github.com/rownd/android/compare/2.3.2...2.4.0) (2023-01-19)


### Features

* add google one tap ([#26](https://github.com/rownd/android/issues/26)) ([14321e5](https://github.com/rownd/android/commit/14321e597c59fe20041546396ebdbddf85ebfabe))
* **auth:** support third-party token exchange ([#28](https://github.com/rownd/android/issues/28)) ([91a09e0](https://github.com/rownd/android/commit/91a09e01fb21eff43e043995a005db2587f980df))