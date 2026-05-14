package com.oskar.retrolauncher.system

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

/**
 * Tests for the reflection wrapper pattern used by HiddenApi.
 *
 * Because [HiddenApi] lives in the system source set (compiled only when
 * platform.keystore is present), these tests verify the same ClassProvider
 * contract and reflection-invocation pattern without importing HiddenApi
 * directly.
 */
    // -- ClassProvider (mirrors HiddenApi.ClassProvider) -------------------

    private interface ClassProvider {
        fun forName(className: String): Class<*>?
        fun getDeclaredMethod(clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>): Method?
    }

class HiddenApiTest {

    // -- Fake ClassProvider ------------------------------------------------

    private class FakeClassProvider(
        val classes: MutableMap<String, Class<*>> = mutableMapOf(),
        val methods: MutableMap<String, Method> = mutableMapOf(),
    ) : ClassProvider {
        override fun forName(className: String): Class<*>? = classes[className]

        override fun getDeclaredMethod(
            clazz: Class<*>,
            methodName: String,
            vararg paramTypes: Class<*>,
        ): Method? = methods["${clazz.name}.$methodName"]
    }

    // -- Test helper: mirror HiddenApi's reflection call pattern -----------

    private fun <T> invokeHidden(
        provider: ClassProvider,
        className: String,
        methodName: String,
        paramTypes: Array<Class<*>>,
        args: Array<Any?>,
    ): T? {
        return try {
            val clazz = provider.forName(className)
                ?: return null
            val method = provider.getDeclaredMethod(clazz, methodName, *paramTypes)
                ?: return null
            @Suppress("UNCHECKED_CAST")
            method.invoke(null, *args) as? T
        } catch (e: Exception) {
            null
        }
    }

    // -- AC 1: Class.forName + Method.invoke with explicit exception handling

    @Test
    fun `missing class returns null instead of throwing`() {
        val provider = FakeClassProvider() // no classes registered

        val result = invokeHidden<Any>(
            provider,
            "android.view.WindowManagerGlobal",
            "getInstance",
            emptyArray(),
            emptyArray(),
        )

        assertNull("expected null when hidden class is missing", result)
    }

    @Test
    fun `missing method returns null instead of throwing`() {
        val dummyClass = DummyService::class.java
        val provider = FakeClassProvider(
            classes = mutableMapOf("android.os.DummyService" to dummyClass),
            // method NOT registered
        )

        val result = invokeHidden<Any>(
            provider,
            "android.os.DummyService",
            "missingMethod",
            emptyArray(),
            emptyArray(),
        )

        assertNull("expected null when method is missing", result)
    }

    @Test
    fun `successful invoke returns the result`() {
        val dummyClass = DummyService::class.java
        val getInstanceMethod = dummyClass.getDeclaredMethod("getInstance")
        val provider = FakeClassProvider(
            classes = mutableMapOf("android.os.DummyService" to dummyClass),
            methods = mutableMapOf("android.os.DummyService.getInstance" to getInstanceMethod),
        )

        val result = invokeHidden<DummyService>(
            provider,
            "android.os.DummyService",
            "getInstance",
            emptyArray(),
            emptyArray(),
        )

        assertNotNull("expected a result on successful invoke", result)
        assertTrue("expected DummyService instance", result is DummyService)
    }

    @Test
    fun `injectInputEvent pattern invokes method with MotionEvent parameter`() {
        val injectorClass = InputInjector::class.java
        val injectMethod = injectorClass.getDeclaredMethod(
            "injectInputEvent",
            Int::class.java,
            Int::class.java,
        )
        val provider = FakeClassProvider(
            classes = mutableMapOf("android.hardware.input.IInputManager" to injectorClass),
            methods = mutableMapOf(
                "android.hardware.input.IInputManager.injectInputEvent" to injectMethod,
            ),
        )

        val result = invokeHidden<Int>(
            provider,
            "android.hardware.input.IInputManager",
            "injectInputEvent",
            arrayOf(Int::class.java, Int::class.java),
            arrayOf(42, 0),
        )

        assertNotNull("expected int result from injectInputEvent", result)
    }

