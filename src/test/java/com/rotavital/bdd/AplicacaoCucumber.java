package com.rotavital.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;

@CucumberContextConfiguration
@SpringBootTest(properties = {
        "DATABASE_URL=",
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///rota_vital_bdd",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
@ActiveProfiles("infra")
@AutoConfigureMockMvc
@MockBean(Clock.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AplicacaoCucumber {
}
