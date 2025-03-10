package com.mercata.openemail

import com.goterl.lazysodium.utils.Key
import com.mercata.openemail.utils.encodeToBase64
import com.mercata.openemail.utils.hashedWithSha256
import com.mercata.openemail.utils.signDataBytes
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun hashingSigningTest() {
        val hashSum = "Hello World !!".hashedWithSha256()
        assert(byteArrayOf(50, 38, -97, 31, 22, -44, 78, -128, 115, 70, -111, 86, 33, -26, -70, -72, 29, -7, 76, -22, -103, -103, 27, -9, 34, 114, 85, -82, -27, -30, 51, -20).contentEquals(hashSum.second))
        assert("32269f1f16d44e807346915621e6bab81df94cea99991bf7227255aee5e233ec".equals(hashSum.first, ignoreCase = true))
        val secretKey = Key.fromBytes(byteArrayOf(41, -21, -67, -45, 45, -13, 111, -18, -77, -23, 29, 124, -126, -121, 76, -35, 74, -93, 21, -29, -1, 114, -74, 82, -53, 54, 121, -70, 120, -18, -4, 76, -27, 4, -51, -71, 55, -51, -83, -87, 118, 119, -56, 27, 20, -54, 57, -17, 13, 46, 1, 11, -14, 62, -96, 58, -109, 92, 114, 93, -114, 70, 18, 20))
        val hashed = hashSum.second.signDataBytes(secretKey).encodeToBase64()
        assert("JIlQHlOIbcHowsOMQ8feQ+uGfnmJhKxJnjSZf1B2s0MWn2BHWBvlwg403N/f6h+scVHNahW8juU7oj698abXAQ==" == hashed)
    }
}