declare namespace Api {
  namespace Mq {
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

    interface Message {
      id: string;
      messageKey: string;
      topic: string;
      tag: string | null;
      bizType: string;
      bizKey: string;
      deliverStatus: number;
      retryCount: number;
      nextRetryTime: string;
      lastError: string | null;
      sentTime: string | null;
      createTime: string;
      bizStatus?: number | null;
      bizStatusText?: string | null;
      bizStatusDetail?: string | null;
    }

    interface MessageQuery extends PageQuery {
      bizType?: string;
      messageKey?: string;
      deliverStatus?: number;
    }
  }
}
