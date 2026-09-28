package sk.kasv.fx.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import sk.kasv.fx.entity.Role;
import sk.kasv.fx.entity.User;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=update")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryPostgresTest {
    @Autowired
    private UserRepository users;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndLoadsUserFromPostgres() {
        String email = "db-check-" + UUID.randomUUID() + "@example.invalid";
        String passwordHash = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());
        User saved = users.saveAndFlush(new User(email, passwordHash, Role.CUSTOMER));
        Long id = saved.getId();
        assertThat(id).isNotNull();

        // Force a SELECT from PostgreSQL instead of returning the cached entity.
        entityManager.clear();
        User loaded = users.findById(id).orElseThrow();
        assertThat(loaded.getEmail()).isEqualTo(email);
        assertThat(loaded.getPassword()).isEqualTo(passwordHash);
        assertThat(loaded.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(loaded.getCreatedAt()).isNotNull();
        // DataJpaTest rolls back the inserted user after the test.
    }
}
