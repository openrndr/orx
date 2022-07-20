package org.openrndr.extra.triangulation

import kotlin.math.pow

// original code: https://github.com/FlorisSteenkamp/double-double/


/**
 * Returns the difference and exact error of subtracting two floating point
 * numbers.
 * Uses an EFT (error-free transformation), i.e. `a-b === x+y` exactly.
 * The returned result is a non-overlapping expansion (smallest value first!).
 *
 * * **precondition:** `abs(a) >= abs(b)` - A fast test that can be used is
 * `(a > b) === (a > -b)`
 *
 * See https://people.eecs.berkeley.edu/~jrs/papers/robustr.pdf
 */
internal fun fastTwoDiff(a: Double, b: Double): DoubleArray {
    val x = a - b;
    val y = (a - x) - b;

    return doubleArrayOf(y, x)
}

/**
 * Returns the sum and exact error of adding two floating point numbers.
 * Uses an EFT (error-free transformation), i.e. a+b === x+y exactly.
 * The returned sum is a non-overlapping expansion (smallest value first!).
 *
 * Precondition: abs(a) >= abs(b) - A fast test that can be used is
 * (a > b) === (a > -b)
 *
 * See https://people.eecs.berkeley.edu/~jrs/papers/robustr.pdf
 */
internal fun fastTwoSum(a: Double, b: Double): DoubleArray {
    val x = a + b;

    return doubleArrayOf(b - (x - a), x)
}


/**
 * Truncates a floating point value's significand and returns the result.
 * Similar to split, but with the ability to specify the number of bits to keep.
 *
 * **Theorem 17 (Veltkamp-Dekker)**: Let a be a p-bit floating-point number, where
 * p >= 3. Choose a splitting point s such that p/2 <= s <= p-1. Then the
 * following algorithm will produce a (p-s)-bit value a_hi and a
 * nonoverlapping (s-1)-bit value a_lo such that abs(a_hi) >= abs(a_lo) and
 * a = a_hi + a_lo.
 *
 * * see [Shewchuk](https://people.eecs.berkeley.edu/~jrs/papers/robustr.pdf)
 *
 * @param a a double
 * @param bits the number of significand bits to leave intact
 */
internal fun reduceSignificand(
    a: Double,
    bits: Int
): Double {

    val s = 53 - bits;
    val f = 2.0.pow(s) + 1;

    val c = f * a;
    val r = c - (c - a);

    return r;
}


/**
 * === 2^Math.ceil(p/2) + 1 where p is the # of significand bits in a double === 53.
 * @internal
 */
private const val f = 134217729;  // 2**27 + 1;


/**
 * Returns the result of splitting a double into 2 26-bit doubles.
 *
 * Theorem 17 (Veltkamp-Dekker): Let a be a p-bit floating-point number, where
 * p >= 3. Choose a splitting point s such that p/2 <= s <= p-1. Then the
 * following algorithm will produce a (p-s)-bit value a_hi and a
 * nonoverlapping (s-1)-bit value a_lo such that abs(a_hi) >= abs(a_lo) and
 * a = a_hi + a_lo.
 *
 * see e.g. [Shewchuk](https://people.eecs.berkeley.edu/~jrs/papers/robustr.pdf)
 * @param a A double floating point number
 */
private fun split(a: Double): DoubleArray {
    val c = f * a;
    val a_h = c - (c - a);
    val a_l = a - a_h;

    return doubleArrayOf(a_h, a_l)
}

/**
 * Returns the exact result of subtracting b from a.
 *
 * @param a minuend - a double-double precision floating point number
 * @param b subtrahend - a double-double precision floating point number
 */
internal fun twoDiff(a: Double, b: Double): DoubleArray {
    val x = a - b;
    val bvirt = a - x;
    val y = (a - (x + bvirt)) + (bvirt - b);

    return doubleArrayOf(y, x)
}

