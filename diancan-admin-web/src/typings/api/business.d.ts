/** 业务模块 API 类型定义 */
declare namespace Api {
  namespace Business {
    type IdType = string | number;
    // ==================== 菜品分类 ====================
    interface DishCategory {
      id: IdType;
      name: string;
      sort: number;
      status: number;
      image?: string | null;
      specGroupIds: IdType[];
      specGroupNames: string[];
      createTime: string;
    }

    interface DishCategoryCreate {
      name: string;
      sort?: number;
      status?: number;
      image?: string;
      specGroupIds?: IdType[];
    }

    interface DishCategoryUpdate extends DishCategoryCreate {
      id: IdType;
    }

    interface DishCategorySortItem {
      id: IdType;
      sort: number;
    }

    // ==================== 规格组 ====================
    interface DishSpecOption {
      id?: IdType;
      name: string;
      sort?: number;
    }

    interface DishSpecGroup {
      id: IdType;
      name: string;
      sort: number;
      status: number;
      createTime: string;
      options: DishSpecOption[];
    }

    interface DishSpecGroupCreate {
      name: string;
      sort?: number;
      status?: number;
      options: DishSpecOption[];
    }

    interface DishSpecGroupUpdate extends DishSpecGroupCreate {
      id: IdType;
    }

    interface DishSpecItem {
      specGroupId: IdType;
      specGroupName: string;
      optionIds: IdType[];
      optionNames: string[];
    }

    // ==================== 菜品 ====================
    interface Dish {
      id: IdType;
      categoryId: IdType;
      categoryName: string;
      name: string;
      price: number;
      image: string | null;
      thumbnail: string | null;
      spiceLevel: number;
      ingredients: string | null;
      description: string | null;
      status: number;
      soldOut: number;
      stock: number;
      preparationTime: number | null;
      createTime: string;
      specItems: DishSpecItem[];
    }

    interface DishQuery {
      categoryId?: IdType;
      name?: string;
      status?: number;
    }

    interface DishCreate {
      categoryId: IdType;
      name: string;
      price: number;
      image?: string;
      thumbnail?: string;
      spiceLevel?: number;
      ingredients?: string;
      description?: string;
      stock?: number;
      preparationTime?: number;
      specItems?: DishSpecItem[];
    }

    interface DishUpdate {
      id: IdType;
      categoryId?: IdType;
      name?: string;
      price?: number;
      image?: string;
      thumbnail?: string;
      spiceLevel?: number;
      ingredients?: string;
      description?: string;
      stock?: number;
      preparationTime?: number;
      specItems?: DishSpecItem[];
    }

    interface FileUploadResult {
      url: string;
      objectName: string;
    }

    // ==================== 桌台 ====================
    interface DiningTable {
      id: number;
      code: string;
      name: string;
      capacity: number;
      status: number;
      qrCodeUrl: string | null;
      areaId: number | null;
      areaName: string | null;
      currentSessionCode?: string | null;
      createTime: string;
    }

    interface TableArea {
      id: number;
      name: string;
      sort: number;
      status: number;
      remark: string | null;
      createTime: string;
    }

    interface TableAreaCreate {
      name: string;
      sort?: number;
      status?: number;
      remark?: string;
    }

    interface TableAreaUpdate extends TableAreaCreate {
      id: number;
    }

    interface TableCreate {
      code: string;
      name: string;
      capacity?: number;
      areaId?: number | null;
    }

    interface TableUpdate {
      id: number;
      code?: string;
      name?: string;
      capacity?: number;
      areaId?: number | null;
    }

    interface QrCodeTask {
      taskId: string;
      taskType: string;
      status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
      message: string;
      total: number;
      completed: number;
      downloadable: boolean;
      fileName?: string | null;
      filePath?: string | null;
      createTime?: string | null;
      finishTime?: string | null;
    }

    // ==================== 订单 ====================
    interface OrderItem {
      id: IdType;
      orderId: IdType;
      dishId: IdType;
      dishName: string;
      dishImage: string | null;
      price: number;
      quantity: number;
      amount: number;
      remark: string | null;
      status: number;
      paymentStatus: number;
      isGift: number;
      addedAt: string;
    }

