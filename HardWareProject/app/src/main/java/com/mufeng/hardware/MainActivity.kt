package com.mufeng.hardware

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    val TAG: String = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dir = getDir("libs", Context.MODE_PRIVATE)
        setPrvLibPath(dir.absolutePath)
        // Example of a call to a native method
        val tv = findViewById(R.id.sample_text) as TextView
        tv.text = stringFromJNI()

        try {
            //            IKeystoreService keystore = IKeystoreService.Stub.asInterface(ServiceManager
            //                    .getService("android.security.keystore"));

            val clazzServiceManager = Class.forName("android.os.ServiceManager")
            // val getServiceMethod = clazzServiceManager.getDeclaredMethod("getService", String.javaClass)
            //val obj = getServiceMethod.invoke(null, "android.security.keystore")
            // Log.d("TAG", "field name: " + obj.javaClass.name)
            val methodsServiceManager = clazzServiceManager.declaredMethods
            for (methodServiceManager in methodsServiceManager) {
                if (methodServiceManager.name.equals("getService")) {
                    val obj: IBinder = methodServiceManager.invoke(null, "android.security.keystore") as IBinder
                    val clazz = Class.forName("android.security.IKeystoreService\$Stub")
                    val methods = clazz.declaredMethods
                    for (method in methods) {
                        if (method.name.equals("asInterface")) {
                            val objKeyStore = method.invoke(null, obj)
                            LogClassDeclared(objKeyStore.javaClass.name)
                        }
                    }
                }
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    val libNameArray = arrayOf("audio_policy.default.so", "audio.primary.msm8998.so", "bluetooth.default.so")
    val libLoadArray = arrayOf("audio_policy", "audio.primary", "bluetooth")
    val libPathArray = arrayOf("/system/lib/hw", "/vendor/lib/hw", "/odm/lib/hw")
    val libSysPathArray = arrayOf("/system/lib", "/vendor/lib", "/odm/lib")

    fun onQureyModule(view: View) {
        val dir = getDir("libs", Context.MODE_PRIVATE)
        checkLibraries(dir)
        for (name in libLoadArray) {
            nativeQueryHardware(dir, name)
        }
    }

    private fun nativeQueryHardware(dir: File, name: String) {
        queryHardWare(name)
        val error = dlerror()
        if (TextUtils.isEmpty(error)) {
            Log.d(TAG, "nativeQueryHardware no dlerror")
        } else {
            Log.d(TAG, "dlerror: " + error)
            val errors = error.split("\"")
            if (errors.size > 1) {
                val libName = errors[1]
                writeSysLibToApp(dir, libName)
                nativeQueryHardware(dir, name)
            }
        }
    }

    private fun writeSysLibToApp(dir: File, libName: String) {
        var success = false
        val app = File(dir, libName)
        if (app.exists()) {
            loadExternalLib(app, dir)
            Log.d(TAG, "loadExternalLib exist")
        } else {
            for (path in libSysPathArray) {
                val sysDir = File(path, libName)
                if (sysDir.exists()) {
                    writeFile(sysDir, app)
                    success = true
                    break
                }
            }
            if (success) {
                loadExternalLib(app, dir)
            }
        }
    }

    private fun loadExternalLib(app: File, dir: File) {
        try {
            System.load(app.absolutePath)
        } catch (e: UnsatisfiedLinkError) {
            Log.d(TAG, "UnsatisfiedLinkError: " + e.message)
            val message: String = e.message!!
            val errors = message.split("\"")
            if (errors.size > 1) {
                writeSysLibToApp(dir, errors[1])
            }
        }
    }

    fun checkLibraries(dir: File) {
        for (name in libNameArray) {
            val app = File(dir, name)
            if (app.exists()) {
                val msg = String.format("Path: %s, Size: %d", app.absolutePath, app.length())
                Log.d(TAG, msg)
            } else {
                for (path in libPathArray) {
                    val sysDir = File(path, name)
                    if (sysDir.exists()) {
                        writeFile(sysDir, app)
                        break
                    }
                }
            }
        }
    }

    private fun writeFile(sysDir: File, app: File) {
        val inStream = FileInputStream(sysDir)
        val writer = FileOutputStream(app)
        val buf = ByteArray(1024)
        while (inStream.read(buf) != -1) {
            writer.write(buf)
        }
        inStream.close()
        writer.flush()
        writer.close()
    }

    private fun LogClassDeclared(className: String) {
        val msg = StringBuilder()
        msg.append("Class: ")
        msg.append(className)
        msg.append(";\n Field: ")
        val clazz = Class.forName(className)
        val fields = clazz.declaredFields
        for (field in fields) {
            msg.append(field.name)
            msg.append(":")
            msg.append(field.type.name)
            msg.append("; \n")
        }
        msg.append(";\n Method: ")
        val methods = clazz.declaredMethods
        for (method in methods) {
            msg.append(method.name)
            msg.append(":")
            msg.append(method.returnType.name)
            msg.append("; \n")
        }
        msg.append(";\n Classes: ")
        val classes = clazz.declaredClasses
        for (class1 in classes) {
            msg.append(class1.name)
            msg.append(", ")
        }
        msg.append(";\n")
        Log.d("TAG", msg.toString())
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    external fun dlerror(): String

    external fun setPrvLibPath(absoluteFile: String)

    external fun queryHardWare(libName: String)

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("app")
        }
    }
}
