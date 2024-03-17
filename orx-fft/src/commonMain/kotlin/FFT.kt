package org.openrndr.extra.fft

import kotlin.math.*

/*
Based on https://github.com/ddf/Minim/blob/e294e2881a20340603ee0156cb9188c15b5915c2/src/main/java/ddf/minim/analysis/FFT.java
I (EJ) stripped away spectrum and averages.

This is the original license (GPLv2). I am not sure if my low-effort Kotlin port falls under the same license.

 *  Copyright (c) 2007 - 2008 by Damien Di Fede <ddf@compartmental.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

class FFT(val size: Int, private val windowFunction: WindowFunction = IdentityWindow()) {

    var real = FloatArray(size)
    var imag = FloatArray(size)

    private fun setComplex(r: FloatArray, i: FloatArray) {
        if (real.size != r.size && imag.size != i.size) {
            error("FourierTransform.setComplex: the two arrays must be the same length as their member counterparts.")
        } else {
            r.copyInto(real)
            i.copyInto(imag)
        }
    }

    fun magnitudeSum(includeDC: Boolean = false): Double {
        var sum = 0.0
        for (i in (if (includeDC) 0 else 1)..size / 2) {
            sum += magnitude(i)
        }
        return sum
    }

    fun scaleAll(sr: Float, includeDC: Boolean = false) {
        for (i in (if (includeDC) 0 else 1)..size / 2) {
            scaleBand(i, sr)
        }
    }

    fun magnitude(i: Int): Float {
        return sqrt(real[i] * real[i] + imag[i] * imag[i])
    }

    fun phase(i: Int): Float {
        return atan2(imag[i], real[i])
    }

    fun shiftPhase(i: Int, shift: Double) {
        val m = magnitude(i)
        val phase = phase(i)
        real[i] = (cos(phase + shift) * m).toFloat()
        imag[i] = (sin(phase + shift) * m).toFloat()

        if (i != 0 && i != size / 2) {
            real[(size - i)] = real[i]
            imag[(size - i)] = -imag[i]
        }
    }

    fun scaleBand(i: Int, sr: Float) {
        if (sr < 0) {
            error("Can't scale a frequency band by a negative value.")
        }

        real[i] *= sr
        imag[i] *= sr

        if (i != 0 && i != size / 2) {
            real[(size - i)] = real[i]
            imag[(size - i)] = -imag[i]
        }
    }


    // performs an in-place fft on the data in the real and imag arrays
    // bit reversing is not necessary as the data will already be bit reversed
    private fun fft() {
        var halfSize = 1
        while (halfSize < real.size) {
            // float k = -(float)Math.PI/halfSize;
            // phase shift step
            // float phaseShiftStepR = (float)Math.cos(k);
            // float phaseShiftStepI = (float)Math.sin(k);
            // using lookup table
            val phaseShiftStepR = cos[halfSize]
            val phaseShiftStepI = sin[halfSize]
            // current phase shift
            var currentPhaseShiftR = 1.0f
            var currentPhaseShiftI = 0.0f
            for (fftStep in 0 until halfSize) {
                var i = fftStep
                while (i < real.size) {
                    val off = i + halfSize
                    val tr = (currentPhaseShiftR * real[off]) - (currentPhaseShiftI * imag[off])
                    val ti = (currentPhaseShiftR * imag[off]) + (currentPhaseShiftI * real[off])
                    real[off] = real[i] - tr
                    imag[off] = imag[i] - ti
                    real[i] += tr
                    imag[i] += ti
                    i += 2 * halfSize
                }
                val tmpR = currentPhaseShiftR
                currentPhaseShiftR = (tmpR * phaseShiftStepR) - (currentPhaseShiftI * phaseShiftStepI)
                currentPhaseShiftI = (tmpR * phaseShiftStepI) + (currentPhaseShiftI * phaseShiftStepR)
            }
            halfSize *= 2
        }
    }

    private fun doWindow(samples: FloatArray) {
        windowFunction.apply(samples)
    }


    fun forward(buffer: FloatArray) {
        if (buffer.size != size) {
            error("FFT.forward: The length of the passed sample buffer must be equal to timeSize().")
        }
        doWindow(buffer)
        // copy samples to real/imag in bit-reversed order
        bitReverseSamples(buffer, 0)
        // perform the fft
        fft()
    }

    fun forward(buffer: FloatArray, startAt: Int) {
        if (buffer.size - startAt < size) {
            error(
                "FourierTransform.forward: not enough samples in the buffer between " +
                        startAt + " and " + buffer.size + " to perform a transform."
            )
        }
        windowFunction.apply(buffer, startAt, size)
        bitReverseSamples(buffer, startAt)
        fft()
    }

    /**
     * Performs a forward transform on the passed buffers.
     *
     * @param buffReal the real part of the time domain signal to transform
     * @param buffImag the imaginary part of the time domain signal to transform
     */
    fun forward(buffReal: FloatArray, buffImag: FloatArray) {
        if (buffReal.size != size || buffImag.size != size) {
            error("FFT.forward: The length of the passed buffers must be equal to timeSize().")
        }
        setComplex(buffReal, buffImag)
        bitReverseComplex()
        fft()
    }

    fun inverse(buffer: FloatArray) {
        if (buffer.size > real.size) {
            error("FFT.inverse: the passed array's length must equal FFT.timeSize().")
        }
        // conjugate
        for (i in 0 until size) {
            imag[i] *= -1.0f
        }
        bitReverseComplex()
        fft()
        // copy the result in real into buffer, scaling as we do
        for (i in buffer.indices) {
            buffer[i] = real[i] / real.size
        }
    }

    private val reverse by lazy { buildReverseTable() }

    private fun buildReverseTable(): IntArray {
        val reverse = IntArray(size)
        // set up the bit reversing table
        reverse[0] = 0
        var limit = 1
        var bit = size / 2
        while (limit < size) {
            for (i in 0 until limit) reverse[i + limit] = reverse[i] + bit
            limit = limit shl 1
            bit = bit shr 1
        }
        return reverse
    }

    // copies the values in the samples array into the real array
    // in bit reversed order. the imag array is filled with zeros.
    private fun bitReverseSamples(samples: FloatArray, startAt: Int) {
        for (i in 0 until size) {
            real[i] = samples[startAt + reverse[i]]
            imag[i] = 0.0f
        }
    }

    // bit reverse real[] and imag[]
    private fun bitReverseComplex() {
        val revReal = FloatArray(real.size)
        val revImag = FloatArray(imag.size)
        for (i in real.indices) {
            revReal[i] = real[reverse[i]]
            revImag[i] = imag[reverse[i]]
        }
        real = revReal
        imag = revImag
    }

    // lookup tables
    private val sin by lazy { FloatArray(size) { i -> sin((-PI.toFloat() / i).toDouble()).toFloat() } }
    private val cos by lazy { FloatArray(size) { i -> cos((-PI.toFloat() / i).toDouble()).toFloat() } }
}