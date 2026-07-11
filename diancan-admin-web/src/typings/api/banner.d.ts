declare namespace Api {
  namespace Banner {
    interface HomeBanner {
      id: number;
      title: string;
      subtitle: string | null;
      imageUrl: string;
      actionType: number;
      targetPath: string | null;
      scene: string;
      sort: number;
      status: number;
      createTime: string;
    }

    interface HomeBannerQuery {
      title?: string;
      status?: number;
      scene?: string;
      pageNum?: number;
      pageSize?: number;
    }

    interface HomeBannerSubmit {
      title: string;
      subtitle?: string;
      imageUrl: string;
      actionType: number;
      targetPath?: string;
      scene: string;
      sort: number;
      status: number;
    }
  }
}
