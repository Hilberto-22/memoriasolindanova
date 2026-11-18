package br.org.memorias;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/** Uses the configured PostgreSQL database without modifying application tables. */
class PostgreSqlTestSchema implements ApplicationContextInitializer<GenericApplicationContext> {
    @Override
    public void initialize(GenericApplicationContext context) {
        var environment = context.getEnvironment();
        String url = environment.getRequiredProperty("spring.datasource.url");
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("Os testes precisam de uma conexão PostgreSQL em DATABASE_URL.");
        }

        var schema = new TestSchema(url,
                environment.getRequiredProperty("spring.datasource.username"),
                environment.getRequiredProperty("spring.datasource.password"),
                "memorias_test_" + UUID.randomUUID().toString().replace("-", ""));
        schema.create();

        environment.getPropertySources().addFirst(new MapPropertySource("isolatedPostgreSqlSchema", Map.of(
                "spring.datasource.hikari.schema", schema.name,
                "spring.flyway.default-schema", schema.name,
                "spring.flyway.schemas", schema.name)));
        context.registerBean("testPostgreSqlSchema", TestSchema.class, () -> schema,
                definition -> definition.setDestroyMethodName("close"));
    }

    static final class TestSchema implements AutoCloseable {
        private final String url;
        private final String username;
        private final String password;
        private final String name;

        TestSchema(String url, String username, String password, String name) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.name = name;
        }

        void create() {
            execute("create schema " + name);
        }

        @Override
        public void close() {
            // name is generated above; only this test's schema is removed.
            execute("drop schema " + name + " cascade");
        }

        private void execute(String sql) {
            try (var connection = DriverManager.getConnection(url, username, password);
                 var statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException failure) {
                throw new IllegalStateException(
                        "Não foi possível preparar ou remover o schema de teste no PostgreSQL. "
                                + "Confira a conexão do .env e a permissão CREATE no banco.", failure);
            }
        }
    }
}
