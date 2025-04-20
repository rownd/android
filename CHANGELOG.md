

## [3.1.1](https://github.com/rownd/android/compare/3.1.0...3.1.1) (2025-04-20)


### Bug Fixes

* **instant:** option to force instant users to add an identity ([#64](https://github.com/rownd/android/issues/64)) ([a221760](https://github.com/rownd/android/commit/a2217600782da7f6c1b1da30b6a6a815c333261c))

# [3.1.0](https://github.com/rownd/android/compare/3.0.2...3.1.0) (2025-04-15)


### Features

* support for android instant apps ([#63](https://github.com/rownd/android/issues/63)) ([3a4a9e4](https://github.com/rownd/android/commit/3a4a9e40f3f1d591d1dc4ba576f70b61acfadac4))

## [3.0.2](https://github.com/rownd/android/compare/3.0.1...3.0.2) (2025-04-01)


### Bug Fixes

* **auth:** prevent crash when handling sign-in links ([7d667b0](https://github.com/rownd/android/commit/7d667b07309538ef839e56b50726d55d925d6654))

## [3.0.1](https://github.com/rownd/android/compare/3.0.0...3.0.1) (2025-03-31)


### Bug Fixes

* light/dark mode handling; use a map for event payloads ([#62](https://github.com/rownd/android/issues/62)) ([14ce94d](https://github.com/rownd/android/commit/14ce94d2f72e1f47e286a85094b7890f389dc34e))

# [3.0.0](https://github.com/rownd/android/compare/2.15.1...3.0.0) (2025-03-31)


### Bug Fixes

* play services improvements; network client enhancements; bottom sheet behaviors ([#61](https://github.com/rownd/android/issues/61)) ([3b7160d](https://github.com/rownd/android/commit/3b7160d548f11cd3343ad5d01ea9674425836f1e))

## [2.15.1](https://github.com/rownd/android/compare/2.15.0...2.15.1) (2025-03-22)


### Bug Fixes

* **state:** prevent crash when state is unparseable ([#60](https://github.com/rownd/android/issues/60)) ([fc25b6b](https://github.com/rownd/android/commit/fc25b6b838b04c6cb174739ea9924572cd0bdcd4))

# [2.15.0](https://github.com/rownd/android/compare/2.14.0...2.15.0) (2025-02-12)


### Features

* support for signing a user out of all sessions ([#58](https://github.com/rownd/android/issues/58)) ([031cc00](https://github.com/rownd/android/commit/031cc00d2f96611d7338fd6dcc10633fa02bd093))

# [2.14.0](https://github.com/rownd/android/compare/2.13.3...2.14.0) (2025-01-17)

## [2.13.3](https://github.com/rownd/android/compare/2.13.2...2.13.3) (2024-11-14)


### Bug Fixes

* **pkg:** add missing minification rules ([f320ba6](https://github.com/rownd/android/commit/f320ba65e134b03829711461253383d55e53f884))
* **store:** regression/crash caused by updates to androidx.datastore ([b680c70](https://github.com/rownd/android/commit/b680c70c1b6fdf96ffd7e9ae390db89cd48d6253))

## [2.13.2](https://github.com/rownd/android/compare/2.13.1...2.13.2) (2024-11-08)


### Bug Fixes

* **pkg:** include minification rules in aar ([340fd4c](https://github.com/rownd/android/commit/340fd4cc34c014693b7d8705c2c323c117a24029))

## [2.13.1](https://github.com/rownd/android/compare/2.13.0...2.13.1) (2024-11-07)


### Bug Fixes

* **pkg:** bad telemetry package ref ([ef5e9cf](https://github.com/rownd/android/commit/ef5e9cf3dcef6474d51fcbd7f690ac7ec8578da2))

# [2.13.0](https://github.com/rownd/android/compare/2.12.1...2.13.0) (2024-10-29)


### Bug Fixes

* **google:** handle supervised users not supported by credential manager ([60e629a](https://github.com/rownd/android/commit/60e629a6a65385982a756fe025b81debb9861d5e))
* **otel:** update aar build approach ([4c50d2a](https://github.com/rownd/android/commit/4c50d2af114d5c1573a49d2617ed057bba3fbba9))


### Features

* **otel:** initial telemetry support ([809a748](https://github.com/rownd/android/commit/809a74888db6e835bad8aa07050a4693708e23ae))

## [2.12.1](https://github.com/rownd/android/compare/2.12.0...2.12.1) (2024-08-21)


### Bug Fixes

* **google:** handle case where user has disabled google sign-in prompts ([b46c687](https://github.com/rownd/android/commit/b46c687da4a2e3f9bd41a6cb36b275d27ce34bef))

# [2.12.0](https://github.com/rownd/android/compare/2.10.1...2.12.0) (2024-08-02)


### Bug Fixes

* bump version number ([87f3ff9](https://github.com/rownd/android/commit/87f3ff92a83426a9ab04280ee1c5016d18a0ac84))
* **hub:** show correct page after multiple loads ([2632c19](https://github.com/rownd/android/commit/2632c1902eb56238c3263a7c238595ac2eb34d11))
* **hub:** stale sheet load ([ea8ae2c](https://github.com/rownd/android/commit/ea8ae2c645bd8bc4ed80df8769f7c09b29939746))
* lowercase passkey api classes ([c3b57ca](https://github.com/rownd/android/commit/c3b57cab5febac59475c08565e886255786afd1a))


### Features

* add authenticated check for passkey registration ([c821632](https://github.com/rownd/android/commit/c821632d0ca8aa3ec880c1ac08b1957f73dd4a0d))
* **auth:** update deps to use androidx.credentialmanager ([#55](https://github.com/rownd/android/issues/55)) ([ba39172](https://github.com/rownd/android/commit/ba39172b1803d3730858e5c2877b1620b3cd9efd))
* improve passkey sdk apis ([4637567](https://github.com/rownd/android/commit/463756750bb5909cce2091b6fdfba697888be4ea))

## [2.10.1](https://github.com/rownd/android/compare/2.10.0...2.10.1) (2024-06-25)


### Bug Fixes

* add user api for get and set ([33c5ee7](https://github.com/rownd/android/commit/33c5ee751071a5040573dc60c518be7dbb40ac55))

# [2.10.0](https://github.com/rownd/android/compare/2.9.1...2.10.0) (2024-06-25)


### Features

* add is loading param for user domain state ([#51](https://github.com/rownd/android/issues/51)) ([9356b7b](https://github.com/rownd/android/commit/9356b7b64c6d462ec96f7f72bd75018edbfb2d15))

## [2.9.1](https://github.com/rownd/android/compare/2.9.0...2.9.1) (2024-05-30)


### Bug Fixes

* **events:** bad event names and user types in serialization ([8e5a090](https://github.com/rownd/android/commit/8e5a090c1a9634cd8ee4c04960f1dc413b7b071b))

# [2.9.0](https://github.com/rownd/android/compare/2.8.0...2.9.0) (2024-05-25)


### Bug Fixes

* **build:** support for android api 26+ ([87d8e6c](https://github.com/rownd/android/commit/87d8e6c176528cca2ff6b519050b7abcd7cc85e0))


### Features

* **events:** emit events that occur during the auth lifecycle ([#50](https://github.com/rownd/android/issues/50)) ([4fa8b89](https://github.com/rownd/android/commit/4fa8b89ebfb8347a75345c41d8d61863f12ce209))

# [2.8.0](https://github.com/rownd/android/compare/2.7.0...2.8.0) (2024-04-25)


### Bug Fixes

* add mutablestateof to remember { class } ([bff0b27](https://github.com/rownd/android/commit/bff0b27415042236f9df4567e4d2244969e1f6a9))


### Features

* add shimmy to bottom sheet ([#48](https://github.com/rownd/android/issues/48)) ([d28e89c](https://github.com/rownd/android/commit/d28e89c7c7910b0ce83e0280d648ae97d546d434))
* suppress RememberReturnType lint rule ([fef54ea](https://github.com/rownd/android/commit/fef54ea71197125a86096cf30504cd0276ee7c84))

# [2.7.0](https://github.com/rownd/android/compare/2.6.7...2.7.0) (2024-04-11)

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