package com.gameboost.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gameboost.backend.models.User;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class UserSerializationTest {
    @Test
    void passwordHashIsNeverSerializedEvenInNestedResponses() throws Exception {
        User user = User.builder().email("player@example.test")
                .password("private-password-hash").role(User.Role.JOUEUR).build();
        ObjectMapper mapper = new ObjectMapper();
        String direct = mapper.writeValueAsString(user);
        String nested = mapper.writeValueAsString(Map.of("sender", user));
        assertThat(direct).contains("player@example.test").doesNotContain("password", "private-password-hash");
        assertThat(nested).contains("sender").doesNotContain("password", "private-password-hash");
        assertThat(user.getPassword()).isEqualTo("private-password-hash");
    }
}
