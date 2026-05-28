package com.interviewassistant.repository;

import com.interviewassistant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 数据访问层。
 *
 * JpaRepository 已经内置了常用方法：save、findById、findAll、deleteById...
 * 我们只需要定义"内置方法没有"的查询。
 *
 * findByUsername 方法：
 * JPA 会解析方法名，自动生成 SQL：
 * SELECT * FROM users WHERE username = ?
 * 不需要写任何实现代码，这就是 JPA 的"方法命名查询"。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
