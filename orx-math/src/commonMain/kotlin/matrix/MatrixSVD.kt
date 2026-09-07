package org.openrndr.extra.math.matrix

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Computes the Singular Value Decomposition (SVD) of this matrix.
 *
 * For a matrix A of dimensions m x n, the SVD factors A into:
 *   A = U * S * V^T
 *
 * where:
 * - U is an orthogonal matrix containing the left singular vectors:
 *   - If [fullMatrix] is true, U is m x m.
 *   - If [fullMatrix] is false, U is m x k, where k = min(m, n).
 * - S contains the singular values in descending order:
 *   - If [fullMatrix] is true, S is an m x n diagonal matrix.
 *   - If [fullMatrix] is false, S is a 1 x k matrix containing the singular values.
 * - V is an orthogonal matrix containing the right singular vectors:
 *   - If [fullMatrix] is true, V is n x n.
 *   - If [fullMatrix] is false, V is n x k, where k = min(m, n).
 *
 * @param fullMatrix If true, full matrices U and V and diagonal matrix S are computed.
 *                   If false, reduced (economy size) matrices are computed.
 * @return A Triple containing (U, S, V) such that this matrix is equal to U * S * V.transposed().
 */
fun Matrix.svd(fullMatrix: Boolean = true): Triple<Matrix, Matrix, Matrix> {
    val m = this.rows
    val n = this.cols
    val k = min(m, n)

    if (m == 0 || n == 0) {
        return if (fullMatrix) {
            Triple(Matrix(m, m), Matrix(m, n), Matrix(n, n))
        } else {
            Triple(Matrix(m, k), Matrix(1, k), Matrix(n, k))
        }
    }

    // If m < n, compute SVD of A^T (which is n x m with n >= m):
    // A^T = U_t * S_t * V_t^T
    // Then A = V_t * S_t^T * U_t^T
    // So U = V_t (m x m or m x k), S = S_t^T (m x n) or S_t (1 x k), V = U_t (n x n or n x k)
    if (m < n) {
        val (ut, st, vt) = this.transposed().svd(fullMatrix)
        return if (fullMatrix) {
            Triple(vt, st.transposed(), ut)
        } else {
            Triple(vt, st, ut)
        }
    }

    // Now m >= n
    // We use the Golub-Reinsch algorithm:
    // 1. Householder bidiagonalization
    // 2. Accumulate right transformations (V)
    // 3. Accumulate left transformations (U)
    // 4. Diagonalization of the bidiagonal matrix using QR steps with Wilkinson shift

    // Working copy in u (m x n initial part, expanded to m x m)
    // To accommodate m x m, we allocate a full m x m matrix for U
    val u = Matrix.zeros(m, m)
    for (i in 0 until m) {
        for (j in 0 until n) {
            u[i, j] = this[i, j]
        }
    }

    val v = Matrix.identity(n)
    val q = DoubleArray(n)     // diagonal elements
    val e = DoubleArray(n)     // superdiagonal elements

    var g = 0.0
    var scale = 0.0
    var anorm = 0.0

    // Step 1: Householder reduction to bidiagonal form
    for (i in 0 until n) {
        val l = i + 1
        e[i] = scale * g
        g = 0.0
        var s = 0.0
        scale = 0.0

        if (i < m) {
            for (k in i until m) {
                scale += abs(u[k, i])
            }
            if (scale != 0.0) {
                for (k in i until m) {
                    u[k, i] /= scale
                    s += u[k, i] * u[k, i]
                }
                var f = u[i, i]
                g = -copySign(sqrt(s), f)
                val h = f * g - s
                u[i, i] = f - g
                for (j in l until n) {
                    var sum = 0.0
                    for (k in i until m) {
                        sum += u[k, i] * u[k, j]
                    }
                    val factor = sum / h
                    for (k in i until m) {
                        u[k, j] += factor * u[k, i]
                    }
                }
                for (k in i until m) {
                    u[k, i] *= scale
                }
            }
        }
        q[i] = scale * g
        g = 0.0
        s = 0.0
        scale = 0.0

        if (i < m && i != n - 1) {
            for (k in l until n) {
                scale += abs(u[i, k])
            }
            if (scale != 0.0) {
                for (k in l until n) {
                    u[i, k] /= scale
                    s += u[i, k] * u[i, k]
                }
                val f = u[i, l]
                g = -copySign(sqrt(s), f)
                val h = f * g - s
                u[i, l] = f - g
                for (k in l until n) {
                    e[k] = u[i, k] / h
                }
                for (j in l until m) {
                    var sum = 0.0
                    for (k in l until n) {
                        sum += u[j, k] * u[i, k]
                    }
                    for (k in l until n) {
                        u[j, k] += sum * e[k]
                    }
                }
                for (k in l until n) {
                    u[i, k] *= scale
                }
            }
        }
        anorm = max(anorm, abs(q[i]) + abs(e[i]))
    }

    // Step 2: Accumulation of right-hand transformations (V)
    for (i in n - 1 downTo 0) {
        val l = i + 1
        if (i < n - 1) {
            if (g != 0.0) {
                for (j in l until n) {
                    v[j, i] = (u[i, j] / u[i, l]) / g
                }
                for (j in l until n) {
                    var s = 0.0
                    for (k in l until n) {
                        s += u[i, k] * v[k, j]
                    }
                    for (k in l until n) {
                        v[k, j] += s * v[k, i]
                    }
                }
            }
            for (j in l until n) {
                v[i, j] = 0.0
                v[j, i] = 0.0
            }
        }
        v[i, i] = 1.0
        g = e[i]
    }

    // Step 3: Accumulation of left-hand transformations (U)
    // Initialize columns from n to m-1 as identity
    for (i in n until m) {
        for (j in 0 until m) {
            u[j, i] = if (i == j) 1.0 else 0.0
        }
    }

    for (i in n - 1 downTo 0) {
        val l = i + 1
        g = q[i]
        for (j in l until m) {
            u[i, j] = 0.0
        }
        if (g != 0.0) {
            g = 1.0 / g
            for (j in l until m) {
                var s = 0.0
                for (k in l until m) {
                    s += u[k, i] * u[k, j]
                }
                val factor = (s / u[i, i]) * g
                for (k in i until m) {
                    u[k, j] += factor * u[k, i]
                }
            }
            for (j in i until m) {
                u[j, i] *= g
            }
        } else {
            for (j in i until m) {
                u[j, i] = 0.0
            }
        }
        u[i, i] += 1.0
    }

    // Step 4: Diagonalization of the bidiagonal form using QR iteration with implicit shifts
    for (k in n - 1 downTo 0) {
        var iterations = 0
        while (true) {
            iterations++
            if (iterations > 100) {
                break
            }

            var flag = true
            var l = 0
            for (ll in k downTo 0) {
                l = ll
                val nm = l - 1
                if (abs(e[l]) + anorm == anorm) {
                    flag = false
                    break
                }
                if (nm >= 0 && abs(q[nm]) + anorm == anorm) {
                    break
                }
            }

            if (flag) {
                var c = 0.0
                var s = 1.0
                for (i in l..k) {
                    val f = s * e[i]
                    e[i] = c * e[i]
                    if (abs(f) + anorm == anorm) break
                    g = q[i]
                    var h = pythag(f, g)
                    q[i] = h
                    h = 1.0 / h
                    c = g * h
                    s = -f * h
                    for (j in 0 until m) {
                        val y = u[j, l - 1]
                        val z = u[j, i]
                        u[j, l - 1] = y * c + z * s
                        u[j, i] = -y * s + z * c
                    }
                }
            }

            val z = q[k]
            if (l == k) {
                // Convergence: ensure singular value is non-negative
                if (z < 0.0) {
                    q[k] = -z
                    for (j in 0 until n) {
                        v[j, k] = -v[j, k]
                    }
                }
                break
            }

            // QR step with shift
            var x = q[l]
            val nm = k - 1
            var y = q[nm]
            g = e[nm]
            var h = e[k]
            var f = ((y - z) * (y + z) + (g - h) * (g + h)) / (2.0 * h * y)
            g = pythag(f, 1.0)
            f = ((x - z) * (x + z) + h * ((y / (f + copySign(g, f))) - h)) / x

            var c = 1.0
            var s = 1.0
            for (j in l..nm) {
                val i = j + 1
                g = e[i]
                y = q[i]
                h = s * g
                g = c * g
                var z1 = pythag(f, h)
                e[j] = z1
                c = f / z1
                s = h / z1
                f = x * c + g * s
                g = -x * s + g * c
                h = y * s
                y *= c
                for (jj in 0 until n) {
                    val x1 = v[jj, j]
                    val z2 = v[jj, i]
                    v[jj, j] = x1 * c + z2 * s
                    v[jj, i] = -x1 * s + z2 * c
                }
                z1 = pythag(f, h)
                q[j] = z1
                if (z1 != 0.0) {
                    z1 = 1.0 / z1
                    c = f * z1
                    s = h * z1
                }
                f = c * g + s * y
                x = -s * g + c * y
                for (jj in 0 until m) {
                    val y1 = u[jj, j]
                    val z2 = u[jj, i]
                    u[jj, j] = y1 * c + z2 * s
                    u[jj, i] = -y1 * s + z2 * c
                }
            }
            e[l] = 0.0
            e[k] = f
            q[k] = x
        }
    }

    // Step 5: Sort singular values in descending order
    for (i in 0 until n - 1) {
        var p = i
        var maxVal = q[i]
        for (j in i + 1 until n) {
            if (q[j] > maxVal) {
                p = j
                maxVal = q[j]
            }
        }
        if (p != i) {
            q[p] = q[i]
            q[i] = maxVal
            for (j in 0 until n) {
                val tmp = v[j, i]
                v[j, i] = v[j, p]
                v[j, p] = tmp
            }
            for (j in 0 until m) {
                val tmp = u[j, i]
                u[j, i] = u[j, p]
                u[j, p] = tmp
            }
        }
    }

    // Construct S matrix
    val s = if (fullMatrix) {
        val s = Matrix.zeros(m, n)
        for (i in 0 until k) {
            s[i, i] = q[i]
        }
        s
    } else {
        val s = Matrix.zeros(1, k)
        for (i in 0 until k) {
            s[0, i] = q[i]
        }
        s
    }

    val uResult = if (fullMatrix) u else u[0 until m, 0 until k]
    val vResult = if (fullMatrix) v else v[0 until n, 0 until k]

    return Triple(uResult, s, vResult)
}

private fun pythag(a: Double, b: Double): Double {
    val absA = abs(a)
    val absB = abs(b)
    return if (absA > absB) {
        val r = b / a
        absA * sqrt(1.0 + r * r)
    } else if (absB > 0.0) {
        val r = a / b
        absB * sqrt(1.0 + r * r)
    } else {
        0.0
    }
}

private fun copySign(magnitude: Double, sign: Double): Double {
    return if (sign < 0.0) -abs(magnitude) else abs(magnitude)
}