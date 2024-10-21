package com.example.florascan

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.nio.ByteOrder

class MainPage : AppCompatActivity() {
    private lateinit var loadBtn: Button
    private lateinit var takeBtn: Button
    private lateinit var predictBtn: Button
    private lateinit var imageView: ImageView
    private lateinit var flowerNameTextView: TextView

    private lateinit var tflite: Interpreter

    private var currentBitmap: Bitmap? = null // Store the currently loaded bitmap

    private val NUM_CLASSES = 5 // Replace this with the actual number of flower categories in your model

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                currentBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                imageView.setImageBitmap(currentBitmap)
            } catch (e: IOException) {
                Log.e("MainPage", "Error loading image from gallery", e)
            }
        }
    }

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            currentBitmap = bitmap
            imageView.setImageBitmap(currentBitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        enableEdgeToEdge()
        supportActionBar?.hide()

        loadBtn = findViewById(R.id.loadbtn)
        takeBtn = findViewById(R.id.takeBtn)
        predictBtn = findViewById(R.id.predictBtn)
        imageView = findViewById(R.id.imageView)
        flowerNameTextView = findViewById(R.id.flowerNameTextView)

        // Full-screen mode
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        loadBtn.setOnClickListener {
            openGallery()
        }

        takeBtn.setOnClickListener {
            openCamera()
        }

        predictBtn.setOnClickListener {
            // Trigger prediction only if there is a current bitmap
            currentBitmap?.let { bitmap ->
                predictFlower(bitmap)
            } ?: run {
                flowerNameTextView.text = "Please load an image first."
            }
        }

        // Load the TensorFlow Lite model
        tflite = Interpreter(loadModelFile("flower_classification_model.tflite")) // Update with your model's name
    }

    private fun openGallery() {
        galleryResultLauncher.launch("image/*")
    }

    private fun openCamera() {
        cameraResultLauncher.launch(null)
    }

    private fun predictFlower(bitmap: Bitmap) {
        // Use the original bitmap without resizing
        imageView.setImageBitmap(bitmap)

        // Convert the original image to ByteBuffer
        val inputBuffer = convertBitmapToByteBuffer(bitmap)

        // Assuming the output is a float array with size equal to the number of classes
        val outputArray = Array(1) { FloatArray(NUM_CLASSES) }

        // Run inference
        tflite.run(inputBuffer, outputArray)

        // Get the predicted flower name
        val predictedIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] } ?: -1
        flowerNameTextView.text = getFlowerName(predictedIndex)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // Assuming your model still requires a fixed input size (for example, 150x150)
        val width = 150 // Use the actual input width required by your model
        val height = 150 // Use the actual input height required by your model

        val byteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3) // 4 bytes per float, 3 channels (RGB)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Resize the bitmap to match the model's input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val intValues = IntArray(width * height)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        // Convert the image pixels to floats in the range [0, 1]
        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16 and 0xFF) / 255.0f)) // Red channel
            byteBuffer.putFloat(((pixelValue shr 8 and 0xFF) / 255.0f))  // Green channel
            byteBuffer.putFloat(((pixelValue and 0xFF) / 255.0f))        // Blue channel
        }

        return byteBuffer
    }



    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getFlowerName(index: Int): String {
        return when (index) {
            0 -> "Rose"
            1 -> "Sunflower"
            2 -> "Tulip"
            3 -> "Dandelion"
            4 -> "Daisy"
            // Add other flower names here
            else -> "Unknown Flower"
        }
    }

    private fun checkCameraPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }
    }

    companion object {
        const val CAMERA_REQUEST_CODE = 100
    }
}
