package com.safeshield.app

import com.safeshield.app.core.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    @Test fun `bytes under one kilobyte`() = assertEquals("512 B", Formatters.bytes(512))

    @Test fun `bytes in megabytes`() {
        assertTrue(Formatters.bytes(5L * 1024 * 1024).startsWith("5.0 M"))
    }

    @Test fun `duration seconds only`() = assertEquals("8s", Formatters.duration(8_400))

    @Test fun `duration with minutes`() = assertEquals("1m 20s", Formatters.duration(80_000))
}
