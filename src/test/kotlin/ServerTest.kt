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
    fun `unknown path redirects to home`() = withTestServer {
        val response = createClient { followRedirects = false }.get("/no-such-page")
        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("/", response.headers["Location"])
    }
}
