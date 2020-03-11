package com.xiongms.facedetection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.lightweh.dlib.FaceDet
import com.xiongms.libcamera2helper.CameraHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val perms = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    private var mCamera2Helper: CameraHelper? = null

    private var faceDet: FaceDet? = null

    private val handlerThread = HandlerThread("face_detection_thread")


    private val handler: Handler by lazy {
        object : Handler(handlerThread.looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.obj is Bitmap) {
                    faceDetection(msg.obj as Bitmap)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handlerThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.stop()
    }


    /**
     * 请求APP所需权限
     */
    private fun requestPermissions() {
        if (!hasPermission(perms)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(perms, 101)
            }
        }
    }

    /**
     * 权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            for (item in grantResults) {
                if (item != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "请在系统设置中允许所有权限", Toast.LENGTH_LONG).show()
                    requestPermissions()
                    return
                }
            }

            initCarema()
        }
    }

    /**
     * 判断是否有权限
     */
    private fun hasPermission(perms: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (perm in perms) {
                if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        } else {
            return true
        }
    }

    override fun onStart() {
        super.onStart()

        if (hasPermission(perms)) {
            initCarema()
        } else {
            requestPermissions()
        }
    }

    private fun initCarema() {
        mCamera2Helper = CameraHelper(this, textureView)
        mCamera2Helper!!.setOnPreviewSurfaceCallback(object :
            CameraHelper.OnPreviewSurfaceCallback {
            override fun onPreviewUpdate(bitmap: Bitmap) {
                // 实时获取摄像头预览图片
                Log.e(TAG, "获取到图片信息")

                handler.removeMessages(1)
                handler.sendMessage(handler.obtainMessage(1, bitmap))
            }
        })
    }

    /**
     * 人脸检测
     */
    private fun faceDetection(bmp: Bitmap) {

        if (faceDet == null) {
            faceDet = FaceDet()
        }
        val scale = boundingBoxView.width.toFloat() / bmp.width.toFloat()
        val faces = faceDet!!.detect(bmp)
        if (faces != null && faces.size > 0) {

            boundingBoxView.setResults(faces, scale)
        }

        Log.d("xiongms", "bmp.height=${bmp.height};bmp.width=${bmp.width};" +
                "boundingBoxView.height=${boundingBoxView.height};boundingBoxView.width=${boundingBoxView.width};" +
                "textureView.height=${textureView.height};textureView.width=${textureView.width};")

        if (!bmp.isRecycled) {
            bmp.recycle()
        }
    }
}