/**
 * Returns the exact result of multiplying two doubles.
 *
 * * the resulting array is the reverse of the standard twoSum in the literature.
 *
 * Theorem 18 (Shewchuk): Let a and b be p-bit floating-point numbers, where
 * p >= 6. Then the following algorithm will produce a nonoverlapping expansion
 * x + y such that ab = x + y, where x is an approximation to ab and y
 * represents the roundoff error in the calculation of x. Furthermore, if
 * round-to-even tiebreaking is used, x and y are non-adjacent.
 *
 * See https://people.eecs.berkeley.edu/~jrs/papers/robustr.pdf
 * @param a A double
 * @param b Another double
 */
internal fun twoProduct(a: Double, b: Double): DoubleArray {
    val x = a * b;

    //const [ah, al] = split(a);
    val c = f * a;
    val ah = c - (c - a);
    val al = a - ah;
    //const [bh, bl] = split(b);
    val d = f * b;
    val bh = d - (d - b);
    val bl = b - bh;

    val y = (al * bl) - ((x - (ah * bh)) - (al * bh) - (ah * bl));

    //const err1 = x - (ah * bh);
    //const err2 = err1 - (al * bh);
    //const err3 = err2 - (ah * bl);
    //const y = (al * bl) - err3;

    return doubleArrayOf(y, x)
}

internal fun twoSquare(a: Double): DoubleArray {
    val x = a * a;

    //const [ah, al] = split(a);
    val c = f * a;
    val ah = c - (c - a);
    val al = a - ah;

    val y = (al * al) - ((x - (ah * ah)) - 2 * (ah * al));

    return doubleArrayOf(y, x)
}

/**
 * Returns the exact result of adding two doubles.
 *
 * * the resulting array is the reverse of the standard twoSum in the literature.
 *
 * Theorem 7 (Knuth): Let a and b be p-bit floating-point numbers. Then the
 * following algorithm will produce a nonoverlapping expansion x + y such that
 * a + b = x + y, where x is an approximation to a + b and y is the roundoff
 * error in the calculation of x.
 *
 * See https://people.eecs.berkeley.edu/~jrs/papers/robustr.pdf
 */
internal fun twoSum(a: Double, b: Double): DoubleArray {
    val x = a + b;
    val bv = x - a;

    return doubleArrayOf((a - (x - bv)) + (b - bv), x)
}

/**
 * Returns the result of subtracting the second given double-double-precision
 * floating point number from the first.
 *
 * * relative error bound: 3u^2 + 13u^3, i.e. fl(a-b) = (a-b)(1+ϵ),
 * where ϵ <= 3u^2 + 13u^3, u = 0.5 * Number.EPSILON
 * * the error bound is not sharp - the worst case that could be found by the
 * authors were 2.25u^2
 *
 * ALGORITHM 6 of https://hal.archives-ouvertes.fr/hal-01351529v3/document
 * @param x a double-double precision floating point number
 * @param y another double-double precision floating point number
 */
internal fun ddDiffDd(x: DoubleArray, y: DoubleArray): DoubleArray {
    val xl = x[0];
    val xh = x[1];
    val yl = y[0];
    val yh = y[1];

    //const [sl,sh] = twoSum(xh,yh);
    val sh = xh - yh; val _1 = sh - xh; val sl = (xh - (sh - _1)) + (-yh - _1);
    //const [tl,th] = twoSum(xl,yl);
    val th = xl - yl; val _2 = th - xl; val tl = (xl - (th - _2)) + (-yl - _2);
    val c = sl + th;
    //const [vl,vh] = fastTwoSum(sh,c)
    val vh = sh + c; val vl = c - (vh - sh);
    val w = tl + vl
    //const [zl,zh] = fastTwoSum(vh,w)
    val zh = vh + w; val zl = w - (zh - vh);

    return doubleArrayOf(zl, zh)
}

/**
 * Returns the product of two double-double-precision floating point numbers.
 *
 * * relative error bound: 7u^2, i.e. fl(a+b) = (a+b)(1+ϵ),
 * where ϵ <= 7u^2, u = 0.5 * Number.EPSILON
 * the error bound is not sharp - the worst case that could be found by the
 * authors were 5u^2
 *
 * * ALGORITHM 10 of https://hal.archives-ouvertes.fr/hal-01351529v3/document
 * @param x a double-double precision floating point number
 * @param y another double-double precision floating point number
 */
