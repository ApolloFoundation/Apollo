/* Ported to JavaScript from Java 07/01/14.
 *
 * Ported from C to Java by Dmitry Skiba [sahn0], 23/02/08.
 * Original: http://cds.xs4all.nl:8081/ecdh/
 */
/* Generic 64-bit integer implementation of Curve25519 ECDH
 * Written by Matthijs van Duin, 200608242056
 * Public domain.
 *
 * Based on work by Daniel J Bernstein, http://cr.yp.to/ecdh.html
 */
var bigInt =require('./bigInt.min');


var curve25519 = function () {

    //region Constants

    var KEY_SIZE = 32;

    /* array length */
    var UNPACKED_SIZE = 16;

    var LONG_10_SIZE;

    /* group order (a prime near 2^252+2^124) */
    var ORDER = [
        237, 211, 245, 92,
        26, 99, 18, 88,
        214, 156, 247, 162,
        222, 249, 222, 20,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 16
    ];

    /* smallest multiple of the order that's >= 2^255 */
    var ORDER_TIMES_8 = [
        104, 159, 174, 231,
        210, 24, 147, 192,
        178, 230, 188, 23,
        245, 206, 247, 166,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 128
    ];

    /* constants 2Gy and 1/(2Gy) */
    var BASE_2Y = [
        22587, 610, 29883, 44076,
        15515, 9479, 25859, 56197,
        23910, 4462, 17831, 16322,
        62102, 36542, 52412, 16035
    ];

    var BASE_R2Y = [
        5744, 16384, 61977, 54121,
        8776, 18501, 26522, 34893,
        23833, 5823, 55924, 58749,
        24147, 14085, 13606, 6080
    ];

    var C1 = [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    var C9 = [9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    var C486671 = [0x6D0F, 0x0007, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    var C39420360 = [0x81C8, 0x0259, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

    var P25 = 33554431; /* (1 << 25) - 1 */
    var P26 = 67108863; /* (1 << 26) - 1 */

    //#endregion

    //region Key Agreement

    /* Private key clamping
     *   k [out] your private key for key agreement
     *   k  [in]  32 random bytes
     */
    function clamp (k) {
        k[31] &= 0x7F;
        k[31] |= 0x40;
        k[ 0] &= 0xF8;
    }

    //endregion

    //region radix 2^8 math

    function cpy32 (d, s) {
        for (var i = 0; i < 32; i++)
            d[i] = s[i];
    }

    /* p[m..n+m-1] = q[m..n+m-1] + z * x */
    /* n is the size of x */
    /* n+m is the size of p and q */
    function mula_small (p, q, m, x, n, z) {
        m = m | 0;
        n = n | 0;
        z = z | 0;

        var v = 0;
        for (var i = 0; i < n; ++i) {
            v += (q[i + m] & 0xFF) + z * (x[i] & 0xFF);
            p[i + m] = (v & 0xFF);
            v >>= 8;
        }

        return v;
    }

    /* p += x * y * z  where z is a small integer
     * x is size 32, y is size t, p is size 32+t
     * y is allowed to overlap with p+32 if you don't care about the upper half  */
    function mula32 (p, x, y, t, z) {
        t = t | 0;
        z = z | 0;

        var n = 31;
        var w = 0;
        var i = 0;
        for (; i < t; i++) {
            var zy = z * (y[i] & 0xFF);
            w += mula_small(p, p, i, x, n, zy) + (p[i+n] & 0xFF) + zy * (x[n] & 0xFF);
            p[i + n] = w & 0xFF;
            w >>= 8;
        }
        p[i + n] = (w + (p[i + n] & 0xFF)) & 0xFF;
        return w >> 8;
    }

    /* divide r (size n) by d (size t), returning quotient q and remainder r
     * quotient is size n-t+1, remainder is size t
     * requires t > 0 && d[t-1] !== 0
     * requires that r[-1] and d[-1] are valid memory locations
     * q may overlap with r+t */
    function divmod (q, r, n, d, t) {
        n = n | 0;
        t = t | 0;

        var rn = 0;
        var dt = (d[t - 1] & 0xFF) << 8;
        if (t > 1)
            dt |= (d[t - 2] & 0xFF);

        while (n-- >= t) {
            var z = (rn << 16) | ((r[n] & 0xFF) << 8);
            if (n > 0)
                z |= (r[n - 1] & 0xFF);

            var i = n - t + 1;
            z /= dt;
            rn += mula_small(r, r, i, d, t, -z);
            q[i] = (z + rn) & 0xFF;
            /* rn is 0 or -1 (underflow) */
            mula_small(r, r, i, d, t, -rn);
            rn = r[n] & 0xFF;
            r[n] = 0;
        }

        r[t-1] = rn & 0xFF;
    }

    function numsize (x, n) {
        while (n-- !== 0 && x[n] === 0) { }
        return n + 1;
    }

    /* Returns x if a contains the gcd, y if b.
     * Also, the returned buffer contains the inverse of a mod b,
     * as 32-byte signed.
     * x and y must have 64 bytes space for temporary use.
     * requires that a[-1] and b[-1] are valid memory locations  */
    function egcd32 (x, y, a, b) {
        var an, bn = 32, qn, i;
        for (i = 0; i < 32; i++)
            x[i] = y[i] = 0;
        x[0] = 1;
        an = numsize(a, 32);
        if (an === 0)
            return y; /* division by zero */
        var temp = new Array(32);
        while (true) {
            qn = bn - an + 1;
            divmod(temp, b, bn, a, an);
            bn = numsize(b, bn);
            if (bn === 0)
                return x;
            mula32(y, x, temp, qn, -1);

            qn = an - bn + 1;
            divmod(temp, a, an, b, bn);
            an = numsize(a, an);
            if (an === 0)
                return y;
            mula32(x, y, temp, qn, -1);
        }
    }

    //endregion

    //region radix 2^25.5 GF(2^255-19) math

    //region pack / unpack


    /* Convert to internal format from little-endian byte format */
    function unpack (x, m) {
        for (var i = 0; i < KEY_SIZE; i += 2)
            x[i / 2] = m[i] & 0xFF | ((m[i + 1] & 0xFF) << 8);
        debugger;
    }

    // TODO : migrate to bigInt
    function unpackJava(x, m) {
        x._0 = (bigInt(m[0])   .and(0xFF))
            .or(((bigInt(m[1]).and(0xFF))).shiftLeft(8))
            .or((bigInt(m[2]).and(0xFF)).shiftLeft(16))
            .or(((bigInt(m[3]).and(0xFF)).and(3)).shiftLeft(24));

        x._1 = ((bigInt(m[3])  .and(0xFF)).and((bigInt(3).not()))).shiftRight(2)
            .or((bigInt(m[4])  .and(0xFF)).shiftLeft(6))
            .or((bigInt(m[5])  .and(0xFF)).shiftLeft(14))
            .or(((bigInt(m[6]) .and(0xFF)) .and(7)).shiftLeft(22));

        x._2 = ((bigInt(m[6])   .and(0xFF)).and(bigInt(7).not())).shiftRight(3)
            .or(( bigInt(m[7])  .and(0xFF)).shiftLeft(5))
            .or((bigInt(m[8])   .and(0xFF)).shiftLeft(13))
            .or(((bigInt(m[9])  .and(0xFF)) .and(31)).shiftLeft(21));

        x._3 = ((bigInt(m[9])   .and(0xFF)).and(bigInt(31).not())).shiftRight(5)
            .or((bigInt(m[10])  .and(0xFF)).shiftLeft(3))
            .or((bigInt(m[11])  .and(0xFF)).shiftLeft(11))
            .or(((bigInt(m[12]) .and(0xFF)) .and(63)).shiftLeft(19));

        x._4 = ((bigInt(m[12])  .and(0xFF)).and(bigInt(63).not())).shiftRight(6)
            .or((bigInt(m[13])   .and(0xFF)).shiftLeft(2))
            .or((bigInt(m[14])   .and(0xFF)).shiftLeft(10))
            .or((bigInt(m[15])  .and(0xFF)).shiftLeft(18));

        x._5 =  (bigInt(m[16])  .and(0xFF))
            .or((bigInt(m[17])  .and(0xFF)).shiftLeft(8))
            .or((bigInt(m[18])  .and(0xFF)).shiftLeft(16))
            .or(((bigInt(m[19]) .and(0xFF)).and(1)).shiftLeft(24));

        x._6 = ((bigInt(m[19])  .and(0xFF)).and(bigInt(1).not())).shiftRight(1)
            .or((bigInt(m[20])  .and(0xFF)).shiftLeft(7))
            .or((bigInt(m[21])  .and(0xFF)).shiftLeft(15))
            .or(((bigInt(m[22]) .and(0xFF)).and(7)).shiftLeft(23));

        x._7 = ((bigInt(m[22])  .and(0xFF)).and(bigInt(7).not())).shiftRight(3)
            .or(( bigInt(m[23])  .and(0xFF)).shiftLeft(5))
            .or((bigInt(m[24])   .and(0xFF)).shiftLeft(13))
            .or(((bigInt(m[25])  .and(0xFF)).and(15)).shiftLeft(21));

        x._8 = ((bigInt(m[25])  .and(0xFF)).and(bigInt(15).not())).shiftRight(4)
            .or((bigInt(m[26])   .and(0xFF)).shiftLeft(4))
            .or((bigInt(m[27])   .and(0xFF)).shiftLeft(12))
            .or(((bigInt(m[28])  .and(0xFF)).and(63)).shiftLeft(20));

        x._9 = ((bigInt(m[28])  .and(0xFF)).and(bigInt(63).not())).shiftLeft(6)
            .or((bigInt(m[29])   .and(0xFF)).shiftLeft(2))
            .or((bigInt(m[30])   .and(0xFF)).shiftLeft(10))
            .or((bigInt(m[31])  .and(0xFF)).shiftLeft(18)) ;



        console.log(x._0);
        console.log(x._1);
        console.log(x._2);
        console.log(x._3);
        console.log(x._4);
        console.log(x._5);
        console.log(x._6);
        console.log(x._7);
        console.log(x._8);
        console.log(x._9);



    }

    // function unpackJava(x, m) {
    //     x._0 = ((m[0] & 0xFF))         | ((m[1] & 0xFF))<<8 |
    //         (m[2] & 0xFF)<<16      | ((m[3] & 0xFF)& 3)<<24;
    //     x._1 = ((m[3] & 0xFF)&~ 3)>>2  | (m[4] & 0xFF)<<6 |
    //         (m[5] & 0xFF)<<14 | ((m[6] & 0xFF)& 7)<<22;
    //     x._2 = ((m[6] & 0xFF)&~ 7)>>3  | (m[7] & 0xFF)<<5 |
    //         (m[8] & 0xFF)<<13 | ((m[9] & 0xFF)&31)<<21;
    //     x._3 = ((m[9] & 0xFF)&~31)>>5  | (m[10] & 0xFF)<<3 |
    //         (m[11] & 0xFF)<<11 | ((m[12] & 0xFF)&63)<<19;
    //     x._4 = ((m[12] & 0xFF)&~63)>>6 | (m[13] & 0xFF)<<2 |
    //         (m[14] & 0xFF)<<10 |  (m[15] & 0xFF)    <<18;


        // x._5 =  (m[16] & 0xFF)         | (m[17] & 0xFF)<<8 |
        //
        //     (m[18] & 0xFF)<<16 | ((m[19] & 0xFF)& 1)<<24;

    //     x._6 = ((m[19] & 0xFF)&~ 1)>>1 | (m[20] & 0xFF)<<7 |
    //         (m[21] & 0xFF)<<15 | ((m[22] & 0xFF)& 7)<<23;
    //     x._7 = ((m[22] & 0xFF)&~ 7)>>3 | (m[23] & 0xFF)<<5 |
    //         (m[24] & 0xFF)<<13 | ((m[25] & 0xFF)&15)<<21;
    //     x._8 = ((m[25] & 0xFF)&~15)>>4 | (m[26] & 0xFF)<<4 |
    //         (m[27] & 0xFF)<<12 | ((m[28] & 0xFF)&63)<<20;
    //     x._9 = ((m[28] & 0xFF)&~63)>>6 | (m[29] & 0xFF)<<2 |
    //         (m[30] & 0xFF)<<10 |  (m[31] & 0xFF)    <<18;
    // }

    /* Check if reduced-form input >= 2^255-19 */
    function is_overflow (x) {
        return (
            ((x[0] > P26 - 19)) &&
                ((x[1] & x[3] & x[5] & x[7] & x[9]) === P25) &&
                ((x[2] & x[4] & x[6] & x[8]) === P26)
            ) || (x[9] > P25);
    }

    /* Convert from internal format to little-endian byte format.  The
     * number must be in a reduced form which is output by the following ops:
     *     unpack, mul, sqr
     *     set --  if input in range 0 .. P25
     * If you're unsure if the number is reduced, first multiply it by 1.  */
    function pack (x, m) {
        for (var i = 0; i < UNPACKED_SIZE; ++i) {
            m[2 * i] = x[i] & 0x00FF;
            m[2 * i + 1] = (x[i] & 0xFF00) >> 8;
        }
    }

    // TODO
    function is_overflow_java(x) {
        return (
            ((x._0.greater(P26) - 19)) &&
            ((x._1.and(x._3).and(x._5).add(x._7).add(x._9)).equals(P25)) &&
            ((x._2.and(x._4).and(x._6).and(x._8)).equals(P26))
        ) || (x._9.greater(P25));
    }

    function packJava(x, m) {
        var ld = 0, ud = 0;
        var t;

        ld = new bigInt((is_overflow_java(x) ? 1 : 0) - ((x._9.lesser(0)) ? 1 : 0));

        ud = ld.multiply((bigInt(-P25).add(1)));
        ld = ld.multiply(19);

        t = ld.add(x._0).add(x._1.shiftLeft(26));

        m[0] = t;
        m[1] = (t.shiftRight(8));

        m[2] = (t.shiftRight(16));
        m[3] = (t.shiftRight(24));
        t = (t.shiftRight(32)).add(x._2.shiftLeft(19));
        m[4] = t;
        m[5] = (t.shiftRight(8));
        m[6] = (t.shiftRight(16));
        m[7] = (t.shiftRight(24));
        t = (t.shiftRight(32)).add(x._3.shiftLeft(13));
        m[8] =   t;
        m[9] =   (t.shiftRight(8));
        m[10] =  (t.shiftRight(16));
        m[11] =  (t.shiftRight(24));
        t = (t.shiftRight(32)).add(x._4.shiftLeft(6));
        m[12] = t;
        m[13] = (t.shiftRight(8));
        m[14] = (t.shiftRight(16));
        m[15] = (t.shiftRight(24));
        t = (t.shiftRight(32)).add(x._5).add(x._6.shiftLeft(25));
        m[16] = t;
        m[17] = (t.shiftRight(8));
        m[18] = (t.shiftRight(16));
        m[19] = (t.shiftRight(24));

        t = (t.shiftRight(32)).add(x._7.shiftLeft(19));
        m[20] = t;
        m[21] = (t.shiftRight(8));
        m[22] = (t.shiftRight(16));
        m[23] = (t.shiftRight(24));
        t = (t.shiftRight(32)).add(x._8.shiftLeft(12));
        m[24] = t;
        m[25] = (t.shiftRight(8));
        m[26] = (t.shiftRight(16));
        m[27] = (t.shiftRight(24));
        t = (t.shiftRight(32)).add((x._9.add(ud)).shiftLeft(6));
        m[28] =  t;
        m[29] =  (t.shiftRight(8));
        m[30] =  (t.shiftRight(16));
        m[31] =  (t.shiftRight(24));
    }

    //endregion

    function createUnpackedArray () {
        return new Uint16Array(UNPACKED_SIZE);
    }

    function createUnpackedInt16Array () {
        return new Int16Array(UNPACKED_SIZE);
    }

    /* Copy a number */
    function cpy (d, s) {
        for (var i = 0; i < UNPACKED_SIZE; ++i)
            d[i] = s[i];
    }

    /* Set a number to value, which must be in range -185861411 .. 185861411 */
    function set (d, s) {
        d[0] = s;
        for (var i = 1; i < UNPACKED_SIZE; ++i)
            d[i] = 0;
    }

    function setJava(out, inp) {
        out._0 = inp;
        out._1 = bigInt(0);
        out._2 = bigInt(0);
        out._3 = bigInt(0);
        out._4 = bigInt(0);
        out._5 = bigInt(0);
        out._6 = bigInt(0);
        out._7 = bigInt(0);
        out._8 = bigInt(0);
        out._9 = bigInt(0);
    }

    /* Add/subtract two numbers.  The inputs must be in reduced form, and the
     * output isn't, so to do another addition or subtraction on the output,
     * first multiply it by one to reduce it. */
    var add = c255laddmodp;
    var sub = c255lsubmodp;

    var addJava = c255laddmodpJava;
    var subJava = c255lsubmodpJava;

    /* Multiply a number by a small integer in range -185861411 .. 185861411.
     * The output is in reduced form, the input x need not be.  x and xy may point
     * to the same buffer. */
    var mul_small = c255lmulasmall;

    /* Multiply two numbers.  The output is in reduced form, the inputs need not be. */
    var mul = c255lmulmodp;

    var mulJava = mulJava;

    /* Square a number.  Optimization of  mul25519(x2, x, x)  */
    var sqr = c255lsqrmodp;
    var sqrJava = sqrJava;
    // var sqrJava = c255lsqrmodpJava;

    /* Calculates a reciprocal.  The output is in reduced form, the inputs need not
     * be.  Simply calculates  y = x^(p-2)  so it's not too fast. */
    /* When sqrtassist is true, it instead calculates y = x^((p-5)/8) */
    function recip (y, x, sqrtassist) {
        var t0 = createUnpackedArray();
        var t1 = createUnpackedArray();
        var t2 = createUnpackedArray();
        var t3 = createUnpackedArray();
        var t4 = createUnpackedArray();

        /* the chain for x^(2^255-21) is straight from djb's implementation */
        var i;
        sqr(t1, x); /*  2 === 2 * 1	*/
        sqr(t2, t1); /*  4 === 2 * 2	*/
        sqr(t0, t2); /*  8 === 2 * 4	*/
        mul(t2, t0, x); /*  9 === 8 + 1	*/
        mul(t0, t2, t1); /* 11 === 9 + 2	*/
        sqr(t1, t0); /* 22 === 2 * 11	*/
        mul(t3, t1, t2); /* 31 === 22 + 9 === 2^5   - 2^0	*/
        sqr(t1, t3); /* 2^6   - 2^1	*/
        sqr(t2, t1); /* 2^7   - 2^2	*/
        sqr(t1, t2); /* 2^8   - 2^3	*/
        sqr(t2, t1); /* 2^9   - 2^4	*/
        sqr(t1, t2); /* 2^10  - 2^5	*/
        mul(t2, t1, t3); /* 2^10  - 2^0	*/
        sqr(t1, t2); /* 2^11  - 2^1	*/
        sqr(t3, t1); /* 2^12  - 2^2	*/
        for (i = 1; i < 5; i++) {
            sqr(t1, t3);
            sqr(t3, t1);
        } /* t3 */ /* 2^20  - 2^10	*/
        mul(t1, t3, t2); /* 2^20  - 2^0	*/
        sqr(t3, t1); /* 2^21  - 2^1	*/
        sqr(t4, t3); /* 2^22  - 2^2	*/
        for (i = 1; i < 10; i++) {
            sqr(t3, t4);
            sqr(t4, t3);
        } /* t4 */ /* 2^40  - 2^20	*/
        mul(t3, t4, t1); /* 2^40  - 2^0	*/
        for (i = 0; i < 5; i++) {
            sqr(t1, t3);
            sqr(t3, t1);
        } /* t3 */ /* 2^50  - 2^10	*/
        mul(t1, t3, t2); /* 2^50  - 2^0	*/
        sqr(t2, t1); /* 2^51  - 2^1	*/
        sqr(t3, t2); /* 2^52  - 2^2	*/
        for (i = 1; i < 25; i++) {
            sqr(t2, t3);
            sqr(t3, t2);
        } /* t3 */ /* 2^100 - 2^50 */
        mul(t2, t3, t1); /* 2^100 - 2^0	*/
        sqr(t3, t2); /* 2^101 - 2^1	*/
        sqr(t4, t3); /* 2^102 - 2^2	*/
        for (i = 1; i < 50; i++) {
            sqr(t3, t4);
            sqr(t4, t3);
        } /* t4 */ /* 2^200 - 2^100 */
        mul(t3, t4, t2); /* 2^200 - 2^0	*/
        for (i = 0; i < 25; i++) {
            sqr(t4, t3);
            sqr(t3, t4);
        } /* t3 */ /* 2^250 - 2^50	*/
        mul(t2, t3, t1); /* 2^250 - 2^0	*/
        sqr(t1, t2); /* 2^251 - 2^1	*/
        sqr(t2, t1); /* 2^252 - 2^2	*/
        if (sqrtassist !== 0) {
            mul(y, x, t2); /* 2^252 - 3 */
        } else {
            sqr(t1, t2); /* 2^253 - 2^3	*/
            sqr(t2, t1); /* 2^254 - 2^4	*/
            sqr(t1, t2); /* 2^255 - 2^5	*/
            mul(y, t1, t0); /* 2^255 - 21	*/
        }
    }

    function recipJava (y, x, sqrtassist) {
        var t0 = createUnpackedArray();
        var t1 = createUnpackedArray();
        var t2 = createUnpackedArray();
        var t3 = createUnpackedArray();
        var t4 = createUnpackedArray();

        /* the chain for x^(2^255-21) is straight from djb's implementation */
        var i;
        sqrJava(t1, x); /*  2 === 2 * 1	*/
        sqrJava(t2, t1); /*  4 === 2 * 2	*/
        sqrJava(t0, t2); /*  8 === 2 * 4	*/
        mulJava(t2, t0, x); /*  9 === 8 + 1	*/
        mulJava(t0, t2, t1); /* 11 === 9 + 2	*/
        sqrJava(t1, t0); /* 22 === 2 * 11	*/
        mulJava(t3, t1, t2); /* 31 === 22 + 9 === 2^5   - 2^0	*/
        sqrJava(t1, t3); /* 2^6   - 2^1	*/
        sqrJava(t2, t1); /* 2^7   - 2^2	*/
        sqrJava(t1, t2); /* 2^8   - 2^3	*/
        sqrJava(t2, t1); /* 2^9   - 2^4	*/
        sqrJava(t1, t2); /* 2^10  - 2^5	*/
        mulJava(t2, t1, t3); /* 2^10  - 2^0	*/
        sqrJava(t1, t2); /* 2^11  - 2^1	*/
        sqrJava(t3, t1); /* 2^12  - 2^2	*/
        for (i = 1; i < 5; i++) {
            sqrJava(t1, t3);
            sqrJava(t3, t1);
        } /* t3 */ /* 2^20  - 2^10	*/
        mulJava(t1, t3, t2); /* 2^20  - 2^0	*/
        sqrJava(t3, t1); /* 2^21  - 2^1	*/
        sqrJava(t4, t3); /* 2^22  - 2^2	*/
        for (i = 1; i < 10; i++) {
            sqrJava(t3, t4);
            sqrJava(t4, t3);
        } /* t4 */ /* 2^40  - 2^20	*/
        mulJava(t3, t4, t1); /* 2^40  - 2^0	*/
        for (i = 0; i < 5; i++) {
            sqrJava(t1, t3);
            sqrJava(t3, t1);
        } /* t3 */ /* 2^50  - 2^10	*/
        mulJava(t1, t3, t2); /* 2^50  - 2^0	*/
        sqrJava(t2, t1); /* 2^51  - 2^1	*/
        sqrJava(t3, t2); /* 2^52  - 2^2	*/
        for (i = 1; i < 25; i++) {
            sqrJava(t2, t3);
            sqrJava(t3, t2);
        } /* t3 */ /* 2^100 - 2^50 */
        mulJava(t2, t3, t1); /* 2^100 - 2^0	*/
        sqrJava(t3, t2); /* 2^101 - 2^1	*/
        sqrJava(t4, t3); /* 2^102 - 2^2	*/
        for (i = 1; i < 50; i++) {
            sqrJava(t3, t4);
            sqrJava(t4, t3);
        } /* t4 */ /* 2^200 - 2^100 */
        mulJava(t3, t4, t2); /* 2^200 - 2^0	*/
        for (i = 0; i < 25; i++) {
            sqrJava(t4, t3);
            sqrJava(t3, t4);
        } /* t3 */ /* 2^250 - 2^50	*/
        mulJava(t2, t3, t1); /* 2^250 - 2^0	*/
        sqrJava(t1, t2); /* 2^251 - 2^1	*/
        sqrJava(t2, t1); /* 2^252 - 2^2	*/
        if (sqrtassist !== 0) {
            mulJava(y, x, t2); /* 2^252 - 3 */
        } else {
            sqrJava(t1, t2); /* 2^253 - 2^3	*/
            sqrJava(t2, t1); /* 2^254 - 2^4	*/
            sqrJava(t1, t2); /* 2^255 - 2^5	*/
            mulJava(y, t1, t0); /* 2^255 - 21	*/
        }
    }

    /* checks if x is "negative", requires reduced input */
    function is_negative (x) {
        var isOverflowOrNegative = is_overflow(x) || x[9] < 0;
        var leastSignificantBit = x[0] & 1;
        return ((isOverflowOrNegative ? 1 : 0) ^ leastSignificantBit) & 0xFFFFFFFF;
    }

    /* a square root */
    function sqrt (x, u) {
        var v = createUnpackedArray();
        var t1 = createUnpackedArray();
        var t2 = createUnpackedArray();

        add(t1, u, u); /* t1 = 2u		*/
        recip(v, t1, 1); /* v = (2u)^((p-5)/8)	*/
        sqr(x, v); /* x = v^2		*/
        mul(t2, t1, x); /* t2 = 2uv^2		*/
        sub(t2, t2, C1); /* t2 = 2uv^2-1		*/
        mul(t1, v, t2); /* t1 = v(2uv^2-1)	*/
        mul(x, u, t1); /* x = uv(2uv^2-1)	*/
    }

    function sqrJava(x2, x) {
        var
        x_0=x._0,x_1=x._1,x_2=x._2,x_3=x._3,x_4=x._4,
            x_5=x._5,x_6=x._6,x_7=x._7,x_8=x._8,x_9=x._9;
        var t;
        t = (x_4.multiply(x_4))
            .add((bigInt(2).multiply((x_0.multiply(x_8)).add(x_2.multiply(x_6)))))
            .add((bigInt(38).multiply(x_9*x_9)))
            .add((bigInt(4).multiply((x_1.multiply(x_7)).add(x_3.multiply(x_5)))));


        x2._8 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));
        t = (t.shiftRight(26))
            .add((bigInt(2).multiply((x_0.multiply(x_9)).add(x_1.multiply(x_8)).add(x_2.multiply(x_7)).add(x_3.multiply(x_6)).add(x_4.multiply(x_5)))));


        x2._9 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));
        t = bigInt(19).multiply(t.shiftRight(25)).add(x_0.multiply(x_0))
            .add((bigInt(38).multiply((x_2.multiply(x_8)).add(x_4.multiply(x_6)).add(x_5.multiply(x_5)))))
            .add((bigInt(76).multiply((x_1.multiply(x_9)).add(x_3.multiply(x_7)))));


        x2._0 = (t.and((bigInt(1).shiftLeft(26).minus(1))));
        t = (t.shiftRight(26))
            .add(bigInt(2) .multiply(x_0 .multiply(x_1)))
            .add(bigInt(38).multiply((x_2.multiply(x_9)).add(x_3.multiply(x_8)).add(x_4.multiply(x_7)).add(x_5.multiply(x_6))));


        x2._1 = (t.and((bigInt(1).shiftLeft(25).minus(1))));
        t = (t.shiftRight(25))
            .add((bigInt(19).multiply((x_6.multiply(x_6)))))
            .add((bigInt(2 ).multiply((x_0.multiply(x_2)) + (x_1.multiply(x_1)))))
            .add((bigInt(38).multiply( x_4.multiply(x_8))))
            .add((bigInt(76).multiply((x_3.multiply(x_9)) + (x_5.multiply(x_7)))));



        x2._2 = (t.and((bigInt(1).shiftLeft(26).minus(1))));
        t = (t.shiftRight(26))
            .add(bigInt(2 ) .multiply((x_0.multiply(x_3)).add(x_1.multiply(x_2))))
            .add(bigInt(38) .multiply((x_4.multiply(x_9)).add(x_5.multiply(x_8)).add(x_6.multiply(x_7))));

        x2._3 = (t.and((bigInt(1).shiftLeft(25).minus(1))));
        t = (t.shiftRight(25)).add(x_2.multiply(x_2))
            .add((bigInt(2 ).multiply(x_0.multiply(x_4))))
            .add((bigInt(38).multiply(x_6.multiply(x_8).add(x_7.multiply(x_7)))))
            .add((bigInt(4 ).multiply(x_1.multiply(x_3))))
            .add((bigInt(76).multiply(x_5.multiply(x_9))));

        x2._4 = (t.and((bigInt(1).shiftLeft(26).minus(1))));
        t = (t.shiftRight(26))
            .add((bigInt(2 ).multiply((x_0.multiply(x_5)).add(x_1.multiply(x_4)).add(x_2.multiply(x_3)))))
            .add((bigInt(38).multiply((x_6.multiply(x_9)).add(x_7.multiply(x_8)))));

        x2._5 = (t.and((bigInt(1).shiftLeft(25).minus(1))));
        t = (t.shiftRight(25))
            .add((bigInt(19).multiply(x_8 .multiply(x_8))))
            .add((bigInt(2 ).multiply((x_0.multiply(x_6)).add((x_2.multiply(x_4)).add((x_3.multiply((x_3))))))))
            .add((bigInt(4 ).multiply(x_1 .multiply(x_5))))
            .add((bigInt(76).multiply(x_7 .multiply(x_9))));

        x2._6 = (t.and((bigInt(1).shiftLeft(26).minus(1))));
        t = (t.shiftRight(26))
            .add((bigInt(2 ).multiply(((x_0.multiply(x_7)).add(x_1.multiply(x_6)).add(x_2.multiply(x_5)).add(x_3.multiply(x_4))))))
            .add((bigInt(38).multiply(x_8  .multiply(x_9))));

        x2._7 = (t.and((bigInt(1).shiftLeft(25).minus(1))));
        t = (t.shiftRight(25)).add(x2._8);

        x2._8 = (t.and((bigInt(1).shiftLeft(26).minus(1))));
        x2._9 = x2._9.add(t.shiftRight(26));
        return x2;
    };



    function cpyJava(out, inp) {
        out._0 = inp._0;
        out._1 = inp._1;
        out._2 = inp._2;
        out._3 = inp._3;
        out._4 = inp._4;
        out._5 = inp._5;
        out._6 = inp._6;
        out._7 = inp._7;
        out._8 = inp._8;
        out._9 = inp._9;
    }

    //endregion

    //region JavaScript Fast Math

    function c255lsqr8h (a7, a6, a5, a4, a3, a2, a1, a0) {
        var r = [];
        var v;
        r[0] = (v = a0*a0) & 0xFFFF;
        r[1] = (v = ((v / 0x10000) | 0) + 2*a0*a1) & 0xFFFF;
        r[2] = (v = ((v / 0x10000) | 0) + 2*a0*a2 + a1*a1) & 0xFFFF;
        r[3] = (v = ((v / 0x10000) | 0) + 2*a0*a3 + 2*a1*a2) & 0xFFFF;
        r[4] = (v = ((v / 0x10000) | 0) + 2*a0*a4 + 2*a1*a3 + a2*a2) & 0xFFFF;
        r[5] = (v = ((v / 0x10000) | 0) + 2*a0*a5 + 2*a1*a4 + 2*a2*a3) & 0xFFFF;
        r[6] = (v = ((v / 0x10000) | 0) + 2*a0*a6 + 2*a1*a5 + 2*a2*a4 + a3*a3) & 0xFFFF;
        r[7] = (v = ((v / 0x10000) | 0) + 2*a0*a7 + 2*a1*a6 + 2*a2*a5 + 2*a3*a4) & 0xFFFF;
        r[8] = (v = ((v / 0x10000) | 0) + 2*a1*a7 + 2*a2*a6 + 2*a3*a5 + a4*a4) & 0xFFFF;
        r[9] = (v = ((v / 0x10000) | 0) + 2*a2*a7 + 2*a3*a6 + 2*a4*a5) & 0xFFFF;
        r[10] = (v = ((v / 0x10000) | 0) + 2*a3*a7 + 2*a4*a6 + a5*a5) & 0xFFFF;
        r[11] = (v = ((v / 0x10000) | 0) + 2*a4*a7 + 2*a5*a6) & 0xFFFF;
        r[12] = (v = ((v / 0x10000) | 0) + 2*a5*a7 + a6*a6) & 0xFFFF;
        r[13] = (v = ((v / 0x10000) | 0) + 2*a6*a7) & 0xFFFF;
        r[14] = (v = ((v / 0x10000) | 0) + a7*a7) & 0xFFFF;
        r[15] = ((v / 0x10000) | 0);
        return r;
    }

    function c255lsqrmodp (r, a) {
        var x = c255lsqr8h(a[15], a[14], a[13], a[12], a[11], a[10], a[9], a[8]);
        var z = c255lsqr8h(a[7], a[6], a[5], a[4], a[3], a[2], a[1], a[0]);
        var y = c255lsqr8h(a[15] + a[7], a[14] + a[6], a[13] + a[5], a[12] + a[4], a[11] + a[3], a[10] + a[2], a[9] + a[1], a[8] + a[0]);

        var v;
        r[0] = (v = 0x800000 + z[0] + (y[8] -x[8] -z[8] + x[0] -0x80) * 38) & 0xFFFF;
        r[1] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[1] + (y[9] -x[9] -z[9] + x[1]) * 38) & 0xFFFF;
        r[2] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[2] + (y[10] -x[10] -z[10] + x[2]) * 38) & 0xFFFF;
        r[3] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[3] + (y[11] -x[11] -z[11] + x[3]) * 38) & 0xFFFF;
        r[4] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[4] + (y[12] -x[12] -z[12] + x[4]) * 38) & 0xFFFF;
        r[5] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[5] + (y[13] -x[13] -z[13] + x[5]) * 38) & 0xFFFF;
        r[6] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[6] + (y[14] -x[14] -z[14] + x[6]) * 38) & 0xFFFF;
        r[7] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[7] + (y[15] -x[15] -z[15] + x[7]) * 38) & 0xFFFF;
        r[8] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[8] + y[0] -x[0] -z[0] + x[8] * 38) & 0xFFFF;
        r[9] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[9] + y[1] -x[1] -z[1] + x[9] * 38) & 0xFFFF;
        r[10] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[10] + y[2] -x[2] -z[2] + x[10] * 38) & 0xFFFF;
        r[11] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[11] + y[3] -x[3] -z[3] + x[11] * 38) & 0xFFFF;
        r[12] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[12] + y[4] -x[4] -z[4] + x[12] * 38) & 0xFFFF;
        r[13] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[13] + y[5] -x[5] -z[5] + x[13] * 38) & 0xFFFF;
        r[14] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[14] + y[6] -x[6] -z[6] + x[14] * 38) & 0xFFFF;
        var r15 = 0x7fff80 + ((v / 0x10000) | 0) + z[15] + y[7] -x[7] -z[7] + x[15] * 38;
        c255lreduce(r, r15);
    }

    function c255lmul8h (a7, a6, a5, a4, a3, a2, a1, a0, b7, b6, b5, b4, b3, b2, b1, b0) {
        var r = [];
        var v;
        r[0] = (v = a0*b0) & 0xFFFF;
        r[1] = (v = ((v / 0x10000) | 0) + a0*b1 + a1*b0) & 0xFFFF;
        r[2] = (v = ((v / 0x10000) | 0) + a0*b2 + a1*b1 + a2*b0) & 0xFFFF;
        r[3] = (v = ((v / 0x10000) | 0) + a0*b3 + a1*b2 + a2*b1 + a3*b0) & 0xFFFF;
        r[4] = (v = ((v / 0x10000) | 0) + a0*b4 + a1*b3 + a2*b2 + a3*b1 + a4*b0) & 0xFFFF;
        r[5] = (v = ((v / 0x10000) | 0) + a0*b5 + a1*b4 + a2*b3 + a3*b2 + a4*b1 + a5*b0) & 0xFFFF;
        r[6] = (v = ((v / 0x10000) | 0) + a0*b6 + a1*b5 + a2*b4 + a3*b3 + a4*b2 + a5*b1 + a6*b0) & 0xFFFF;
        r[7] = (v = ((v / 0x10000) | 0) + a0*b7 + a1*b6 + a2*b5 + a3*b4 + a4*b3 + a5*b2 + a6*b1 + a7*b0) & 0xFFFF;
        r[8] = (v = ((v / 0x10000) | 0) + a1*b7 + a2*b6 + a3*b5 + a4*b4 + a5*b3 + a6*b2 + a7*b1) & 0xFFFF;
        r[9] = (v = ((v / 0x10000) | 0) + a2*b7 + a3*b6 + a4*b5 + a5*b4 + a6*b3 + a7*b2) & 0xFFFF;
        r[10] = (v = ((v / 0x10000) | 0) + a3*b7 + a4*b6 + a5*b5 + a6*b4 + a7*b3) & 0xFFFF;
        r[11] = (v = ((v / 0x10000) | 0) + a4*b7 + a5*b6 + a6*b5 + a7*b4) & 0xFFFF;
        r[12] = (v = ((v / 0x10000) | 0) + a5*b7 + a6*b6 + a7*b5) & 0xFFFF;
        r[13] = (v = ((v / 0x10000) | 0) + a6*b7 + a7*b6) & 0xFFFF;
        r[14] = (v = ((v / 0x10000) | 0) + a7*b7) & 0xFFFF;
        r[15] = ((v / 0x10000) | 0);
        return r;
    }

    function c255lmulmodp (r, a, b) {
        // Karatsuba multiplication scheme: x*y = (b^2+b)*x1*y1 - b*(x1-x0)*(y1-y0) + (b+1)*x0*y0
        var x = c255lmul8h(a[15], a[14], a[13], a[12], a[11], a[10], a[9], a[8], b[15], b[14], b[13], b[12], b[11], b[10], b[9], b[8]);
        var z = c255lmul8h(a[7], a[6], a[5], a[4], a[3], a[2], a[1], a[0], b[7], b[6], b[5], b[4], b[3], b[2], b[1], b[0]);
        var y = c255lmul8h(a[15] + a[7], a[14] + a[6], a[13] + a[5], a[12] + a[4], a[11] + a[3], a[10] + a[2], a[9] + a[1], a[8] + a[0],
            b[15] + b[7], b[14] + b[6], b[13] + b[5], b[12] + b[4], b[11] + b[3], b[10] + b[2], b[9] + b[1], b[8] + b[0]);

        var v;
        r[0] = (v = 0x800000 + z[0] + (y[8] -x[8] -z[8] + x[0] -0x80) * 38) & 0xFFFF;
        r[1] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[1] + (y[9] -x[9] -z[9] + x[1]) * 38) & 0xFFFF;
        r[2] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[2] + (y[10] -x[10] -z[10] + x[2]) * 38) & 0xFFFF;
        r[3] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[3] + (y[11] -x[11] -z[11] + x[3]) * 38) & 0xFFFF;
        r[4] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[4] + (y[12] -x[12] -z[12] + x[4]) * 38) & 0xFFFF;
        r[5] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[5] + (y[13] -x[13] -z[13] + x[5]) * 38) & 0xFFFF;
        r[6] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[6] + (y[14] -x[14] -z[14] + x[6]) * 38) & 0xFFFF;
        r[7] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[7] + (y[15] -x[15] -z[15] + x[7]) * 38) & 0xFFFF;
        r[8] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[8] + y[0] -x[0] -z[0] + x[8] * 38) & 0xFFFF;
        r[9] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[9] + y[1] -x[1] -z[1] + x[9] * 38) & 0xFFFF;
        r[10] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[10] + y[2] -x[2] -z[2] + x[10] * 38) & 0xFFFF;
        r[11] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[11] + y[3] -x[3] -z[3] + x[11] * 38) & 0xFFFF;
        r[12] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[12] + y[4] -x[4] -z[4] + x[12] * 38) & 0xFFFF;
        r[13] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[13] + y[5] -x[5] -z[5] + x[13] * 38) & 0xFFFF;
        r[14] = (v = 0x7fff80 + ((v / 0x10000) | 0) + z[14] + y[6] -x[6] -z[6] + x[14] * 38) & 0xFFFF;
        var r15 = 0x7fff80 + ((v / 0x10000) | 0) + z[15] + y[7] -x[7] -z[7] + x[15] * 38;
        c255lreduce(r, r15);
    }

    // TODO: migrate to bigint operations
    function mulJava (xy, x, y) {
        // Karatsuba multiplication scheme: x*y = (b^2+b)*x1*y1 - b*(x1-x0)*(y1-y0) + (b+1)*x0*y0

        /* sahn0:
        * Using local variables to avoid class access.
        * This seem to improve performance a bit...
        */
        var
        x_0=x._0,x_1=x._1,x_2=x._2,x_3=x._3,x_4=x._4,
            x_5=x._5,x_6=x._6,x_7=x._7,x_8=x._8,x_9=x._9;
        var
        y_0=y._0,y_1=y._1,y_2=y._2,y_3=y._3,y_4=y._4,
            y_5=y._5,y_6=y._6,y_7=y._7,y_8=y._8,y_9=y._9;

        //
        var t;
        t = (x_0.multiply(y_8)).add(x_2.multiply(y_6)).add(x_4.multiply(y_4)).add(x_6.multiply(y_2))
            .add(x_8.multiply(y_0)).add(bigInt(2).multiply((x_1.multiply(y_7)).add(x_3.multiply(y_5)).add
                (x_5.multiply(y_3)).add(x_7.multiply(y_1)))).add(bigInt(38).multiply(x_9.multiply(y_9)));


        xy._8 = t.and(bigInt(1).shiftLeft(26).minus(1));


        t = (t.shiftRight(26)).add(x_0.multiply(y_9)).add(x_1.multiply(y_8)).add(x_2.multiply(y_7))
                .add(x_3.multiply(y_6)).add(x_4.multiply(y_5)).add(x_5.multiply(y_4))
                .add(x_6.multiply(y_3)).add(x_7.multiply(y_2)).add(x_8.multiply(y_1))
                .add(x_9.multiply(y_0));

        xy._9 = (t.and(bigInt(1).shiftLeft(25).minus(1)));


        t = (x_0.multiply(y_0)).add((bigInt(19)).multiply((t.shiftRight(25)).add(x_2.multiply(y_8)).add(x_4.multiply(y_6))
                .add(x_6.multiply(y_4)).add(x_8.multiply(y_2)))).add(bigInt(38)
                .multiply((x_1.multiply(y_9)).add(x_3.multiply(y_7)).add(x_5.multiply(y_5))
                .add(x_7.multiply(y_3)).add(x_9.multiply(y_1))));


        xy._0 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));


        t = (t.shiftRight(26)).add(x_0.multiply(y_1)).add(x_1.multiply(y_0)).add((bigInt(19)).multiply((x_2.multiply(y_9))
                .add(x_3.multiply(y_8)).add(x_4.multiply(y_7)).add(x_5.multiply(y_6))
                .add(x_6.multiply(y_5)).add(x_7.multiply(y_4)).add(x_8.multiply(y_3))
                .add(x_9.multiply(y_2))));


        xy._1 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));

        t = (t.shiftRight(25)).add(x_0.multiply(y_2)).add(x_2.multiply(y_0)).add((bigInt(19)).multiply((x_4.multiply(y_8))
                .add(x_6.multiply(y_6)).add(x_8.multiply(y_4))))
                .add((bigInt(2)).multiply(x_1.multiply(y_1)))
                .add((bigInt(38)).multiply((x_3.multiply(y_9)).add(x_5.multiply(y_7))
                .add(x_7.multiply(y_5)).add(x_9.multiply(y_3))));


        xy._2 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));

        t = (t.shiftRight(26)).add(x_0.multiply(y_3)).add(x_1.multiply(y_2)).add(x_2.multiply(y_1))
                .add(x_3.multiply(y_0)).add((bigInt(19)).multiply((x_4.multiply(y_9)).add(x_5.multiply(y_8))
                .add(x_6.multiply(y_7)).add(x_7.multiply(y_6))
                .add(x_8.multiply(y_5)).add(x_9.multiply(y_4))));

        xy._3 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));

        t = (t.shiftRight(25)).add(x_0.multiply(y_4)).add(x_2.multiply(y_2)).add(x_4.multiply(y_0)).add((bigInt(19)
                .multiply((x_6.multiply(y_8)).add(x_8.multiply(y_6)))).add((bigInt(2)).multiply((x_1.multiply(y_3))
                .add(x_3*y_1))).add((bigInt(38)).multiply((x_5.multiply(y_9)).add(x_7.multiply(y_7)).add(x_9.multiply(y_5)))));

        xy._4 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));

        t = (t.shiftRight(26)).add(x_0.multiply(y_5)).add(x_1.multiply(y_4)).add(x_2.multiply(y_3))
            .add(x_3.multiply(y_2)).add(x_4.multiply(y_1)).add(x_5.multiply(y_0))
            .add((bigInt(19)).multiply((x_6.multiply(y_9)).add(x_7.multiply(y_8)).add(x_8.multiply(y_7)).add(x_9.multiply(y_6))));

        xy._5 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));

        t = (t.shiftRight(25)).add(x_0.multiply(y_6)).add(x_2.multiply(y_4)).add(x_4.multiply(y_2))
            .add(x_6.multiply(y_0)).add((bigInt(19)).multiply(x_8.multiply(y_8))).add((bigInt(2)).multiply((x_1.multiply(y_5))
            .add(x_3.multiply(y_3)).add(x_5.multiply(y_1))))
            .add((bigInt(38).multiply((x_7.multiply(y_9)).add(x_9.multiply(y_7)))));


        xy._6 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));

        t = (t.shiftRight(26)).add(x_0.multiply(y_7)).add(x_1.multiply(y_6)).add(x_2.multiply(y_5))
            .add(x_3.multiply(y_4)).add(x_4.multiply(y_3)).add(x_5.multiply(y_2))
            .add(x_6.multiply(y_1)).add(x_7.multiply(y_0)).add((bigInt(19)).multiply((x_8.multiply(y_9))
            .add(x_9.multiply(y_8))));

        xy._7 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));

        t = (t.shiftRight(25)).add(xy._8);

        xy._8 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));
        xy._9 =  xy._9.add(t.shiftRight(26));

        return xy;
    }

    function c255lreduce (a, a15) {
        var v = a15;
        a[15] = v & 0x7FFF;
        v = ((v / 0x8000) | 0) * 19;
        for (var i = 0; i <= 14; ++i) {
            a[i] = (v += a[i]) & 0xFFFF;
            v = ((v / 0x10000) | 0);
        }

        a[15] += v;
    }

    function c255laddmodp (r, a, b) {
        var v;
        r[0] = (v = (((a[15] / 0x8000) | 0) + ((b[15] / 0x8000) | 0)) * 19 + a[0] + b[0]) & 0xFFFF;
        for (var i = 1; i <= 14; ++i)
            r[i] = (v = ((v / 0x10000) | 0) + a[i] + b[i]) & 0xFFFF;

        r[15] = ((v / 0x10000) | 0) + (a[15] & 0x7FFF) + (b[15] & 0x7FFF);
    }

    // TODO: migrate to bigint
    function c255laddmodpJava (xy, x, y) {
        xy._0 = x._0.add(y._0);    xy._1 = x._1.add(y._1);
        xy._2 = x._2.add(y._2);    xy._3 = x._3.add(y._3);
        xy._4 = x._4.add(y._4);    xy._5 = x._5.add(y._5);
        xy._6 = x._6.add(y._6);    xy._7 = x._7.add(y._7);
        xy._8 = x._8.add(y._8);    xy._9 = x._9.add(y._9);
    }

    function c255lsubmodp (r, a, b) {
        var v;
        r[0] = (v = 0x80000 + (((a[15] / 0x8000) | 0) - ((b[15] / 0x8000) | 0) - 1) * 19 + a[0] - b[0]) & 0xFFFF;
        for (var i = 1; i <= 14; ++i)
            r[i] = (v = ((v / 0x10000) | 0) + 0x7fff8 + a[i] - b[i]) & 0xFFFF;

        r[15] = ((v / 0x10000) | 0) + 0x7ff8 + (a[15] & 0x7FFF) - (b[15] & 0x7FFF);
    }

    // TODO: migrate to bigint
    function c255lsubmodpJava (xy, x, y) {
        xy._0 = x._0.minus(y._0);    xy._1 = x._1.minus(y._1);
        xy._2 = x._2.minus(y._2);    xy._3 = x._3.minus(y._3);
        xy._4 = x._4.minus(y._4);    xy._5 = x._5.minus(y._5);
        xy._6 = x._6.minus(y._6);    xy._7 = x._7.minus(y._7);
        xy._8 = x._8.minus(y._8);    xy._9 = x._9.minus(y._9);
    }

    function c255lmulasmall (r, a, m) {
        var v;
        r[0] = (v = a[0] * m) & 0xFFFF;
        for (var i = 1; i <= 14; ++i)
            r[i] = (v = ((v / 0x10000) | 0) + a[i]*m) & 0xFFFF;

        var r15 = ((v / 0x10000) | 0) + a[15]*m;
        c255lreduce(r, r15);
    }

    //endregion

    /********************* Elliptic curve *********************/

    /* y^2 = x^3 + 486662 x^2 + x  over GF(2^255-19) */

    /* t1 = ax + az
     * t2 = ax - az  */
    function mont_prep (t1, t2, ax, az) {
        add(t1, ax, az);
        sub(t2, ax, az);
    }

    function mont_prepJava (t1, t2, ax, az) {

        c255laddmodpJava(t1, ax, az);
        c255lsubmodpJava(t2, ax, az);
    }

    /* A = P + Q   where
     *  X(A) = ax/az
     *  X(P) = (t1+t2)/(t1-t2)
     *  X(Q) = (t3+t4)/(t3-t4)
     *  X(P-Q) = dx
     * clobbers t1 and t2, preserves t3 and t4  */
    function mont_add (t1, t2, t3, t4, ax, az, dx) {
        mul(ax, t2, t3);
        mul(az, t1, t4);
        add(t1, ax, az);
        sub(t2, ax, az);
        sqr(ax, t1);
        sqr(t1, t2);
        mul(az, t1, dx);
    }


    function mont_addJava (t1, t2, t3, t4, ax, az, dx) {
        mulJava(ax, t2, t3);
        mulJava(az, t1, t4);
        addJava(t1, ax, az);
        subJava(t2, ax, az);
        sqrJava(ax, t1);
        sqrJava(t1, t2);
        mulJava(az, t1, dx);
    }

    function mont_dblJava(t1,  t2, t3, t4, bx, bz) {
        sqrJava(t1, t3);
        sqrJava(t2, t4);
        mulJava(bx, t1, t2);
        subJava(t2, t1, t2);
        mul_smallJava(bz, t2, 121665);
        addJava(t1, t1, bz);
        mulJava(bz, t1, t2);
    }

    function mul_smallJava(xy, x, y) {

        var t;
        t = (x._8.multiply(y));
        xy._8 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));
        t = (t.shiftRight(26)).add(x._9.multiply(y));
        xy._9 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));
        t = bigInt(19).multiply(t.shiftRight(25)).add(x._0.multiply(y));
        xy._0 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));
        t = (t.shiftRight(26)).add(x._1.multiply(y));
        xy._1 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));
        t = (t.shiftRight(25)).add(x._2.multiply(y));
        xy._2 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));
        t = (t.shiftRight(26)).add(x._3.multiply(y));
        xy._3 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));
        t = (t.shiftRight(25)).add(x._4.multiply(y));
        xy._4 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));
        t = (t.shiftRight(26)).add(x._5.multiply(y));
        xy._5 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));
        t = (t.shiftRight(25)).add(x._6.multiply(y));
        xy._6 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));
        t = (t.shiftRight(26)).add(x._7.multiply(y));
        xy._7 = (t.and((bigInt(1).shiftLeft(25)).minus(1)));
        t = (t.shiftRight(25)).add(xy._8);
        xy._8 = (t.and((bigInt(1).shiftLeft(26)).minus(1)));
        xy._9 = xy._9.add((t.shiftRight(26)));
        return xy;
    }


    /* B = 2 * Q   where
     *  X(B) = bx/bz
     *  X(Q) = (t3+t4)/(t3-t4)
     * clobbers t1 and t2, preserves t3 and t4  */
    function mont_dbl (t1, t2, t3, t4, bx, bz) {
        sqr(t1, t3);
        sqr(t2, t4);
        mul(bx, t1, t2);
        sub(t2, t1, t2);
        mul_small(bz, t2, 121665);
        add(t1, t1, bz);
        mul(bz, t1, t2);
    }

    /* Y^2 = X^3 + 486662 X^2 + X
     * t is a temporary  */
    function x_to_y2 (t, y2, x) {
        // C1 = [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

        sqr(t, x);
        mul_small(y2, x, 486662);
        add(t, t, y2);
        add(t, t, C1);
        mul(y2, t, x);
    }


    // function mul( xy,  x,  y) {
    //     /* sahn0:
    //      * Using local variables to avoid class access.
    //      * This seem to improve performance a bit...
    //      */
    //     long
    //     x_0=x._0,x_1=x._1,x_2=x._2,x_3=x._3,x_4=x._4,
    //         x_5=x._5,x_6=x._6,x_7=x._7,x_8=x._8,x_9=x._9;
    //     long
    //     y_0=y._0,y_1=y._1,y_2=y._2,y_3=y._3,y_4=y._4,
    //         y_5=y._5,y_6=y._6,y_7=y._7,y_8=y._8,y_9=y._9;
    //     long t;
    //     t = (x_0*y_8) + (x_2*y_6) + (x_4*y_4) + (x_6*y_2) +
    //         (x_8*y_0) + 2 * ((x_1*y_7) + (x_3*y_5) +
    //             (x_5*y_3) + (x_7*y_1)) + 38 *
    //         (x_9*y_9);
    //     xy._8 = (t & ((1 << 26) - 1));
    //     t = (t >> 26) + (x_0*y_9) + (x_1*y_8) + (x_2*y_7) +
    //         (x_3*y_6) + (x_4*y_5) + (x_5*y_4) +
    //         (x_6*y_3) + (x_7*y_2) + (x_8*y_1) +
    //         (x_9*y_0);
    //     xy._9 = (t & ((1 << 25) - 1));
    //     t = (x_0*y_0) + 19 * ((t >> 25) + (x_2*y_8) + (x_4*y_6)
    //         + (x_6*y_4) + (x_8*y_2)) + 38 *
    //         ((x_1*y_9) + (x_3*y_7) + (x_5*y_5) +
    //             (x_7*y_3) + (x_9*y_1));
    //     xy._0 = (t & ((1 << 26) - 1));
    //     t = (t >> 26) + (x_0*y_1) + (x_1*y_0) + 19 * ((x_2*y_9)
    //         + (x_3*y_8) + (x_4*y_7) + (x_5*y_6) +
    //         (x_6*y_5) + (x_7*y_4) + (x_8*y_3) +
    //         (x_9*y_2));
    //     xy._1 = (t & ((1 << 25) - 1));
    //     t = (t >> 25) + (x_0*y_2) + (x_2*y_0) + 19 * ((x_4*y_8)
    //         + (x_6*y_6) + (x_8*y_4)) + 2 * (x_1*y_1)
    //         + 38 * ((x_3*y_9) + (x_5*y_7) +
    //             (x_7*y_5) + (x_9*y_3));
    //     xy._2 = (t & ((1 << 26) - 1));
    //     t = (t >> 26) + (x_0*y_3) + (x_1*y_2) + (x_2*y_1) +
    //         (x_3*y_0) + 19 * ((x_4*y_9) + (x_5*y_8) +
    //             (x_6*y_7) + (x_7*y_6) +
    //             (x_8*y_5) + (x_9*y_4));
    //     xy._3 = (t & ((1 << 25) - 1));
    //     t = (t >> 25) + (x_0*y_4) + (x_2*y_2) + (x_4*y_0) + 19 *
    //         ((x_6*y_8) + (x_8*y_6)) + 2 * ((x_1*y_3) +
    //             (x_3*y_1)) + 38 *
    //         ((x_5*y_9) + (x_7*y_7) + (x_9*y_5));
    //     xy._4 = (t & ((1 << 26) - 1));
    //     t = (t >> 26) + (x_0*y_5) + (x_1*y_4) + (x_2*y_3) +
    //         (x_3*y_2) + (x_4*y_1) + (x_5*y_0) + 19 *
    //         ((x_6*y_9) + (x_7*y_8) + (x_8*y_7) +
    //             (x_9*y_6));
    //     xy._5 = (t & ((1 << 25) - 1));
    //     t = (t >> 25) + (x_0*y_6) + (x_2*y_4) + (x_4*y_2) +
    //         (x_6*y_0) + 19 * (x_8*y_8) + 2 * ((x_1*y_5) +
    //             (x_3*y_3) + (x_5*y_1)) + 38 *
    //         ((x_7*y_9) + (x_9*y_7));
    //     xy._6 = (t & ((1 << 26) - 1));
    //     t = (t >> 26) + (x_0*y_7) + (x_1*y_6) + (x_2*y_5) +
    //         (x_3*y_4) + (x_4*y_3) + (x_5*y_2) +
    //         (x_6*y_1) + (x_7*y_0) + 19 * ((x_8*y_9) +
    //             (x_9*y_8));
    //     xy._7 = (t & ((1 << 25) - 1));
    //     t = (t >> 25) + xy._8;
    //     xy._8 = (t & ((1 << 26) - 1));
    //     xy._9 += (t >> 26);
    //     return xy;
    // }

    /* P = kG   and  s = sign(P)/k  */
    function core (Px, s, k, Gx) {
        var dx = createUnpackedArray();
        var t1 = createUnpackedArray();
        var t2 = createUnpackedArray();
        var t3 = createUnpackedArray();
        var t4 = createUnpackedArray();
        var x = [createUnpackedArray(), createUnpackedArray()];
        var z = [createUnpackedArray(), createUnpackedArray()];
        var i, j;

        /* unpack the base */
        if (Gx !== null)
            unpack(dx, Gx);
        else
            set(dx, 9);

        /* 0G = point-at-infinity */
        set(x[0], 1);
        set(z[0], 0);

        /* 1G = G */
        cpy(x[1], dx);
        set(z[1], 1);

        for (i = 32; i-- !== 0;) {
            for (j = 8; j-- !== 0;) {
                /* swap arguments depending on bit */
                var bit1 = (k[i] & 0xFF) >> j & 1;
                var bit0 = ~(k[i] & 0xFF) >> j & 1;
                var ax = x[bit0];
                var az = z[bit0];
                var bx = x[bit1];
                var bz = z[bit1];

                /* a' = a + b	*/
                /* b' = 2 b	*/
                mont_prep(t1, t2, ax, az);
                mont_prep(t3, t4, bx, bz);
                mont_add(t1, t2, t3, t4, ax, az, dx);
                mont_dbl(t1, t2, t3, t4, bx, bz);
            }
        }

        recip(t1, z[0], 0);
        mul(dx, x[0], t1);

        pack(dx, Px);

        /* calculate s such that s abs(P) = G  .. assumes G is std base point */
        if (s !== null) {
            x_to_y2(t2, t1, dx); /* t1 = Py^2  */
            recip(t3, z[1], 0); /* where Q=P+G ... */
            mul(t2, x[1], t3); /* t2 = Qx  */
            add(t2, t2, dx); /* t2 = Qx + Px  */
            add(t2, t2, C486671); /* t2 = Qx + Px + Gx + 486662  */
            sub(dx, dx, C9); /* dx = Px - Gx  */
            sqr(t3, dx); /* t3 = (Px - Gx)^2  */
            mul(dx, t2, t3); /* dx = t2 (Px - Gx)^2  */
            sub(dx, dx, t1); /* dx = t2 (Px - Gx)^2 - Py^2  */
            sub(dx, dx, C39420360); /* dx = t2 (Px - Gx)^2 - Py^2 - Gy^2  */
            mul(t1, dx, BASE_R2Y); /* t1 = -Py  */

            if (is_negative(t1) !== 0)    /* sign is 1, so just copy  */
                cpy32(s, k);
            else            /* sign is -1, so negate  */
                mula_small(s, ORDER_TIMES_8, 0, k, 32, -1);

            /* reduce s mod q
             * (is this needed?  do it just in case, it's fast anyway) */
            //divmod((dstptr) t1, s, 32, order25519, 32);

            /* take reciprocal of s mod q */
            var temp1 = new Array(32);
            var temp2 = new Array(64);
            var temp3 = new Array(64);
            cpy32(temp1, ORDER);
            cpy32(s, egcd32(temp2, temp3, s, temp1));
            if ((s[31] & 0x80) !== 0)
                mula_small(s, s, 0, ORDER, 32, 1);

        }
    }

    /* P = kG   and  s = sign(P)/k  */
    /*
    * Px = sharedKey
    * s  = signment key (null)
    * k  = privateKey
    * Gx = publicKey
    * */
    function coreJava (Px, s, k, Gx) {
        var dx = new long10();
        var t1 = new long10();
        var t2 = new long10();
        var t3 = new long10();
        var t4 = new long10();
        var x = [new long10(), new long10()];
        var z = [new long10(), new long10()];
        var i, j;

        /* unpack the base */
        if (Gx !== null) {
            console.log(Gx);
            console.log(dx);

            unpackJava(dx, Gx);

            console.log(dx);

        }
        else
            setJava(dx, bigInt(9));

        /* 0G = point-at-infinity */
        setJava(x[0], bigInt(1));
        setJava(z[0], bigInt(0));


        console.log('=================');
        console.log(z[0]);

        /* 1G = G */
        cpyJava(x[1], dx); // Copy dx to x1
        setJava(z[1], bigInt(1));  //

        debugger;

        for (i = 32; i--!=0; ) {
            if (i==0) {
                i=0;
            }
            for (j = 8; j--!=0; ) {
                /* swap arguments depending on bit */
                var bit1 = (k[i] & 0xFF) >> j & 1;
                var bit0 = ~(k[i] & 0xFF) >> j & 1;
                var ax = long10(x[bit0]);
                var az = long10(z[bit0]);
                var bx = long10(x[bit1]);
                var bz = long10(z[bit1]);

                /* a' = a + b    */
                /* b' = 2 b    */
                mont_prep(t1, t2, ax, az);
                mont_prep(t3, t4, bx, bz);
                mont_add(t1, t2, t3, t4, ax, az, dx);
                mont_dbl(t1, t2, t3, t4, bx, bz);
            }
        }

        z[0] = new long10(z[0]);
        x[0] = new long10(x[0]);
        recipJava(t1, z[0], 0);

        mulJava(dx, x[0], t1);

        packJava(dx, Px);

        /* calculate s such that s abs(P) = G  .. assumes G is std base point */
    }


    /********* DIGITAL SIGNATURES *********/

    /* deterministic EC-KCDSA
     *
     *    s is the private key for signing
     *    P is the corresponding public key
     *    Z is the context data (signer public key or certificate, etc)
     *
     * signing:
     *
     *    m = hash(Z, message)
     *    x = hash(m, s)
     *    keygen25519(Y, NULL, x);
     *    r = hash(Y);
     *    h = m XOR r
     *    sign25519(v, h, x, s);
     *
     *    output (v,r) as the signature
     *
     * verification:
     *
     *    m = hash(Z, message);
     *    h = m XOR r
     *    verify25519(Y, v, h, P)
     *
     *    confirm  r === hash(Y)
     *
     * It would seem to me that it would be simpler to have the signer directly do
     * h = hash(m, Y) and send that to the recipient instead of r, who can verify
     * the signature by checking h === hash(m, Y).  If there are any problems with
     * such a scheme, please let me know.
     *
     * Also, EC-KCDSA (like most DS algorithms) picks x random, which is a waste of
     * perfectly good entropy, but does allow Y to be calculated in advance of (or
     * parallel to) hashing the message.
     */

    /* Signature generation primitive, calculates (x-h)s mod q
     *   h  [in]  signature hash (of message, signature pub key, and context data)
     *   x  [in]  signature private key
     *   s  [in]  private key for signing
     * returns signature value on success, undefined on failure (use different x or h)
     */

    function sign (h, x, s) {
        // v = (x - h) s  mod q
        var w, i;
        var h1 = new Array(32)
        var x1 = new Array(32);
        var tmp1 = new Array(64);
        var tmp2 = new Array(64);

        // Don't clobber the arguments, be nice!
        cpy32(h1, h);
        cpy32(x1, x);

        // Reduce modulo group order
        var tmp3 = new Array(32);
        divmod(tmp3, h1, 32, ORDER, 32);
        divmod(tmp3, x1, 32, ORDER, 32);

        // v = x1 - h1
        // If v is negative, add the group order to it to become positive.
        // If v was already positive we don't have to worry about overflow
        // when adding the order because v < ORDER and 2*ORDER < 2^256
        var v = new Array(32);
        mula_small(v, x1, 0, h1, 32, -1);
        mula_small(v, v , 0, ORDER, 32, 1);

        // tmp1 = (x-h)*s mod q
        mula32(tmp1, v, s, 32, 1);
        divmod(tmp2, tmp1, 64, ORDER, 32);

        for (w = 0, i = 0; i < 32; i++)
            w |= v[i] = tmp1[i];

        return w !== 0 ? v : undefined;
    }

    /* Signature verification primitive, calculates Y = vP + hG
     *   v  [in]  signature value
     *   h  [in]  signature hash
     *   P  [in]  public key
     *   Returns signature public key
     */
    function verify (v, h, P) {
        /* Y = v abs(P) + h G  */
        var d = new Array(32);
        var p = [createUnpackedArray(), createUnpackedArray()];
        var s = [createUnpackedArray(), createUnpackedArray()];
        var yx = [createUnpackedArray(), createUnpackedArray(), createUnpackedArray()];
        var yz = [createUnpackedArray(), createUnpackedArray(), createUnpackedArray()];
        var t1 = [createUnpackedArray(), createUnpackedArray(), createUnpackedArray()];
        var t2 = [createUnpackedArray(), createUnpackedArray(), createUnpackedArray()];

        var vi = 0, hi = 0, di = 0, nvh = 0, i, j, k;

        /* set p[0] to G and p[1] to P  */

        set(p[0], 9);
        unpack(p[1], P);

        /* set s[0] to P+G and s[1] to P-G  */

        /* s[0] = (Py^2 + Gy^2 - 2 Py Gy)/(Px - Gx)^2 - Px - Gx - 486662  */
        /* s[1] = (Py^2 + Gy^2 + 2 Py Gy)/(Px - Gx)^2 - Px - Gx - 486662  */

        x_to_y2(t1[0], t2[0], p[1]); /* t2[0] = Py^2  */
        sqrt(t1[0], t2[0]); /* t1[0] = Py or -Py  */
        j = is_negative(t1[0]); /*      ... check which  */
        add(t2[0], t2[0], C39420360); /* t2[0] = Py^2 + Gy^2  */
        mul(t2[1], BASE_2Y, t1[0]); /* t2[1] = 2 Py Gy or -2 Py Gy  */
        sub(t1[j], t2[0], t2[1]); /* t1[0] = Py^2 + Gy^2 - 2 Py Gy  */
        add(t1[1 - j], t2[0], t2[1]); /* t1[1] = Py^2 + Gy^2 + 2 Py Gy  */
        cpy(t2[0], p[1]); /* t2[0] = Px  */
        sub(t2[0], t2[0], C9); /* t2[0] = Px - Gx  */
        sqr(t2[1], t2[0]); /* t2[1] = (Px - Gx)^2  */
        recip(t2[0], t2[1], 0); /* t2[0] = 1/(Px - Gx)^2  */
        mul(s[0], t1[0], t2[0]); /* s[0] = t1[0]/(Px - Gx)^2  */
        sub(s[0], s[0], p[1]); /* s[0] = t1[0]/(Px - Gx)^2 - Px  */
        sub(s[0], s[0], C486671); /* s[0] = X(P+G)  */
        mul(s[1], t1[1], t2[0]); /* s[1] = t1[1]/(Px - Gx)^2  */
        sub(s[1], s[1], p[1]); /* s[1] = t1[1]/(Px - Gx)^2 - Px  */
        sub(s[1], s[1], C486671); /* s[1] = X(P-G)  */
        mul_small(s[0], s[0], 1); /* reduce s[0] */
        mul_small(s[1], s[1], 1); /* reduce s[1] */

        /* prepare the chain  */
        for (i = 0; i < 32; i++) {
            vi = (vi >> 8) ^ (v[i] & 0xFF) ^ ((v[i] & 0xFF) << 1);
            hi = (hi >> 8) ^ (h[i] & 0xFF) ^ ((h[i] & 0xFF) << 1);
            nvh = ~(vi ^ hi);
            di = (nvh & (di & 0x80) >> 7) ^ vi;
            di ^= nvh & (di & 0x01) << 1;
            di ^= nvh & (di & 0x02) << 1;
            di ^= nvh & (di & 0x04) << 1;
            di ^= nvh & (di & 0x08) << 1;
            di ^= nvh & (di & 0x10) << 1;
            di ^= nvh & (di & 0x20) << 1;
            di ^= nvh & (di & 0x40) << 1;
            d[i] = di & 0xFF;
        }

        di = ((nvh & (di & 0x80) << 1) ^ vi) >> 8;

        /* initialize state */
        set(yx[0], 1);
        cpy(yx[1], p[di]);
        cpy(yx[2], s[0]);
        set(yz[0], 0);
        set(yz[1], 1);
        set(yz[2], 1);

        /* y[0] is (even)P + (even)G
         * y[1] is (even)P + (odd)G  if current d-bit is 0
         * y[1] is (odd)P + (even)G  if current d-bit is 1
         * y[2] is (odd)P + (odd)G
         */

        vi = 0;
        hi = 0;

        /* and go for it! */
        for (i = 32; i-- !== 0;) {
            vi = (vi << 8) | (v[i] & 0xFF);
            hi = (hi << 8) | (h[i] & 0xFF);
            di = (di << 8) | (d[i] & 0xFF);

            for (j = 8; j-- !== 0;) {
                mont_prep(t1[0], t2[0], yx[0], yz[0]);
                mont_prep(t1[1], t2[1], yx[1], yz[1]);
                mont_prep(t1[2], t2[2], yx[2], yz[2]);

                k = ((vi ^ vi >> 1) >> j & 1)
                    + ((hi ^ hi >> 1) >> j & 1);
                mont_dbl(yx[2], yz[2], t1[k], t2[k], yx[0], yz[0]);

                k = (di >> j & 2) ^ ((di >> j & 1) << 1);
                mont_add(t1[1], t2[1], t1[k], t2[k], yx[1], yz[1],
                    p[di >> j & 1]);

                mont_add(t1[2], t2[2], t1[0], t2[0], yx[2], yz[2],
                    s[((vi ^ hi) >> j & 2) >> 1]);
            }
        }

        k = (vi & 1) + (hi & 1);
        recip(t1[0], yz[k], 0);
        mul(t1[1], yx[k], t1[0]);

        var Y = [];
        pack(t1[1], Y);
        return Y;
    }

    /* Key-pair generation
     *   P  [out] your public key
     *   s  [out] your private key for signing
     *   k  [out] your private key for key agreement
     *   k  [in]  32 random bytes
     * s may be NULL if you don't care
     *
     * WARNING: if s is not NULL, this function has data-dependent timing */
    function keygen (k) {
        var P = [];
        var s = [];
        k = k || [];
        clamp(k);
        core(P, s, k, null);

        return { p: P, s: s, k: k };
    }

    return {
        sign: sign,
        verify: verify,
        keygen: keygen,
        mulJava: mulJava,
        addJava : addJava,
        subJava : subJava,
        sqrJava: sqrJava,
        recipJava : recipJava,
        packJava : packJava,
        is_overflow_java : is_overflow_java,
        cpyJava : cpyJava,
        setJava : setJava,
        mul_smallJava : mul_smallJava,
        coreJava : coreJava,
        unpackJava : unpackJava

    };
}();

