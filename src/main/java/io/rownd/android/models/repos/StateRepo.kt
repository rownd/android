package io.rownd.android.models.repos

class StateRepo {
    lateinit var appConfig: AppConfigRepo
    var auth: AuthRepo = AuthRepo()

    fun start() {
        appConfig = AppConfigRepo()
    }
}