package com.share.rental.rental.service;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ResilientRemoteCallService {

    public static final String ITEM_RESOURCE = "rental:item-access";
    public static final String WALLET_RESOURCE = "rental:wallet-access";

    public static <T> T callItem(Supplier<T> supplier) {
        return guarded(ITEM_RESOURCE, supplier);
    }

    public static <T> T callWallet(Supplier<T> supplier) {
        return guarded(WALLET_RESOURCE, supplier);
    }

    private static <T> T guarded(String resource, Supplier<T> supplier) {
        Entry entry = null;
        try {
            entry = SphU.entry(resource);
            return supplier.get();
        } catch (BlockException | RuntimeException ex) {
            throw new BusinessException(ErrorCode.SERVICE_BUSY);
        } finally {
            if (entry != null) {
                entry.close();
            }
        }
    }
}
