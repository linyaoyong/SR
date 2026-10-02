package com.share.rental.auth;

import com.share.rental.auth.entity.CreditScoreRecord;
import com.share.rental.auth.entity.User;
import com.share.rental.auth.entity.UserBlacklist;
import com.share.rental.auth.enums.UserRoleEnum;
import com.share.rental.auth.enums.UserStatusEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthEntityCompileTest {

    @Test
    void enumsExposeExpectedCodes() {
        assertThat(UserRoleEnum.ADMIN.code()).isEqualTo(1);
        assertThat(UserRoleEnum.USER.code()).isEqualTo(0);
        assertThat(UserStatusEnum.NORMAL.code()).isEqualTo(0);
        assertThat(UserStatusEnum.BANNED.code()).isEqualTo(1);
    }

    @Test
    void entitiesCanInstantiate() {
        User user = new User();
        user.setUsername("senjing");
        user.setRole(UserRoleEnum.USER.code());
        assertThat(user.getUsername()).isEqualTo("senjing");

        UserBlacklist blacklist = new UserBlacklist();
        blacklist.setUserId(1L);
        assertThat(blacklist.getUserId()).isEqualTo(1L);

        CreditScoreRecord record = new CreditScoreRecord();
        record.setChangeValue(1);
        assertThat(record.getChangeValue()).isEqualTo(1);
    }
}
