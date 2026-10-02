package com.share.rental.item;

import com.share.rental.item.entity.Category;
import com.share.rental.item.entity.Item;
import com.share.rental.item.enums.ItemStatusEnum;
import com.share.rental.item.enums.PriceTypeEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ItemEntityCompileTest {

    @Test
    void enumsExposeExpectedCodes() {
        assertThat(ItemStatusEnum.LISTED.code()).isEqualTo(1);
        assertThat(ItemStatusEnum.FORCE_OFF_SHELF.code()).isEqualTo(3);
        assertThat(PriceTypeEnum.FREE.code()).isEqualTo(0);
        assertThat(PriceTypeEnum.DAILY.code()).isEqualTo(1);
    }

    @Test
    void entitiesCanInstantiate() {
        Item item = new Item();
        item.setTitle("相机");
        item.setPriceType(PriceTypeEnum.DAILY.code());
        item.setDailyPrice(new BigDecimal("50.00"));
        assertThat(item.getTitle()).isEqualTo("相机");

        Category category = new Category();
        category.setName("数码设备");
        assertThat(category.getName()).isEqualTo("数码设备");
    }
}
