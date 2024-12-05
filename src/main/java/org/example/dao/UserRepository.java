package org.example.dao;

import org.example.po.UserPo;
import org.springframework.data.repository.ListCrudRepository;

public interface UserRepository extends ListCrudRepository<UserPo, Long> {
}
