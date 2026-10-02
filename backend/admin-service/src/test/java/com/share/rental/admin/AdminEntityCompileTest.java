package com.share.rental.admin;

import com.share.rental.admin.entity.AdminOperationLog;
import com.share.rental.admin.entity.AuditRecord;
import com.share.rental.admin.enums.OperationTypeEnum;
import com.share.rental.admin.enums.TargetTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.main.lazy-initialization=true",
        "spring.cloud.nacos.discovery.enabled=false"
})
class AdminEntityCompileTest {

    @Test
    void operationTypeBanUserCodeIsString() {
        assertEquals("BAN_USER", OperationTypeEnum.BAN_USER.code());
    }

    @Test
    void targetTypeItemCodeIsString() {
        assertEquals("ITEM", TargetTypeEnum.ITEM.code());
    }

    @Test
    void adminOperationLogFieldsAccessible() {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(1L);
        log.setOperationType(OperationTypeEnum.BAN_USER.code());
        log.setTargetType(TargetTypeEnum.USER.code());
        log.setTargetId(100L);
        log.setRemark("test");
        assertEquals(1L, log.getAdminId());
        assertEquals("BAN_USER", log.getOperationType());
    }

    @Test
    void auditRecordFieldsAccessible() {
        AuditRecord record = new AuditRecord();
        record.setAdminId(1L);
        record.setTargetType(TargetTypeEnum.ITEM.code());
        record.setTargetId(200L);
        record.setNewStatus(1);
        assertEquals(1, record.getNewStatus());
    }
}
