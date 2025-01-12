import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class KeyPointClassifier(
    modelPath: String,
    numThreads: Int = 1
) {
    private val interpreter: Interpreter
    private val inputShape: IntArray
    private val outputShape: IntArray

    init {
        val options = Interpreter.Options().apply {
            setNumThreads(numThreads)
        }
        interpreter = Interpreter(loadModelFile(modelPath), options)
        inputShape = interpreter.getInputTensor(0).shape() // Input tensor shape
        outputShape = interpreter.getOutputTensor(0).shape() // Output tensor shape
    }

    operator fun invoke(landmarkList: List<Float>): Int {
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputShape[1]).apply {
            order(ByteOrder.nativeOrder())
            for (value in landmarkList) {
                putFloat(value)
            }
        }

        val outputBuffer = ByteBuffer.allocateDirect(4 * outputShape[1]).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter.run(inputBuffer, outputBuffer)

        // Extract the results from the output buffer
        outputBuffer.rewind()
        val results = FloatArray(outputShape[1]) { outputBuffer.float }

        // Get the index of the maximum value
        return results.indices.maxByOrNull { results[it] } ?: -1
    }

    private fun loadModelFile(modelPath: String): ByteBuffer {
        val assetFileDescriptor = javaClass.classLoader!!.getResourceAsStream(modelPath)
            ?: throw IllegalArgumentException("Model file not found: $modelPath")

        val fileBytes = assetFileDescriptor.readBytes()
        return ByteBuffer.allocateDirect(fileBytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(fileBytes)
            rewind()
        }
    }
}
