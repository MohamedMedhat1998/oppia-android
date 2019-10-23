package org.oppia.util.profile

import android.app.Application
import android.content.Context
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.oppia.util.data.AsyncResult
import org.oppia.util.logging.EnableConsoleLog
import org.oppia.util.logging.EnableFileLog
import org.oppia.util.logging.GlobalLogLevel
import org.oppia.util.logging.LogLevel
import org.oppia.util.threading.BackgroundDispatcher
import org.oppia.util.threading.BlockingDispatcher
import org.robolectric.annotation.Config
import java.io.File
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

/** Tests for [DirectoryManagementUtil]. */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DirectoryManagementUtilTest {

  @Inject
  lateinit var directoryManagementUtil: DirectoryManagementUtil


  @Inject
  lateinit var context: Context

  // https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/
  @ObsoleteCoroutinesApi
  private val testThread = newSingleThreadContext("TestMain")

  @Before
  @ExperimentalCoroutinesApi
  @ObsoleteCoroutinesApi
  fun setUp() {
    Dispatchers.setMain(testThread)
    setUpTestApplicationComponent()
  }

  @After
  @ExperimentalCoroutinesApi
  @ObsoleteCoroutinesApi
  fun tearDown() {
    Dispatchers.resetMain()
    testThread.close()
  }

  private val TEST_DIRECTORY_1 = "TEST_DIRECTORY_1"
  private val TEST_DIRECTORY_2 = "TEST_DIRECTORY_2"
  private val TEST_FILE_1 = "TEST_FILE_1"
  private val TEST_FILE_2 = "TEST_FILE_2"

  private fun setUpTestApplicationComponent() {
    DaggerDirectoryManagementUtilTest_TestApplicationComponent.builder()
      .setApplication(ApplicationProvider.getApplicationContext())
      .build()
      .inject(this)
  }

  @Test
  fun testUtil_createDir_checkDirExists() {
    directoryManagementUtil.getOrCreateDir(TEST_DIRECTORY_1)

    val dir = File(getAbsoluteDirPath(TEST_DIRECTORY_1))

    assertThat(dir.exists()).isTrue()
  }

  @Test
  fun testUtil_createDir_getSameDir_checkDirsEquality() {
    val dir = directoryManagementUtil.getOrCreateDir(TEST_DIRECTORY_1)

    val sameDir = directoryManagementUtil.getOrCreateDir(TEST_DIRECTORY_1)

    assertThat(dir).isEqualTo(sameDir)
  }

  @Test
  fun testUtil_createDir_createDifferentDir_checkDirsNotEqual() {
    val dir = directoryManagementUtil.getOrCreateDir(TEST_DIRECTORY_1)

    val diffDir = directoryManagementUtil.getOrCreateDir(TEST_DIRECTORY_2)

    assertThat(dir).isNotEqualTo(diffDir)
  }

  @Test
  fun testUtil_createDir_restartApplication_checkDirExists() {
    directoryManagementUtil.getOrCreateDir(TEST_DIRECTORY_1)

    setUpTestApplicationComponent()

    val dir = File(getAbsoluteDirPath(TEST_DIRECTORY_1))
    assertThat(dir.exists()).isTrue()
  }


  @Test
  fun testUtil_createDir_deleteDir_checkDirDoesNotExist() {
    directoryManagementUtil.getOrCreateDir(TEST_DIRECTORY_1)

    val success = directoryManagementUtil.deleteDir(TEST_DIRECTORY_1)

    val dir = File(getAbsoluteDirPath(TEST_DIRECTORY_1))
    assertThat(dir.exists()).isFalse()
    assertThat(success).isTrue()
  }

  @Test
  fun testUtil_createDir_createFilesUnderDir_deleteDir_checkDirDoesNotExist() {
    val dir = directoryManagementUtil.getOrCreateDir(TEST_DIRECTORY_1)
    val file1 = File(dir, TEST_FILE_1)
    file1.createNewFile()
    val file2 = File(dir, TEST_FILE_2)
    file2.createNewFile()

    val success = directoryManagementUtil.deleteDir(TEST_DIRECTORY_1)

    assertThat(dir.exists()).isFalse()
    assertThat(success).isTrue()
  }

  private fun getAbsoluteDirPath(path: String): String {
    return context.filesDir.toString().dropLast(5) + "app_" + path
  }

  @Qualifier
  annotation class TestDispatcher

  // TODO(#89): Move this to a common test application component.
  @Module
  class TestModule {
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
      return application
    }

    @ExperimentalCoroutinesApi
    @Singleton
    @Provides
    @TestDispatcher
    fun provideTestDispatcher(): CoroutineDispatcher {
      return TestCoroutineDispatcher()
    }

    @Singleton
    @Provides
    @BackgroundDispatcher
    fun provideBackgroundDispatcher(@TestDispatcher testDispatcher: CoroutineDispatcher): CoroutineDispatcher {
      return testDispatcher
    }

    @Singleton
    @Provides
    @BlockingDispatcher
    fun provideBlockingDispatcher(@TestDispatcher testDispatcher: CoroutineDispatcher): CoroutineDispatcher {
      return testDispatcher
    }

    // TODO(#59): Either isolate these to their own shared test module, or use the real logging
    // module in tests to avoid needing to specify these settings for tests.
    @EnableConsoleLog
    @Provides
    fun provideEnableConsoleLog(): Boolean = true

    @EnableFileLog
    @Provides
    fun provideEnableFileLog(): Boolean = false

    @GlobalLogLevel
    @Provides
    fun provideGlobalLogLevel(): LogLevel = LogLevel.VERBOSE
  }

  // TODO(#89): Move this to a common test application component.
  @Singleton
  @Component(modules = [TestModule::class])
  interface TestApplicationComponent {
    @Component.Builder
    interface Builder {
      @BindsInstance
      fun setApplication(application: Application): Builder

      fun build(): TestApplicationComponent
    }

    fun inject(directoryManagementUtilTest: DirectoryManagementUtilTest)
  }
}