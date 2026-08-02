package com.igarciamen.tasks;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TasksApplicationTests {

    @Test
    void contextLoads() {
        // Arranca contra H2 en memoria (src/test/resources/application.properties),
        // no depende de tener Postgres levantado.
    }

}