    interface Order {
      id: IdType;
      orderNo: string;
      tableId: IdType;
      tableCode: string;
      tableSessionCode?: string | null;
      areaName?: string | null;
      originalAmount: number;
      discountRate: number;
      actualAmount: number;
      paidAmount: number;
      status: number;
      paymentMode: number;
      paymentMethod?: number | null;
      orderType: number;
      remark: string | null;
      customerOpenid: string | null;
      createTime: string;
      items: OrderItem[];
    }

    interface OrderQuery {
      startDate?: string;
      endDate?: string;
      status?: number;
      tableId?: number;
      areaName?: string;
      orderNo?: string;
    }

    interface OrderOperationLog {
      id: IdType;
      orderId: IdType;
      orderItemId: IdType | null;
      operationType: string;
      operatorId: IdType;
      operatorName: string;
      reason: string | null;
      detail: string | null;
      createTime: string;
    }

    interface OrderDetail extends Order {
      operationLogs: OrderOperationLog[];
    }

    interface OrderRefundRequest {
      reason: string;
    }

    // ==================== 报表 ====================
    interface Revenue {
      date: string;
      totalRevenue: number;
      orderCount: number;
    }

    interface DishRanking {
      dishId: number;
      dishName: string;
      totalQuantity: number;
      totalAmount: number;
    }

    interface TableTurnover {
      date: string;
      totalOrders: number;
      totalTables: number;
      turnoverRate: number;
    }

    interface DashboardTableStats {
      total: number;
      free: number;
      occupied: number;
      settled: number;
      cleaning: number;
    }

    interface DashboardAlert {
      title: string;
      detail: string;
      tone: 'danger' | 'warning' | 'neutral';
      actionLabel: string;
      actionTo: string;
    }

    interface DashboardOverview {
      todayRevenue: number;
      yesterdayRevenue: number;
      todayOrderCount: number;
      yesterdayOrderCount: number;
      averageTicket: number;
      occupancyRate: number;
      todayTableTurnover: number;
      yesterdayTableTurnover: number;
      tableStats: DashboardTableStats;
      revenueTrend: Revenue[];
      dishRanking: DishRanking[];
      alerts: DashboardAlert[];
      sessionMetrics: DashboardSessionMetric[];
    }

    interface DashboardSessionMetric {
      label: string;
      startTime: string;
      endTime: string;
      revenue: number;
      orderCount: number;
      averageTicket: number;
    }

    // ==================== 打印机 ====================
    interface Printer {
      id: number;
      name: string;
      sn: string;
      type: number;
      status: number;
      location: string | null;
      categoryIds: number[];
      createTime: string;
    }

    interface PrinterCreate {
      name: string;
      sn: string;
      type: number;
      location?: string;
    }

    interface PrinterUpdate {
      id: number;
      name?: string;
      sn?: string;
      type?: number;
      status?: number;
      location?: string;
    }

    interface CategoryMappingItem {
      printerId: number;
      categoryId: number;
    }

    // ==================== 审计日志 ====================
    interface AuditLog {
      id: number;
      orderId: number;
      orderItemId: number | null;
      operationType: string;
      operatorId: number;
      operatorName: string;
      reason: string | null;
      detail: string | null;
      createTime: string;
    }

    interface AuditLogQuery {
      startDate?: string;
      endDate?: string;
      operatorName?: string;
      operationType?: string;
    }

    interface AuditLogExportTask {
      id: number;
      taskStatus: number;
      startDate?: string | null;
      endDate?: string | null;
      operatorName?: string | null;
      operationType?: string | null;
      totalCount: number;
      exportedCount: number;
      fileName?: string | null;
      filePath?: string | null;
      lastError?: string | null;
      startedTime?: string | null;
      finishedTime?: string | null;
      createTime: string;
    }

    interface AuditLogExportTaskQuery extends PageQuery {
      taskStatus?: number;
    }

    // ==================== 服务端操作 ====================
    /** 管理端下单项 */
    interface AdminOrderItem {
      dishId: IdType;
      quantity: number;
      remark?: string;
    }

    /** 管理端下单请求 */
    interface AdminOrderCreate {
      tableId: number;
      tableCode?: string;
      clientOrderNo?: string;
      items: AdminOrderItem[];
      paymentMode: number;
      userId?: IdType;
      couponId?: IdType;
      usePoints?: number;
      orderType?: number;
      remark?: string;
      preOrder?: boolean;
    }

    interface AdminOrderEstimateItem {
      dishId: IdType;
      quantity: number;
    }

