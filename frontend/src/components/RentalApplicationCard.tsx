import { useEffect, useState } from 'react';
import { Button, Popconfirm, Tag, message } from 'antd';
import { Link } from 'react-router-dom';
import ProposalEditor from './ProposalEditor';
import { rentalService } from '../services/rental';
import { applicationStatusText, deliveryTypeText } from '../types/status';
import type { ItemDetail, RentalApplication, UserPublicProfile } from '../types/api';

interface RentalApplicationCardProps {
  application: RentalApplication;
  currentUserId?: number;
  item?: ItemDetail;
  counterparty?: UserPublicProfile;
  counterpartyLabel?: string;
  onChanged: () => void;
}

// 租借申请卡片：展示申请状态、当前 proposal、双方确认标记，并提供修改/确认/取消操作。
// 字段对齐 docs/03-api-contract.md 第 9.1 节。
export default function RentalApplicationCard({
  application,
  currentUserId,
  item,
  counterparty,
  counterpartyLabel,
  onChanged,
}: RentalApplicationCardProps) {
  const [editorOpen, setEditorOpen] = useState(false);
  const [actioning, setActioning] = useState<string | null>(null);
  const [orderId, setOrderId] = useState<number | null>(null);
  const [resolvedApplication, setResolvedApplication] = useState(application);

  useEffect(() => {
    setResolvedApplication(application);
  }, [application]);

  useEffect(() => {
    if (application.currentProposal || !application.currentProposalId) return;
    let active = true;
    rentalService
      .applicationDetail(application.id)
      .then((detail) => {
        if (active) setResolvedApplication(detail);
      })
      .catch(() => {
        if (active) setResolvedApplication(application);
      });
    return () => {
      active = false;
    };
  }, [application]);

  const proposal = resolvedApplication.currentProposal;
  const resolvedItemTitle = item?.title ?? resolvedApplication.itemTitle;
  const resolvedItemImage = item?.images?.[0]?.url;
  const isRenter = currentUserId === resolvedApplication.renterId;
  const isOwner = currentUserId === resolvedApplication.ownerId;
  const isParticipant = isRenter || isOwner;

  // 我的确认标记：根据当前用户身份取 ownerConfirmed 或 renterConfirmed。
  const myConfirmed = isOwner
    ? resolvedApplication.ownerConfirmed === 1
    : isRenter
      ? resolvedApplication.renterConfirmed === 1
      : false;

  const canEdit = resolvedApplication.status === 0 && isParticipant && !!proposal;
  const canConfirm = resolvedApplication.status === 0 && isParticipant && !myConfirmed && !!proposal;
  const canCancel = resolvedApplication.status === 0 && isParticipant;
  const isConverted = resolvedApplication.status === 2;

  // 申请状态变为 CONVERTED(2) 后，订单已创建。RentalApplicationResponse 不含 orderId，
  // 故通过 GET /api/orders 拉取并按 applicationId 匹配。
  useEffect(() => {
    if (!isConverted) {
      setOrderId(null);
      return;
    }
    let active = true;
    (async () => {
      try {
        const orders = await rentalService.orders();
        if (!active) return;
        const matched = orders.find((o) => o.applicationId === resolvedApplication.id);
        setOrderId(matched ? matched.id : null);
      } catch {
        if (active) setOrderId(null);
      }
    })();
    return () => {
      active = false;
    };
  }, [isConverted, resolvedApplication.id]);

  const handleConfirm = async () => {
    setActioning('confirm');
    try {
      const updated = await rentalService.confirmProposal(resolvedApplication.id);
      setResolvedApplication(updated);
      message.success('已确认协商方案');
      onChanged();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioning(null);
    }
  };

  const handleCancel = async () => {
    setActioning('cancel');
    try {
      await rentalService.cancelApplication(resolvedApplication.id);
      message.success('已取消申请');
      onChanged();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioning(null);
    }
  };

  return (
    <section className="sr-card sr-application-card" aria-label="租借申请卡片">
      <div className="sr-application-card-header">
        <h3 className="sr-application-card-title">
          {resolvedItemTitle ? `申请：${resolvedItemTitle}` : `申请 #${resolvedApplication.id}`}
        </h3>
        <Tag color="blue">{applicationStatusText[resolvedApplication.status]}</Tag>
      </div>
      <div className="sr-application-summary">
        {resolvedItemImage ? (
          <img src={resolvedItemImage} alt={resolvedItemTitle ?? '申请物品图片'} />
        ) : (
          <div className="sr-application-summary-placeholder">暂无图片</div>
        )}
        <div>
          <strong>{resolvedItemTitle ?? `物品 #${resolvedApplication.itemId}`}</strong>
          {counterparty && (
            <div className="sr-user-inline">
              {counterparty.avatarUrl ? (
                <img src={counterparty.avatarUrl} alt={`${counterparty.username}头像`} />
              ) : (
                <span>{counterparty.username.slice(0, 1)}</span>
              )}
              <span>{counterparty.username}</span>
              {counterpartyLabel && <span>{counterpartyLabel}</span>}
              <span>信用分 {counterparty.creditScore}</span>
            </div>
          )}
        </div>
      </div>
      <div className="sr-application-card-badges">
        <Tag color={resolvedApplication.ownerConfirmed === 1 ? 'success' : 'default'}>
          物主{resolvedApplication.ownerConfirmed === 1 ? '已确认' : '未确认'}
        </Tag>
        <Tag color={resolvedApplication.renterConfirmed === 1 ? 'success' : 'default'}>
          租借者{resolvedApplication.renterConfirmed === 1 ? '已确认' : '未确认'}
        </Tag>
        {proposal?.versionNo != null && (
          <span className="sr-application-card-version">
            版本 v{proposal.versionNo}
          </span>
        )}
      </div>
      {proposal ? (
        <dl className="sr-application-card-grid">
          <div>
            <dt>数量</dt>
            <dd>{proposal.quantity}</dd>
          </div>
          <div>
            <dt>交付方式</dt>
            <dd>{deliveryTypeText[proposal.deliveryType]}</dd>
          </div>
          <div>
            <dt>租期</dt>
            <dd>
              {proposal.rentStartTime} ~ {proposal.rentEndTime}
            </dd>
          </div>
          <div>
            <dt>租金</dt>
            <dd>¥{proposal.rentAmount}</dd>
          </div>
          <div>
            <dt>押金</dt>
            <dd>¥{proposal.depositAmount}</dd>
          </div>
          {proposal.deliveryType === 0 && (
            <>
              <div>
                <dt>面交时间</dt>
                <dd>{proposal.meetupTime ?? '-'}</dd>
              </div>
              <div>
                <dt>面交地点</dt>
                <dd>{proposal.meetupLocation ?? '-'}</dd>
              </div>
            </>
          )}
          {proposal.deliveryType === 1 && (
            <>
              <div>
                <dt>收件人</dt>
                <dd>{proposal.receiverName ?? '-'}</dd>
              </div>
              <div>
                <dt>收件电话</dt>
                <dd>{proposal.receiverPhone ?? '-'}</dd>
              </div>
              <div>
                <dt>收件地址</dt>
                <dd>{proposal.receiverAddress ?? '-'}</dd>
              </div>
            </>
          )}
          {proposal.remark && (
            <div>
              <dt>备注</dt>
              <dd>{proposal.remark}</dd>
            </div>
          )}
        </dl>
      ) : (
        <div className="sr-application-card-grid sr-text">协商方案暂不可用</div>
      )}

      <div className="sr-actions sr-application-card-actions">
        {canEdit && (
          <Button onClick={() => setEditorOpen(true)}>修改协商</Button>
        )}
        {canConfirm && (
          <Button type="primary" loading={actioning === 'confirm'} onClick={handleConfirm}>
            确认协商
          </Button>
        )}
        {canCancel && (
          <Popconfirm
            title="确认取消该申请？"
            onConfirm={handleCancel}
            okText="取消申请"
            cancelText="再想想"
            okButtonProps={{ danger: true }}
          >
            <Button danger loading={actioning === 'cancel'}>
              取消申请
            </Button>
          </Popconfirm>
        )}
        {isConverted && orderId && (
          <Link to={`/orders/${orderId}`} className="sr-btn sr-btn-md sr-btn-primary">
            查看订单
          </Link>
        )}
        {isConverted && !orderId && (
          <span className="sr-text">订单已创建，正在加载订单链接…</span>
        )}
      </div>

      {proposal && (
          <ProposalEditor
            application={resolvedApplication}
            open={editorOpen}
            onClose={() => setEditorOpen(false)}
          onSaved={onChanged}
        />
      )}
    </section>
  );
}
