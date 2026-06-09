package com.lalkalol

import com.lalkalol.testsupport.SpringIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ServerTest : SpringIntegrationTest() {
    @Test
    fun `test root endpoint`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
    }

    @Test
    fun `unknown path serves spa shell`() {
        mockMvc.perform(get("/no-such-page"))
            .andExpect(status().isOk)
    }

    @Test
    fun `static assets are served under static prefix`() {
        mockMvc.perform(get("/static/assets/app-MZ0QUVSk.js"))
            .andExpect(status().isOk)
    }

    @Test
    fun `health endpoints respond ok`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/health/ready"))
            .andExpect(status().isOk)
    }
}
