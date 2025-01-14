/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.handlandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max
import kotlin.math.min
import KeyPointClassifier

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private var classifier: KeyPointClassifier? = null
    private var results: HandLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var handSignId: Int = 0
    private var pre_processed_landmark_list: List<Float> = emptyList()
    // Variable to hold the letter
    private var letters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "L", "M", "N", "O", "P", "R", "S", "T", "U", "V", "W", "Y","J","K","Q","X", "Z","Ñ")
    init {
        initPaints()
        // Inicializar el clasificador si el contexto no es null
        context?.let {
            classifier = KeyPointClassifier(it)
        }
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Liberar recursos del clasificador
        classifier?.close()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    private fun calcLandmarkList(imageWidth: Int, imageHeight: Int, landmarks: List<NormalizedLandmark>): List<List<Int>> {
        val landmarkPoint = mutableListOf<List<Int>>()

        // Iterar sobre cada landmark y calcular las coordenadas absolutas en píxeles
        for (landmark in landmarks) {
            val landmarkX = (landmark.x() * imageWidth).toInt().coerceAtMost(imageWidth - 1)
            val landmarkY = (landmark.y() * imageHeight).toInt().coerceAtMost(imageHeight - 1)
            landmarkPoint.add(listOf(landmarkX, landmarkY))
        }

        return landmarkPoint
    }

    fun preProcessLandmark(landmarkList: List<List<Int>>): List<Double> {
        // Hacer una copia profunda de la lista de entrada
        val tempLandmarkList = landmarkList.map { it.toMutableList() }.toMutableList()

        // Convertir a coordenadas relativas
        var baseX = 0
        var baseY = 0
        for ((index, landmarkPoint) in tempLandmarkList.withIndex()) {
            if (index == 0) {
                baseX = landmarkPoint[0]
                baseY = landmarkPoint[1]
            }
            tempLandmarkList[index][0] = tempLandmarkList[index][0] - baseX
            tempLandmarkList[index][1] = tempLandmarkList[index][1] - baseY
        }

        // Convertir a una lista unidimensional
        val flattenedList = tempLandmarkList.flatten()

        // Normalización
        val maxValue = flattenedList.map { kotlin.math.abs(it) }.maxOrNull()?.toDouble() ?: 1.0

        val normalizedList = flattenedList.map { it / maxValue }

        return normalizedList
    }





    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results?.let { handLandmarkerResult ->
            for (landmark in handLandmarkerResult.landmarks()) {
                classifier?.let {
                    // Calcula la lista de landmarks
                    val landmarkList = calcLandmarkList(imageWidth, imageHeight, landmark)
                    // Preprocesa los landmarks
                    val preprocessedLandmarkList = preProcessLandmark(landmarkList).map { it.toFloat() }
                    // Clasifica el signo de la mano
                    handSignId = it.classify(preprocessedLandmarkList)

                }
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = Float.MIN_VALUE
                var maxY = Float.MIN_VALUE

                // Recorremos cada punto del landmark
                for (normalizedLandmark in landmark) {
                    //empty list of landmarpoint
                    val landmarkpoint = mutableListOf<Float>()
                    val x = normalizedLandmark.x() * imageWidth * scaleFactor
                    val y = normalizedLandmark.y() * imageHeight * scaleFactor
                    //append x and y to the list


                    // Actualizamos los límites del bounding box
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)

                    // Dibujamos cada punto (opcional)
                    canvas.drawPoint(x, y, pointPaint)
                }

                // Dibujar las conexiones entre los puntos de la mano
                HandLandmarker.HAND_CONNECTIONS.forEach {
                    canvas.drawLine(
                        landmark[it!!.start()].x() * imageWidth * scaleFactor,
                        landmark[it.start()].y() * imageHeight * scaleFactor,
                        landmark[it.end()].x() * imageWidth * scaleFactor,
                        landmark[it.end()].y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }

                // Dibujamos el rectángulo delimitador (bounding box)
                val rectPaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL // Rectángulo con relleno
                    alpha = 50 // Transparencia
                }
                canvas.drawRect(minX, minY, maxX, maxY, rectPaint)

                // Dibujamos la letra en el centro del rectángulo
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 80f
                    textAlign = Paint.Align.CENTER
                }
                val textX = (minX + maxX) / 2
                val textY = (minY + maxY) / 2 - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(letters[handSignId], textX, textY, textPaint)
            }
        }
    }


    fun setResults(
        handLandmarkerResults: HandLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = handLandmarkerResults



        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}