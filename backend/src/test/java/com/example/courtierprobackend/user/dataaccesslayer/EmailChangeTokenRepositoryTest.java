package com.example.courtierprobackend.user.dataaccesslayer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

@DataJpaTest
class EmailChangeTokenRepositoryTest {
    @Autowired
    private EmailChangeTokenRepository repository;

}
