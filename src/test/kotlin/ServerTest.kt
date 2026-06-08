package com.lalkalol

import com.lalkalol.testsupport.withTestServer
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun `test root endpoint`() = withTestServer {
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

    @Test
    fun `unknown path serves spa shell`() = withTestServer {
        val response = client.get("/no-such-page")
        assertEquals(HttpStatusCode.OK, response.status)
    }
    @Test
    fun `health endpoints respond ok`() = withTestServer {
        assertEquals(HttpStatusCode.OK, client.get("/health").status)
        assertEquals(HttpStatusCode.OK, client.get("/health/ready").status)
    }
}