module.exports = curve25519;


var long10 = function(arr) {
    if (arr && arr.length) {

        return {
            _0 : bigInt(arr[0]),
            _1 : bigInt(arr[1]),
            _2 : bigInt(arr[2]),
            _3 : bigInt(arr[3]),
            _4 : bigInt(arr[4]),
            _5 : bigInt(arr[5]),
            _6 : bigInt(arr[6]),
            _7 : bigInt(arr[7]),
            _8 : bigInt(arr[8]),
            _9 : bigInt(arr[9])

        }
    } else {

        return {
            _0 : bigInt(0),
            _1 : bigInt(0),
            _2 : bigInt(0),
            _3 : bigInt(0),
            _4 : bigInt(0),
            _5 : bigInt(0),
            _6 : bigInt(0),
            _7 : bigInt(0),
            _8 : bigInt(0),
            _9 : bigInt(0)

        }
    }
};

// Tests


// Ready!!
// QUnit.test("addJava", function (assert) {
//     var x  = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]);
//     var y  = long10([12000, 13000, 14000, 15000, 16000,17000, 18000, 19000, 20000, 21000]);
//     var xy = long10();
//
//     var temp = curve25519.addJava(xy, x, y);
//
//     xy = Object.values(xy);
//     xy = xy.map(function(i) {
//
//         return i['value'];
//     });
//
//     console.log('addJava :[' + xy.toString() + ']');
//
//     assert.deepEqual(xy, [14000, 16000, 18000, 20000, 22000, 24000, 26000,28000, 30000, 32000]);
// });
//
// // Ready!!
// QUnit.test("subJava", function (assert) {
//     var x  = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]);
//     var y  = long10([12000, 13000, 14000, 15000, 16000,17000, 18000, 19000, 20000, 21000]);
//     var xy = long10();
//
//     var temp = curve25519.subJava(xy, x, y);
//
//     xy = Object.values(xy);
//     xy = xy.map(function(i) {
//         return i['value'];
//     });
//
//     console.log('subJava :[' + xy.toString() + ']');
//
//     assert.deepEqual(xy, [-10000, -10000, -10000, -10000, -10000, -10000, -10000, -10000, -10000, -10000 ]);
// });
//
// // Development
// QUnit.test("mulJava", function (assert) {
//     var x  = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]);
//     var y  = long10([12000, 13000, 14000, 15000, 16000,17000, 18000, 19000, 20000, 21000]);
//     var xy = long10();
//
//     var temp = curve25519.mulJava(xy, x, y);
//
//     xy = Object.values(xy);
//     xy = xy.map(function(i) {
//         return i['value'];
//     });
//
//
//     console.log('mulJava :[' + xy.toString() + ']');
//
//
//     assert.deepEqual(xy, [48773799, 20865339, 11427004, 27705909, 58870161, 30257923, 42212139, 18739108, 13888390, 16921620]);
// });
//
// // Development
// QUnit.test("sqrJava", function (assert) {
//     var x  = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]);
//     var y  = long10([12000, 13000, 14000, 15000, 16000,17000, 18000, 19000, 20000, 21000]);
//     var xy = long10();
//
//     var temp = curve25519.sqrJava(x, y);
//
//     x = Object.values(x);
//     console.log(x);
//     x = x.map(function(i) {
//         return i['value'];
//     });
//
//     console.log('sqrJava :[' + x.toString() + ']');
//
//
//     assert.deepEqual(x, [23182666, 27320415, 33593052, 15097837, 55426326, 16784192, 6900223, 22597206, 7559059, 22754602]);
// });
//
// // Ready!!!
// QUnit.test("recipJava", function (assert) {
//     var x  = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]);
//     var y  = long10([12000, 13000, 14000, 15000, 16000,17000, 18000, 19000, 20000, 21000]);
//     var xy = long10();
//
//     var temp = curve25519.recipJava(xy, x, y);
//
//     xy = Object.values(xy);
//     xy = xy.map(function(i) {
//         return i['value'];
//     });
//
//
//     console.log('recipJava :[' + xy.toString() + ']');
//
//
//     assert.deepEqual(xy, [28752729, 24156288, 50485493, 28444979, 25105293, 5300205, 14135273, 33260211, 43142049, 12499538]);
// });

