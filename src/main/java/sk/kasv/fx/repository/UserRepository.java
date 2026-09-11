package sk.kasv.fx.repository;

import sk.kasv.fx.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}