package app.aihandmade.core.metrics

import app.aihandmade.core.color.LabPlanes
import app.aihandmade.core.color.toLabPlanes

private const val C1 = 1.0  // (0.01 * 100)^2
private const val C2 = 9.0  // (0.03 * 100)^2

/** Core: mean SSIM between the L* channels of two equal-sized Lab planes. */
fun ssim(reference: LabPlanes, candidate: LabPlanes): Double {
    require(reference.width == candidate.width && reference.height == candidate.height) {
        "reference and candidate must have equal dimensions"
    }
    val width = reference.width
    val height = reference.height

    val X = reference.L
    val Y = candidate.L
    val windowSize = minOf(8, width, height)
    val n = (windowSize * windowSize).toDouble()

    val stride = width + 1
    val satSize = stride * (height + 1)
    val ix = DoubleArray(satSize)
    val ixx = DoubleArray(satSize)
    val iy = DoubleArray(satSize)
    val iyy = DoubleArray(satSize)
    val ixy = DoubleArray(satSize)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val xv = X[y * width + x].toDouble()
            val yv = Y[y * width + x].toDouble()
            val above = y * stride + x
            val left = (y + 1) * stride + x
            val diag = y * stride + (x + 1)
            val cur = (y + 1) * stride + (x + 1)
            ix[cur] = xv + ix[diag] + ix[left] - ix[above]
            ixx[cur] = xv * xv + ixx[diag] + ixx[left] - ixx[above]
            iy[cur] = yv + iy[diag] + iy[left] - iy[above]
            iyy[cur] = yv * yv + iyy[diag] + iyy[left] - iyy[above]
            ixy[cur] = xv * yv + ixy[diag] + ixy[left] - ixy[above]
        }
    }

    val numX = width - windowSize + 1
    val numY = height - windowSize + 1
    var sum = 0.0

    for (y0 in 0 until numY) {
        val y1 = y0 + windowSize
        for (x0 in 0 until numX) {
            val x1 = x0 + windowSize
            fun box(sat: DoubleArray) =
                sat[y1 * stride + x1] - sat[y0 * stride + x1] - sat[y1 * stride + x0] + sat[y0 * stride + x0]

            val muX = box(ix) / n
            val muY = box(iy) / n
            val varX = box(ixx) / n - muX * muX
            val varY = box(iyy) / n - muY * muY
            val cov = box(ixy) / n - muX * muY

            sum += ((2.0 * muX * muY + C1) * (2.0 * cov + C2)) /
                   ((muX * muX + muY * muY + C1) * (varX + varY + C2))
        }
    }

    return sum / (numX * numY)
}

/** Convenience: convert both ARGB images via core/color `toLabPlanes`, then delegate to the core. */
fun ssim(reference: IntArray, candidate: IntArray, width: Int, height: Int): Double =
    ssim(reference.toLabPlanes(width, height), candidate.toLabPlanes(width, height))