// Ready!!!
// QUnit.test("packJava", function (assert) {
//     var x  = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]);
//     var y = [] ;
//     var xy = long10();
//
//     var temp = curve25519.packJava(x, y);
//
//     y = Object.values(y);
//     y = y.map(function(i) {
//         return i['value'];
//     });
//
//     y = new Int8Array(y);
//     y = Object.values(y);
//     var testArray = [-48, 7, 0, -32, 46, 0, 0, 125, 0, 0, 113, 2, 0, -36, 5, 0, 88, 27, 0, -128, 62, 0, 64, 25, 1, 0, 113, 2, 0, -66, 10, 0];
//
//     console.log('packJava      :[' + y.toString() + ']');
//     console.log('packJava test :[' + testArray.toString() + ']');
//
//     assert.deepEqual(y, testArray);
// });


// Ready!!!
// QUnit.test("is_overflow_java", function (assert) {
//     var x1  = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, '1100000000000000000']);
//     var x2 = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 110000]);
//
//     var temp1 = curve25519.is_overflow_java(x1);
//     var temp2 = curve25519.is_overflow_java(x2);
//
//
//
//     console.log('is_overflow_java greater :[' + x1.toString() + ']');
//     console.log('is_overflow_java less    :[' + x2.toString() + ']');
//
//
//     assert.deepEqual(temp1, true);
//     assert.deepEqual(temp2, false);
// });