internal fun ddMultDd(x: DoubleArray, y: DoubleArray): DoubleArray {


    //const xl = x[0];
    val xh = x[1];
    //const yl = y[0];
    val yh = y[1];

    //const [cl1,ch] = twoProduct(xh,yh);
    val ch = xh*yh;
    val c = f * xh; val ah = c - (c - xh); val al = xh - ah;
    val d = f * yh; val bh = d - (d - yh); val bl = yh - bh;
    val cl1 = (al*bl) - ((ch - (ah*bh)) - (al*bh) - (ah*bl));

    //return fastTwoSum(ch,cl1 + (xh*yl + xl*yh));
    val b = cl1 + (xh*y[0] + x[0]*yh);
    val xx = ch + b;

    return doubleArrayOf(b - (xx - ch), xx)
}


/**
 * Returns the result of adding two double-double-precision floating point
 * numbers.
 *
 * * relative error bound: 3u^2 + 13u^3, i.e. fl(a+b) = (a+b)(1+ϵ),
 * where ϵ <= 3u^2 + 13u^3, u = 0.5 * Number.EPSILON
 * * the error bound is not sharp - the worst case that could be found by the
 * authors were 2.25u^2
 *
 * ALGORITHM 6 of https://hal.archives-ouvertes.fr/hal-01351529v3/document
 * @param x a double-double precision floating point number
 * @param y another double-double precision floating point number
 */
internal fun ddAddDd(x: DoubleArray, y: DoubleArray): DoubleArray {
    val xl = x[0];
    val xh = x[1];
    val yl = y[0];
    val yh = y[1];

    //const [sl,sh] = twoSum(xh,yh);
    val sh = xh + yh; val _1 = sh - xh; val sl = (xh - (sh - _1)) + (yh - _1);
    //val [tl,th] = twoSum(xl,yl);
    val th = xl + yl; val _2 = th - xl; val tl = (xl - (th - _2)) + (yl - _2);
    val c = sl + th;
    //val [vl,vh] = fastTwoSum(sh,c)
    val vh = sh + c; val vl = c - (vh - sh);
    val w = tl + vl
    //val [zl,zh] = fastTwoSum(vh,w)
    val zh = vh + w; val zl = w - (zh - vh);

    return doubleArrayOf(zl, zh)
}


/**
 * Returns the product of a double-double-precision floating point number and a
 * double.
 *
 * * slower than ALGORITHM 8 (one call to fastTwoSum more) but about 2x more
 * accurate
 * * relative error bound: 1.5u^2 + 4u^3, i.e. fl(a+b) = (a+b)(1+ϵ),
 * where ϵ <= 1.5u^2 + 4u^3, u = 0.5 * Number.EPSILON
 * * the bound is very sharp
 * * probably prefer `ddMultDouble2` due to extra speed
 *
 * * ALGORITHM 7 of https://hal.archives-ouvertes.fr/hal-01351529v3/document
 * @param y a double
 * @param x a double-double precision floating point number
 */
internal fun ddMultDouble1(y: Double, x: DoubleArray): DoubleArray {
    val xl = x[0];
    val xh = x[1];

    //val [cl1,ch] = twoProduct(xh,y);
    val ch = xh*y;
    val c = f * xh; val ah = c - (c - xh); val al = xh - ah;
    val d = f * y; val bh = d - (d - y); val bl = y - bh;
    val cl1 = (al*bl) - ((ch - (ah*bh)) - (al*bh) - (ah*bl));

    val cl2 = xl*y;
    //val [tl1,th] = fastTwoSum(ch,cl2);
    val th = ch + cl2;
    val tl1 = cl2 - (th - ch);

    val tl2 = tl1 + cl1;
    //val [zl,zh] = fastTwoSum(th,tl2);
    val zh = th + tl2;
    val zl = tl2 - (zh - th);

    return doubleArrayOf(zl,zh);
}