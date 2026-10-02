package com.share.rental.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void childInheritsCommonFields() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setCreateTime(java.time.LocalDateTime.now());

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getCreateTime()).isNotNull();
    }

    static class TestEntity extends BaseEntity {
    }
}