// // Ready!!!
// QUnit.test("cpyJava", function (assert) {
//     var x1  = long10([2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 1100000]);
//     var x2 =  long10();
//
//     var temp1 = curve25519.cpyJava(x2, x1);
//
//     assert.deepEqual(x2, x1);
//     x1._5 = bigInt(0);
//     assert.notDeepEqual(x2, x1);
//
// });

// // Ready!!!
// QUnit.test("setJava", function (assert) {
//     var x  = long10([1111,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]);
//     var y = bigInt(19) ;
//     var testArray = [19,  0,  0,  0,  0, 0,  0,  0,  0, 0];
//
//     var temp = curve25519.setJava(x, y);
//
//     x = Object.values(x);
//     x = x.map(function(i) {
//         return i['value'];
//     });
//
//     assert.deepEqual(x, testArray);
// });

// // Ready!!!
// QUnit.test("mul_smallJava", function (assert) {
//     var x  = long10([-32343,  '30000000000',  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]);
//     var y  = bigInt(100000);
//     var xy = long10();
//     var testArray = [54034944, 5472207, 19644919, 30237959, 63129102, 28911368, 61802516, 27584779, 60475930, 26258190];
//
//
//     var temp = curve25519.mul_smallJava(xy, x, y);
//
//     xy = Object.values(xy);
//     xy = xy.map(function(i) {
//         return i['value'];
//     });
//
//     console.log('xy        :[' + xy.toString() + ']');
//     console.log('testArray :[' + testArray.toString() + ']');
//
//     assert.deepEqual(xy, testArray);
// });

