package org.openrndr.extra.fft

abstract class WindowFunction {
    private var length: Int = 0

    /**
     * Apply the window function to a sample buffer.
     *
     * @param samples a sample buffer
     */
    fun apply(samples: FloatArray) {
        this.length = samples.size

        for (n in samples.indices) {
            samples[n] *= value(samples.size, n)
        }
    }

    /**
     * Apply the window to a portion of this sample buffer,
     * given an offset from the beginning of the buffer
     * and the number of samples to be windowed.
     *
     * @param samples
     * float[]: the array of samples to apply the window to
     * @param offset
     * int: the index in the array to begin windowing
     * @param length
     * int: how many samples to apply the window to
     */
    fun apply(samples: FloatArray, offset: Int, length: Int) {
        this.length = length

        for (n in offset until offset + length) {
            samples[n] *= value(length, n - offset)
        }
    }
    protected abstract fun value(length: Int, index: Int): Float
}