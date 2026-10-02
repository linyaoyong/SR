package com.share.rental.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.redis.RedisKey;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.client.AuthRentalClient;
import com.share.rental.rental.client.ItemRentalClient;
import com.share.rental.rental.client.MessageRentalClient;
import com.share.rental.rental.client.WalletRentalClient;
import com.share.rental.rental.dto.CardMessageRequest;
import com.share.rental.rental.dto.ItemInfoFeignResponse;
import com.share.rental.rental.dto.RentalApplicationCreateRequest;
import com.share.rental.rental.dto.RentalApplicationDetailResponse;
import com.share.rental.rental.dto.RentalApplicationResponse;
import com.share.rental.rental.dto.RentalProposalResponse;
import com.share.rental.rental.dto.RentalProposalUpdateRequest;
import com.share.rental.rental.dto.UserStatusFeignResponse;
import com.share.rental.rental.dto.WalletUsableResponse;
import com.share.rental.rental.entity.RentalApplication;
import com.share.rental.rental.entity.RentalProposal;
import com.share.rental.rental.enums.ApplicationStatusEnum;
import com.share.rental.rental.mapper.RentalApplicationMapper;
import com.share.rental.rental.mapper.RentalProposalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RentalApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RentalApplicationService.class);

    private static final int ITEM_STATUS_LISTED = 1;
    private static final int ITEM_AUDIT_RECTIFY = 2;
    private static final int USER_STATUS_NORMAL = 0;

    private final RentalApplicationMapper applicationMapper;
    private final RentalProposalMapper proposalMapper;
    private final ItemRentalClient itemClient;
    private final AuthRentalClient authClient;
    private final WalletRentalClient walletClient;
    private final RentalOrderService orderService;
    private final StringRedisTemplate redisTemplate;
    private final MessageRentalClient messageRentalClient;

    @Autowired
    public RentalApplicationService(RentalApplicationMapper applicationMapper,
                                    RentalProposalMapper proposalMapper,
                                    ItemRentalClient itemClient,
                                    AuthRentalClient authClient,
                                    WalletRentalClient walletClient,
                                    RentalOrderService orderService,
                                    StringRedisTemplate redisTemplate,
                                    MessageRentalClient messageRentalClient) {
        this.applicationMapper = applicationMapper;
        this.proposalMapper = proposalMapper;
        this.itemClient = itemClient;
        this.authClient = authClient;
        this.walletClient = walletClient;
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
        this.messageRentalClient = messageRentalClient;
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalApplicationDetailResponse createApplication(Long renterId, RentalApplicationCreateRequest request) {
        ItemInfoFeignResponse item = requireItem(request.getItemId());
        if (item.getOwnerId().equals(renterId)) {
            throw new BusinessException(ErrorCode.RENTAL_OWNER_CANNOT_RENT_OWN_ITEM);
        }
        if (item.getStatus() == null || item.getStatus() != ITEM_STATUS_LISTED
                || item.getAuditStatus() == null || item.getAuditStatus() == ITEM_AUDIT_RECTIFY) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        UserStatusFeignResponse renter = requireUserStatus(renterId);
        if (renter.getStatus() == null || renter.getStatus() != USER_STATUS_NORMAL) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        WalletUsableResponse wallet = requireWalletUsable(renterId);
        if (wallet.getUsable() == null || !wallet.getUsable()) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        if (request.getRentStartTime() == null || request.getRentEndTime() == null
                || !request.getRentStartTime().isBefore(request.getRentEndTime())) {
            throw new BusinessException(ErrorCode.RENTAL_TIME_INVALID);
        }
        acquireDuplicateGuard(renterId, request.getItemId());

        RentalApplication application = new RentalApplication();
        application.setItemId(item.getId());
        application.setOwnerId(item.getOwnerId());
        application.setRenterId(renterId);
        application.setStatus(ApplicationStatusEnum.NEGOTIATING.code());
        application.setOwnerConfirmed(0);
        application.setRenterConfirmed(1);
        application.setPrepaidRentAmount(BigDecimal.ZERO);
        application.setPreFrozenDepositAmount(BigDecimal.ZERO);
        applicationMapper.insert(application);

        RentalProposal proposal = new RentalProposal();
        proposal.setApplicationId(application.getId());
        proposal.setVersionNo(1);
        proposal.setOperatorId(renterId);
        proposal.setQuantity(request.getQuantity());
        proposal.setDeliveryType(request.getDeliveryType());
        proposal.setRentStartTime(request.getRentStartTime());
        proposal.setRentEndTime(request.getRentEndTime());
        proposal.setMeetupTime(request.getMeetupTime());
        proposal.setMeetupLocation(request.getMeetupLocation());
        proposal.setReceiverName(request.getReceiverName());
        proposal.setReceiverPhone(request.getReceiverPhone());
        proposal.setReceiverAddress(request.getReceiverAddress());
        proposal.setRentAmount(request.getRentAmount() == null
                ? calculateDefaultRentAmount(item, request)
                : request.getRentAmount());
        proposal.setDepositAmount(request.getDepositAmount() == null
                ? defaultDepositAmount(item)
                : request.getDepositAmount());
        proposal.setChangedFields("");
        proposal.setRemark(request.getRemark());
        proposalMapper.insert(proposal);

        application.setCurrentProposalId(proposal.getId());
        applicationMapper.updateById(application);

        // 给物主发申请卡片消息，失败不影响申请创建
        try {
            CardMessageRequest card = new CardMessageRequest(
                    renterId,
                    item.getOwnerId(),
                    item.getId(),
                    "收到新的租借申请：" + item.getTitle(),
                    1,
                    application.getId(),
                    null
            );
            messageRentalClient.createCardMessage(card);
        } catch (Exception e) {
            log.warn("发送租借申请卡片消息失败 applicationId={}: {}", application.getId(), e.getMessage());
        }

        return toDetailResponse(application, proposal, item.getTitle());
    }

    public List<RentalApplicationResponse> listApplications(Long userId) {
        LambdaQueryWrapper<RentalApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalApplication::getOwnerId, userId)
                .or()
                .eq(RentalApplication::getRenterId, userId)
                .orderByDesc(RentalApplication::getCreateTime);
        List<RentalApplication> applications = applicationMapper.selectList(wrapper);
        return applications.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RentalApplicationDetailResponse getApplication(Long applicationId, Long userId) {
        RentalApplication application = loadApplication(applicationId);
        requireParticipant(application, userId);
        RentalProposal currentProposal = null;
        if (application.getCurrentProposalId() != null) {
            currentProposal = proposalMapper.selectById(application.getCurrentProposalId());
        }
        return toDetailResponse(application, currentProposal);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalApplicationDetailResponse updateProposal(Long applicationId, Long operatorId, RentalProposalUpdateRequest request) {
        RentalApplication application = loadApplication(applicationId);
        if (!isParticipant(application, operatorId)) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        }
        if (application.getStatus() == null || application.getStatus() != ApplicationStatusEnum.NEGOTIATING.code()) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_STATUS_INVALID);
        }
        RentalProposal current = loadProposal(application.getCurrentProposalId());
        int nextVersion = nextProposalVersion(applicationId);

        RentalProposal proposal = cloneProposal(current);
        proposal.setApplicationId(applicationId);
        proposal.setVersionNo(nextVersion);
        proposal.setOperatorId(operatorId);
        String changedFields = applyUpdate(proposal, request);
        proposal.setChangedFields(changedFields);
        if (request.getRentStartTime() != null || request.getRentEndTime() != null) {
            LocalDateTime start = proposal.getRentStartTime();
            LocalDateTime end = proposal.getRentEndTime();
            if (start == null || end == null || !start.isBefore(end)) {
                throw new BusinessException(ErrorCode.RENTAL_TIME_INVALID);
            }
        }
        proposalMapper.insert(proposal);

        application.setCurrentProposalId(proposal.getId());
        if (operatorId.equals(application.getOwnerId())) {
            application.setOwnerConfirmed(1);
            application.setRenterConfirmed(0);
        } else {
            application.setRenterConfirmed(1);
            application.setOwnerConfirmed(0);
        }
        applicationMapper.updateById(application);

        return toDetailResponse(application, proposal, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalApplicationDetailResponse confirm(Long applicationId, Long userId) {
        RentalApplication application = loadApplication(applicationId);
        if (!isParticipant(application, userId)) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        }
        if (application.getStatus() == null || application.getStatus() != ApplicationStatusEnum.NEGOTIATING.code()) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_STATUS_INVALID);
        }
        if (userId.equals(application.getOwnerId())) {
            application.setOwnerConfirmed(1);
        }
        if (userId.equals(application.getRenterId())) {
            application.setRenterConfirmed(1);
        }
        if (application.getOwnerConfirmed() == 1 && application.getRenterConfirmed() == 1) {
            RentalProposal proposal = loadProposal(application.getCurrentProposalId());
            orderService.createOrder(application, proposal);
        } else {
            applicationMapper.updateById(application);
        }
        RentalProposal current = loadProposal(application.getCurrentProposalId());
        return toDetailResponse(application, current, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long applicationId, Long userId) {
        RentalApplication application = loadApplication(applicationId);
        requireParticipant(application, userId);
        if (application.getStatus() == null
                || application.getStatus() != ApplicationStatusEnum.NEGOTIATING.code()) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_STATUS_INVALID);
        }
        application.setStatus(ApplicationStatusEnum.CANCELLED.code());
        applicationMapper.updateById(application);
    }

    private ItemInfoFeignResponse requireItem(Long itemId) {
        ApiResponse<ItemInfoFeignResponse> resp = ResilientRemoteCallService.callItem(
                () -> itemClient.getItemInfo(itemId));
        if (resp == null || resp.data() == null) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        return resp.data();
    }

    private void acquireDuplicateGuard(Long renterId, Long itemId) {
        String key = RedisKey.APPLICATION_DUPLICATE + renterId + ":" + itemId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.REQUEST_TOO_FREQUENT);
        }
    }

    private RentalApplication loadApplication(Long applicationId) {
        RentalApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private RentalProposal loadProposal(Long proposalId) {
        RentalProposal proposal = proposalMapper.selectById(proposalId);
        if (proposal == null) {
            throw new BusinessException(ErrorCode.RENTAL_PROPOSAL_NOT_FOUND);
        }
        return proposal;
    }

    private boolean isParticipant(RentalApplication application, Long userId) {
        return userId.equals(application.getOwnerId()) || userId.equals(application.getRenterId());
    }

    private void requireParticipant(RentalApplication application, Long userId) {
        if (!isParticipant(application, userId)) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        }
    }

    private int nextProposalVersion(Long applicationId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RentalProposal> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RentalProposal>()
                        .eq(RentalProposal::getApplicationId, applicationId);
        List<RentalProposal> proposals = proposalMapper.selectList(wrapper);
        return proposals.stream()
                .mapToInt(RentalProposal::getVersionNo)
                .max()
                .orElse(0) + 1;
    }

    private BigDecimal calculateDefaultRentAmount(ItemInfoFeignResponse item, RentalApplicationCreateRequest request) {
        BigDecimal dailyPrice = item.getDailyPrice() == null ? BigDecimal.ZERO : item.getDailyPrice();
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        long minutes = Duration.between(request.getRentStartTime(), request.getRentEndTime()).toMinutes();
        long billingDays = Math.max(1, (minutes + 1439) / 1440);
        int minRentDays = item.getMinRentDays() == null || item.getMinRentDays() < 1 ? 1 : item.getMinRentDays();
        long chargedDays = Math.max(billingDays, minRentDays);
        return dailyPrice
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(chargedDays));
    }

    private BigDecimal defaultDepositAmount(ItemInfoFeignResponse item) {
        return item.getDepositAmount() == null ? BigDecimal.ZERO : item.getDepositAmount();
    }

    private RentalProposal cloneProposal(RentalProposal source) {
        RentalProposal target = new RentalProposal();
        target.setQuantity(source.getQuantity());
        target.setDeliveryType(source.getDeliveryType());
        target.setRentStartTime(source.getRentStartTime());
        target.setRentEndTime(source.getRentEndTime());
        target.setMeetupTime(source.getMeetupTime());
        target.setMeetupLocation(source.getMeetupLocation());
        target.setReceiverName(source.getReceiverName());
        target.setReceiverPhone(source.getReceiverPhone());
        target.setReceiverAddress(source.getReceiverAddress());
        target.setRentAmount(source.getRentAmount());
        target.setDepositAmount(source.getDepositAmount());
        target.setRemark(source.getRemark());
        return target;
    }

    private String applyUpdate(RentalProposal proposal, RentalProposalUpdateRequest request) {
        StringBuilder changed = new StringBuilder();
        if (request.getQuantity() != null) {
            proposal.setQuantity(request.getQuantity());
            appendField(changed, "quantity");
        }
        if (request.getRentStartTime() != null) {
            proposal.setRentStartTime(request.getRentStartTime());
            appendField(changed, "rentStartTime");
        }
        if (request.getRentEndTime() != null) {
            proposal.setRentEndTime(request.getRentEndTime());
            appendField(changed, "rentEndTime");
        }
        if (request.getMeetupTime() != null) {
            proposal.setMeetupTime(request.getMeetupTime());
            appendField(changed, "meetupTime");
        }
        if (request.getMeetupLocation() != null) {
            proposal.setMeetupLocation(request.getMeetupLocation());
            appendField(changed, "meetupLocation");
        }
        if (request.getReceiverName() != null) {
            proposal.setReceiverName(request.getReceiverName());
            appendField(changed, "receiverName");
        }
        if (request.getReceiverPhone() != null) {
            proposal.setReceiverPhone(request.getReceiverPhone());
            appendField(changed, "receiverPhone");
        }
        if (request.getReceiverAddress() != null) {
            proposal.setReceiverAddress(request.getReceiverAddress());
            appendField(changed, "receiverAddress");
        }
        if (request.getRentAmount() != null) {
            proposal.setRentAmount(request.getRentAmount());
            appendField(changed, "rentAmount");
        }
        if (request.getDepositAmount() != null) {
            proposal.setDepositAmount(request.getDepositAmount());
            appendField(changed, "depositAmount");
        }
        if (request.getRemark() != null) {
            proposal.setRemark(request.getRemark());
            appendField(changed, "remark");
        }
        return changed.toString();
    }

    private void appendField(StringBuilder builder, String field) {
        if (builder.length() > 0) {
            builder.append(",");
        }
        builder.append(field);
    }

    private UserStatusFeignResponse requireUserStatus(Long userId) {
        ApiResponse<UserStatusFeignResponse> resp = authClient.getUserStatus(userId);
        if (resp == null || resp.data() == null) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        return resp.data();
    }

    private WalletUsableResponse requireWalletUsable(Long userId) {
        ApiResponse<WalletUsableResponse> resp = ResilientRemoteCallService.callWallet(
                () -> walletClient.isUsable(userId));
        if (resp == null || resp.data() == null) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        return resp.data();
    }

    private RentalApplicationResponse toResponse(RentalApplication application) {
        RentalApplicationResponse resp = new RentalApplicationResponse();
        resp.setId(application.getId());
        resp.setItemId(application.getItemId());
        resp.setRenterId(application.getRenterId());
        resp.setOwnerId(application.getOwnerId());
        resp.setStatus(application.getStatus());
        resp.setCurrentProposalId(application.getCurrentProposalId());
        resp.setOwnerConfirmed(application.getOwnerConfirmed());
        resp.setRenterConfirmed(application.getRenterConfirmed());
        resp.setCreateTime(application.getCreateTime());
        return resp;
    }

    private RentalApplicationDetailResponse toDetailResponse(RentalApplication application, RentalProposal proposal) {
        return toDetailResponse(application, proposal, null);
    }

    private RentalApplicationDetailResponse toDetailResponse(RentalApplication application,
                                                             RentalProposal proposal,
                                                             String itemTitle) {
        RentalApplicationDetailResponse resp = new RentalApplicationDetailResponse();
        resp.setId(application.getId());
        resp.setItemId(application.getItemId());
        resp.setRenterId(application.getRenterId());
        resp.setOwnerId(application.getOwnerId());
        resp.setStatus(application.getStatus());
        resp.setCurrentProposalId(application.getCurrentProposalId());
        resp.setOwnerConfirmed(application.getOwnerConfirmed());
        resp.setRenterConfirmed(application.getRenterConfirmed());
        resp.setCreateTime(application.getCreateTime());
        resp.setItemTitle(itemTitle);
        resp.setCurrentProposal(proposal == null ? null : toProposalResponse(proposal));
        return resp;
    }

    private RentalProposalResponse toProposalResponse(RentalProposal proposal) {
        RentalProposalResponse resp = new RentalProposalResponse();
        resp.setId(proposal.getId());
        resp.setApplicationId(proposal.getApplicationId());
        resp.setVersionNo(proposal.getVersionNo());
        resp.setOperatorId(proposal.getOperatorId());
        resp.setQuantity(proposal.getQuantity());
        resp.setDeliveryType(proposal.getDeliveryType());
        resp.setRentStartTime(proposal.getRentStartTime());
        resp.setRentEndTime(proposal.getRentEndTime());
        resp.setMeetupTime(proposal.getMeetupTime());
        resp.setMeetupLocation(proposal.getMeetupLocation());
        resp.setReceiverName(proposal.getReceiverName());
        resp.setReceiverPhone(proposal.getReceiverPhone());
        resp.setReceiverAddress(proposal.getReceiverAddress());
        resp.setRentAmount(proposal.getRentAmount());
        resp.setDepositAmount(proposal.getDepositAmount());
        resp.setChangedFields(proposal.getChangedFields());
        resp.setRemark(proposal.getRemark());
        resp.setCreateTime(proposal.getCreateTime());
        return resp;
    }
}
