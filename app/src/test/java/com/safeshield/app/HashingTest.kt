package com.safeshield.app

import com.safeshield.app.core.Hashing
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class HashingTest {

    @Test
    fun `known vector matches`() {
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            Hashing.sha256("hello"),
        )
    }

    @Test
    fun `empty string vector matches`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Hashing.sha256(""),
        )
    }

    @Test
    fun `file and string hashes agree`() {
        val file = File.createTempFile("safeshield", ".bin")
        file.writeText("hello")
        assertEquals(Hashing.sha256("hello"), Hashing.sha256(file))
        file.delete()
    }

    @Test
    fun `missing file returns null instead of throwing`() {
        assertEquals(null, Hashing.sha256(File("/does/not/exist.apk")))
    }
}
