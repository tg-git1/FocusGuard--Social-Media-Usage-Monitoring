package com.example.myapplication

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class OpenClawPredictor(context: Context) {
    private var tflite: Interpreter? = null

    init {
        // Load the lightweight .tflite model when the class initializes
        val model = loadModelFile(context, "focus_behavior_model.tflite")
        tflite = Interpreter(model)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    // Pass in the user's current data to get a risk prediction
    fun predictBreakdownRisk(distractionMinutes: Float, productiveMinutes: Float, timeOfDay: Float): Float {
        // 1. Format input data into a Tensor array
        val input = floatArrayOf(distractionMinutes, productiveMinutes, timeOfDay)
        val inputArray = arrayOf(input)

        // 2. Prepare the output container (assuming it returns a single risk score)
        val outputMap = Array(1) { FloatArray(1) }

        // 3. Run the model inference
        tflite?.run(inputArray, outputMap)

        // 4. Return the risk score (e.g., 0.0 to 1.0)
        return outputMap[0][0]
    }

    fun close() {
        // ALWAYS close the interpreter to prevent memory leaks
        tflite?.close()
    }
}