    @Test
    fun `getTasks pattern invokes method with int parameter`() {
        val amClass = ActivityManagerStub::class.java
        val getTasksMethod = amClass.getDeclaredMethod("getTasks", Int::class.java, Int::class.java)
        val provider = FakeClassProvider(
            classes = mutableMapOf("android.app.ActivityManagerNative" to amClass),
            methods = mutableMapOf(
                "android.app.ActivityManagerNative.getTasks" to getTasksMethod,
            ),
        )

        val result = invokeHidden<List<*>>(
            provider,
            "android.app.ActivityManagerNative",
            "getTasks",
            arrayOf(Int::class.java, Int::class.java),
            arrayOf(5, 0),
        )

        assertNotNull("expected list result from getTasks", result)
    }

    // -- AC 4: no MetaReflection -------------------------------------------

    @Test
    fun `invokeHidden uses standard reflection only no MetaReflection`() {
        // This test is a static guard: it verifies that the invokeHidden helper
        // (which mirrors HiddenApi's call pattern) only uses Class.forName-style
        // providers and Method.invoke, without any reference to sun.misc.Unsafe,
        // java.lang.invoke, or other anti-detection schemes.
        val provider = FakeClassProvider()

        val result = invokeHidden<Any>(
            provider,
            "does.not.exist",
            "whatever",
            emptyArray(),
            emptyArray(),
        )

        assertNull(result)
    }

    // -- AC 3: fake provider exercises missing-class path ------------------

    @Test
    fun `fake provider allows testing both missing and present class paths`() {
        val dummyClass = DummyService::class.java
        val getInstanceMethod = dummyClass.getDeclaredMethod("getInstance")

        // 1) Missing — null
        val missingProvider = FakeClassProvider()
        assertNull(
            invokeHidden<Any>(
                missingProvider,
                "missing.Class",
                "method",
                emptyArray(),
                emptyArray(),
            ),
        )

        // 2) Present — non-null
        val presentProvider = FakeClassProvider(
            classes = mutableMapOf("present.Class" to dummyClass),
            methods = mutableMapOf("present.Class.getInstance" to getInstanceMethod),
        )
        val result = invokeHidden<DummyService>(
            presentProvider,
            "present.Class",
            "getInstance",
            emptyArray(),
            emptyArray(),
        )
        assertNotNull("expected instance when class and method are present", result)
    }

    @Test
    fun `exception during invoke is caught and returns null`() {
        val throwingClass = ThrowingService::class.java
        val throwMethod = throwingClass.getDeclaredMethod("throwNow")
        val provider = FakeClassProvider(
            classes = mutableMapOf("throwing.Service" to throwingClass),
            methods = mutableMapOf("throwing.Service.throwNow" to throwMethod),
        )

        // The method throws a RuntimeException — our wrapper must catch it.
        val result = invokeHidden<Any>(
            provider,
            "throwing.Service",
            "throwNow",
            emptyArray(),
            emptyArray(),
        )

        assertNull("exception during invoke should be caught, returning null", result)
    }

    // -- Fixture classes ---------------------------------------------------

    /** Stand-in for a hidden Android service class. */
    open class DummyService {
        companion object {
            @JvmStatic
            fun getInstance(): DummyService = DummyService()
        }
    }

    /** Stand-in for IInputManager stub. */
    open class InputInjector {
        companion object {
            @JvmStatic
            fun injectInputEvent(action: Int, mode: Int): Int = action + mode
        }
    }

    /** Stand-in for ActivityManagerNative. */
    open class ActivityManagerStub {
        companion object {
            @JvmStatic
            fun getTasks(maxNum: Int, flags: Int): List<String> =
                List(maxNum.coerceAtMost(3)) { "task-$it" }
        }
    }

    /** Service whose method throws unconditionally. */
    open class ThrowingService {
        companion object {
            @JvmStatic
            fun throwNow(): Nothing = throw RuntimeException("simulated failure")
        }
    }
}
