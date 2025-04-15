package io.rownd.android.di.module

//@Module
//class FakeAuthRepoModule {
//    @Provides fun provideTokenApi(): TokenApiClient = FakeTokenApi()
//    @Provides fun provideAuthApi(): AuthApi = FakeAuthApi()
//    @Provides fun provideStateRepo(): StateRepo = FakeStateRepo()
//    @Provides fun provideUserRepo(): UserRepo = FakeUserRepo()
//    @Provides fun provideSignInRepo(): SignInRepo = FakeSignInRepo()
//    @Provides fun provideRowndContext(): RowndContext = FakeRowndContext()
//
//    @Provides
//    fun provideAuthRepo(
//        rowndContext: RowndContext,
//        authApi: AuthApi,
//        stateRepo: StateRepo,
//        userRepo: UserRepo,
//        signInRepo: SignInRepo,
//        tokenApiClient: TokenApiClient
//    ): AuthRepo {
//        val authRepo = AuthRepo()
//        authRepo.rowndContext = rowndContext
//        authRepo.authApi = authApi
//        authRepo.stateRepo = stateRepo
//        authRepo.userRepo = userRepo
//        authRepo.signInRepo = signInRepo
//        authRepo.tokenApiClient = tokenApiClient
//        return authRepo
//    }
//}