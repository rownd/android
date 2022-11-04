package io.rownd.android;

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.Component
import io.rownd.android.util.TestApiClientModule
import org.junit.Before
import org.junit.runner.RunWith
import javax.inject.Singleton

@Singleton
@Component(modules = [TestApiClientModule::class])
interface TestRowndGraph : RowndGraph {}

@RunWith(AndroidJUnit4::class)
open class BaseInstrumentedTest {

    val rownd = RowndClient(DaggerTestRowndGraph.create())

    @Before
    open fun setup() {

    }
}
