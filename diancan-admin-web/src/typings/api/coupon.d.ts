declare namespace Api {
  namespace Coupon {
    interface PageQuery {
      pageNum?: number;
      pageSize?: number;
    }

    interface PageResult<T> {
      list: T[];
      pageNum: number;
      pageSize: number;
      total: number;
      pages?: number;
    }

    interface CouponTemplate {
      id: string;
      name: string;
      type: number;
      thresholdAmount: number;
      discountAmount: number;
      discountRate: number | null;
      totalQuantity: number;
      issuedQuantity: number;
      perUserLimit: number;
      validityType: number;
      validFrom: string | null;
      validTo: string | null;
      validDays: number | null;
      status: number;
      description: string | null;
      availableWeekdays: string | null;
      createTime: string;
    }

    interface CouponTemplateQuery extends PageQuery {
      name?: string;
      status?: number;
      type?: number;
    }

    interface CouponTemplateSubmit {
      name: string;
      type: number;
      thresholdAmount?: number;
      discountAmount?: number;
      discountRate?: number | null;
      totalQuantity: number;
      perUserLimit: number;
      validityType: number;
      validFrom?: string | null;
      validTo?: string | null;
      validDays?: number | null;
      status: number;
      description?: string;
      availableWeekdays?: string | null;
    }

    interface CouponTemplateUpdate extends CouponTemplateSubmit {
      id: string;
    }

    interface CouponGrantSubmit {
      templateId: string;
      grantMode: number;
      userIds?: string[];
      remark?: string;
    }

    interface CouponGrantTask {
      id: string;
      templateId: string;
      templateName: string;
      grantMode: number;
      taskStatus: number;
      targetCount: number;
      successCount: number;
      failCount: number;
      remark: string | null;
      startedTime: string | null;
      lastError: string | null;
      totalBatchCount: number;
      finishedBatchCount: number;
      finishedTime: string | null;
    }

    interface CouponGrantTaskQuery extends PageQuery {
      templateName?: string;
      taskStatus?: number;
    }

    interface CouponGrantTaskDetail {
      id: string;
      taskId: string;
      userId: string;
      username: string;
      phone: string | null;
      grantStatus: number;
      failReason: string | null;
      finishedTime: string | null;
      createTime: string;
    }

    interface CouponGrantTaskDetailQuery extends PageQuery {
      taskId: string;
      grantStatus?: number;
      keyword?: string;
    }

    interface UserCoupon {
      id: string;
      templateId: string;
      userId: string;
      username: string;
      nickname: string | null;
      phone: string | null;
      couponName: string;
      couponType: number;
      thresholdAmount: number;
      discountAmount: number;
      discountRate: number | null;
      sourceType: number;
      status: number;
      receivedTime: string;
      validFrom: string;
      validTo: string;
      usedTime: string | null;
      orderId: string | null;
      availableWeekdays: string | null;
    }

    interface UserCouponQuery extends PageQuery {
      templateId?: string;
      userId?: string;
      status?: number;
      keyword?: string;
    }
  }
}
