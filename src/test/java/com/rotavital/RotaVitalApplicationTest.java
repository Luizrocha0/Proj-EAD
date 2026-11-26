package com.rotavital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "RENDER_GIT_COMMIT=revisao-do-teste")
@AutoConfigureMockMvc
class RotaVitalApplicationTest {

    @Test
    void informaARevisaoPublicadaPeloRender(@Autowired MockMvc api) throws Exception {
        api.perform(get("/actuator/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.app.commit").value("revisao-do-teste"));
    }
}
