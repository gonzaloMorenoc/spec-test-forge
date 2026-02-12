package com.specforge.core.generator.payload;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadGeneratorTest {

    @Test
    void generatesPayloadWithinSchemaConstraints() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "required", List.of("name", "age", "email", "roles"),
                "properties", Map.of(
                        "name", Map.of("type", "string", "minLength", 5, "maxLength", 10),
                        "age", Map.of("type", "integer", "minimum", 18, "maximum", 65),
                        "email", Map.of("type", "string", "format", "email"),
                        "roles", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "items", Map.of("type", "string", "enum", List.of("admin", "viewer"))
                        )
                )
        );

        Object payload = new PayloadGenerator(1234L).generate(schema);
        Map<String, Object> map = assertInstanceOf(Map.class, payload);

        String name = assertInstanceOf(String.class, map.get("name"));
        assertTrue(name.length() >= 5);
        assertTrue(name.length() <= 10);

        Number age = assertInstanceOf(Number.class, map.get("age"));
        assertTrue(age.longValue() >= 18);
        assertTrue(age.longValue() <= 65);

        assertEquals("user@example.com", map.get("email"));

        List<?> roles = assertInstanceOf(List.class, map.get("roles"));
        assertEquals(1, roles.size());
        assertEquals("admin", roles.getFirst());
    }
}
