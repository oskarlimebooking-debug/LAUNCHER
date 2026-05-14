package com.oskar.retrolauncher.system

import android.util.Log
import android.view.MotionEvent

/**
 * Reflection wrappers for hidden Android platform APIs.
 *
 * Each helper accesses a hidden class through [Class.forName] + [Method.invoke],
 * wrapped in an explicit try/catch with a [Log.w] fallback. Every method logs
 * its hidden-API dependency at call time.
 *
 * This file compiles only under the `system` flavor; it is excluded from
 * `standard` builds when `platform.keystore` is absent.
 */
object HiddenApi {
    private const val TAG = "HiddenApi"

    // -- Injectable ClassProvider (swapped in tests) -----------------------

    internal var classProvider: ClassProvider = RealClassProvider

    interface ClassProvider {
        fun forName(className: String): Class<*>?
        fun getDeclaredMethod(
            clazz: Class<*>,
            methodName: String,
            vararg paramTypes: Class<*>,
        ): java.lang.reflect.Method?
        fun getMethod(
            clazz: Class<*>,
            methodName: String,
            vararg paramTypes: Class<*>,
        ): java.lang.reflect.Method?
    }

    private object RealClassProvider : ClassProvider {
        override fun forName(className: String): Class<*>? =
            try {
                Class.forName(className)
            } catch (_: ClassNotFoundException) {
                null
            }

        override fun getDeclaredMethod(
            clazz: Class<*>,
            methodName: String,
            vararg paramTypes: Class<*>,
        ): java.lang.reflect.Method? =
            try {
                clazz.getDeclaredMethod(methodName, *paramTypes)
            } catch (_: NoSuchMethodException) {
                null
            }

        override fun getMethod(
            clazz: Class<*>,
            methodName: String,
            vararg paramTypes: Class<*>,
        ): java.lang.reflect.Method? =
            try {
                clazz.getMethod(methodName, *paramTypes)
            } catch (_: NoSuchMethodException) {
                null
            }
    }

    // -- Public helpers ----------------------------------------------------

    /**
     * Returns the [android.hardware.display.DisplayManager] system service,
     * accessed via [android.view.WindowManagerGlobal] reflection.
     */
    fun getDisplayManager(): android.hardware.display.DisplayManager? {
        Log.w(TAG, "HiddenApi dependency: WindowManagerGlobal")
        return try {
            val wmgClass = classProvider.forName("android.view.WindowManagerGlobal")
                ?: return null.also {
                    Log.w(TAG, "WindowManagerGlobal class not found")
                }
            val getInstanceMethod = classProvider.getMethod(wmgClass, "getInstance")
                ?: return null.also {
                    Log.w(TAG, "WindowManagerGlobal.getInstance method not found")
                }
            val instance = getInstanceMethod.invoke(null)

            val getWmsMethod = classProvider.getDeclaredMethod(
                instance?.javaClass ?: wmgClass,
                "getWindowManagerService",
            ) ?: return null.also {
                Log.w(TAG, "getWindowManagerService method not found")
            }
            getWmsMethod.invoke(instance) as? android.hardware.display.DisplayManager
        } catch (e: Exception) {
            Log.w(TAG, "Failed to access WindowManagerGlobal", e)
            null
        }
    }

    /**
     * Injects a [MotionEvent] into the system input queue via the hidden
     * [android.hardware.input.IInputManager] AIDL interface.
     *
     * @return true if the event was injected successfully.
     */
    fun injectInputEvent(event: MotionEvent): Boolean {
        Log.w(TAG, "HiddenApi dependency: IInputManager")
        try {
            val smClass = classProvider.forName("android.os.ServiceManager")
                ?: return false.also {
                    Log.w(TAG, "ServiceManager class not found")
                }
            val getServiceMethod = classProvider.getMethod(
                smClass,
                "getService",
                String::class.java,
            ) ?: return false.also {
                Log.w(TAG, "ServiceManager.getService method not found")
            }
            val binder = getServiceMethod.invoke(null, "input") as? android.os.IBinder
                ?: return false.also {
                    Log.w(TAG, "input service binder not available")
                }

            val stubClass = classProvider.forName(
                "android.hardware.input.IInputManager\$Stub",
            ) ?: return false.also {
                Log.w(TAG, "IInputManager\$Stub class not found")
            }
            val asInterfaceMethod = classProvider.getMethod(
                stubClass,
                "asInterface",
                android.os.IBinder::class.java,
            ) ?: return false.also {
                Log.w(TAG, "IInputManager\$Stub.asInterface method not found")
            }
            val inputManager = asInterfaceMethod.invoke(null, binder)
                ?: return false.also {
                    Log.w(TAG, "IInputManager asInterface returned null")
                }

            val injectMethod = classProvider.getDeclaredMethod(
                inputManager.javaClass,
                "injectInputEvent",
                MotionEvent::class.java,
                Int::class.java,
            ) ?: return false.also {
                Log.w(TAG, "injectInputEvent method not found")
            }
            injectMethod.invoke(inputManager, event, 0)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inject input event", e)
            false
        }
    }

    /**
     * Returns the list of recent tasks via the hidden
     * [android.app.ActivityManagerNative] class.
     */
    fun getRunningTasks(maxNum: Int): List<android.app.ActivityManager.RunningTaskInfo>? {
        Log.w(TAG, "HiddenApi dependency: ActivityManagerNative")
        try {
            val amnClass = classProvider.forName("android.app.ActivityManagerNative")
                ?: return null.also {
                    Log.w(TAG, "ActivityManagerNative class not found")
                }
            val getDefaultMethod = classProvider.getMethod(amnClass, "getDefault")
                ?: return null.also {
                    Log.w(TAG, "ActivityManagerNative.getDefault method not found")
                }
            val am = getDefaultMethod.invoke(null)
                ?: return null.also {
                    Log.w(TAG, "ActivityManagerNative.getDefault returned null")
                }

            val getTasksMethod = classProvider.getDeclaredMethod(
                am.javaClass,
                "getTasks",
                Int::class.java,
                Int::class.java,
            ) ?: return null.also {
                Log.w(TAG, "getTasks method not found")
            }

            @Suppress("UNCHECKED_CAST")
            return getTasksMethod.invoke(am, maxNum, 0)
                as? List<android.app.ActivityManager.RunningTaskInfo>
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get running tasks", e)
            null
        }
    }
}
