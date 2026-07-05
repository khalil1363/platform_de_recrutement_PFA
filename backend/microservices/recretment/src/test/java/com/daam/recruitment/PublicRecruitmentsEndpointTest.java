package com.daam.recruitment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicRecruitmentsEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPublishedRecruitments() throws Exception {
        mockMvc.perform(get("/api/recruitment/public/recruitments"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
