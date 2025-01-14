import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class KeyPointClassifier(
    context: Context,
    modelPath: String = "keypoint_classifier.tflite",
    private val numThreads: Int = 1
) {
    private val interpreter: Interpreter
    private val inputShape: IntArray
    private val outputShape: IntArray

    init {
        // Cargar el modelo desde los assets
        val modelBuffer = loadModelFile(context, modelPath)

        // Crear el intérprete de TFLite
        val options = Interpreter.Options().apply {
            setNumThreads(numThreads)
        }
        interpreter = Interpreter(modelBuffer, options)

        // Obtener detalles de entrada y salida
        inputShape = interpreter.getInputTensor(0).shape() // e.g., [1, N]
        outputShape = interpreter.getOutputTensor(0).shape() // e.g., [1, M]
    }

    fun classify(landmarkList: List<Float>): Int {
        // Asegurarse de que los datos coincidan con la forma esperada
        require(landmarkList.size == inputShape[1]) {
            "Expected input of size ${inputShape[1]}, but got ${landmarkList.size}"
        }

        // Preparar el buffer de entrada
        val inputBuffer = ByteBuffer.allocateDirect(4 * landmarkList.size)
        inputBuffer.order(ByteOrder.nativeOrder())
        landmarkList.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        // Preparar el buffer de salida
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputShape[1])
        outputBuffer.order(ByteOrder.nativeOrder())
        outputBuffer.rewind()

        // Ejecutar el modelo
        interpreter.run(inputBuffer, outputBuffer)

        // Obtener el resultado con mayor probabilidad
        outputBuffer.rewind()
        val outputArray = FloatArray(outputShape[1])
        outputBuffer.asFloatBuffer().get(outputArray)

        // Encontrar el índice del valor máximo
        val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] } ?: -1

        return maxIndex
    }

    fun close() {
        interpreter.close()
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        inputStream.close()
        return modelBuffer
    }
}