    interface AdminOrderEstimate {
      tableId?: number;
      userId?: IdType;
      couponId?: IdType;
      usePoints?: number;
      items: AdminOrderEstimateItem[];
    }

    interface AdminOrderEstimateResultItem {
      dishId: IdType;
      dishName: string;
      unitPrice: number;
      quantity: number;
      amount: number;
    }

    interface AdminOrderEstimateResult {
      originalAmount: number;
      memberDiscountAmount: number;
      couponDiscountAmount: number;
      pointsDiscountAmount: number;
      discountAmount: number;
      payableAmount: number;
      couponId?: IdType | null;
      couponName?: string | null;
      requestedPoints: number;
      actualUsedPoints: number;
      availablePoints: number;
      maxUsablePoints: number;
      items: AdminOrderEstimateResultItem[];
      tips: string[];
    }

    /** 加菜请求 */
    interface AddItemRequest {
      dishId: IdType;
      quantity: number;
      remark?: string;
    }

    /** 整单打折请求 */
    interface DiscountRequest {
      discountRate: number;
      reason?: string;
    }

    /** 退菜请求 */
    interface ReturnItemRequest {
      authPassword: string;
      reason: string;
    }

    /** 换菜请求 */
    interface ReplaceItemRequest {
      newDishId: number;
      quantity: number;
      remark?: string;
      authPassword: string;
      reason: string;
    }

    /** 现金支付请求 */
    interface CashPayRequest {
      orderId: IdType;
      receivedAmount: number;
    }

    /** 现金支付结果 */
    interface CashPayResult {
      id: IdType;
      orderId: IdType;
      paymentNo: string;
      paymentMethod: number;
      amount: number;
      status: number;
      createTime: string;
      changeAmount: number;
    }

    /** 支付结果 */
    interface PaymentResult {
      id: IdType;
      orderId: IdType;
      paymentNo: string;
      thirdPartyNo: string | null;
      appId?: string | null;
      timeStamp?: string | null;
      nonceStr?: string | null;
      packageValue?: string | null;
      signType?: string | null;
      paySign?: string | null;
      payUrl?: string | null;
      qrCodeUrl?: string | null;
      paymentMethod: number;
      amount: number;
      status: number;
      createTime: string;
    }

    /** 支付状态 */
    interface PaymentStatusResult {
      paymentNo: string;
      status: number;
      paidAmount: number;
      remainingAmount: number;
    }

    /** 分单结账项 */
    interface SplitBillItem {
      orderItemIds: IdType[];
      paymentMethod: number;
    }

    /** 分单结账请求 */
    interface SplitBillRequest {
      orderId: IdType;
      items: SplitBillItem[];
    }

    interface PaymentRecord {
      id: IdType;
      orderId: IdType;
      orderNo: string;
      tableCode: string;
      areaName?: string | null;
      paymentNo: string;
      thirdPartyNo: string | null;
      paymentMethod: number;
      amount: number;
      status: number;
      payerOpenid: string | null;
      payerName: string | null;
      createTime: string;
    }

    interface PaymentRecordQuery {
      orderId?: string;
      paymentNo?: string;
      paymentMethod?: number;
      status?: number;
      areaName?: string;
      startDate?: string;
      endDate?: string;
    }

    interface ReviewRecord {
      id: IdType;
      orderId: IdType;
      orderNo: string;
      tableCode: string;
      overallRating: number;
      content: string | null;
      customerOpenid: string | null;
      createTime: string;
    }

    interface ReviewItemDetail {
      id: IdType;
      orderItemId: IdType;
      rating: number;
    }

    interface ReviewDetail {
      id: IdType;
      orderId: IdType;
      overallRating: number;
      content: string | null;
      customerOpenid: string | null;
      createTime: string;
      itemReviews: ReviewItemDetail[];
    }

    interface ReviewQuery {
      orderId?: string;
      overallRating?: string;
      customerOpenid?: string;
      startDate?: string;
      endDate?: string;
    }

    interface FeedbackRecord {
      id: IdType;
      content: string;
      contactPhone: string | null;
      customerPhone: string | null;
      customerNickname: string | null;
      customerOpenid: string | null;
      status: number;
      replyContent: string | null;
      replyTime: string | null;
      createTime: string;
    }

    interface FeedbackQuery {
      status?: number;
      keyword?: string;
      contactPhone?: string;
      startDate?: string;
      endDate?: string;
    }

    interface FeedbackReply {
      replyContent: string;
    }
  }
}