// Ready!!!
QUnit.test("coreJava", function (assert) {
    var k = new Int8Array([-42,72,59,14,-18,102,-76,-20,57,127, 62, -43, -102, -62, -55, 37, -11, 11, -52, 40, 104, 51, -117, 105, 55, -98, -26, 94, 46, -97, 28, 45]);
    var p = new Int8Array([-2, -120, -71, 80, -31, -76, 120, 115, -33, -15,  -34,  -113,  -87,  37,  13,  82,  -54,  -24,  82,  -64,  -15,  93,  94,  -56,  19,  -64,  83,  37,  -120,  -26,  -41,  62]);

    var z = new Int8Array(32);

    var testArray = new Int8Array([-96,78,95,-81,-7,57,-127,54,4,8, 7, -1, -14, -121, -10, 102, 114, 34, 75, -80, 20, -2, 27, -42, -53, 33, 14, -10, 74, -52, 62, 21]);


    curve25519.coreJava(z, null, k, p);


    assert.deepEqual(z, testArray);
});

//
// QUnit.test("unpackJava", function (assert) {
//     var x  = new Int8Array([-48, 7, 0, -32, 46, 0, 0, 125, 0, 0, 113, 2, 0, -36, 5, 0, 88, 27, 0, -128, 62, 0, 64, 25, 1, 0, 113, 2, 0, -66, 10, 0]);
//     var y = [] ;
//     var xy = long10();
//
//     var testArray = [2000,  3000,  4000,  5000,  6000, 7000,  8000,  9000,  10000, 11000]
//
//     curve25519.unpackJava(y, x);
//
//     y = Object.values(y);
//     y = y.map(function(i) {
//         return i['value'];
//     });
//
//     assert.deepEqual(y, testArray);
// });





