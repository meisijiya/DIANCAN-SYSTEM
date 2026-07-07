declare namespace Api {
  /**
   * namespace Auth
   *
   * backend api module: "auth"
   */
  namespace Auth {
    interface LoginToken {
      userId: number;
      username: string;
      nickname: string;
      avatar: string | null;
      token: string;
    }

    interface UserInfo {
      userId: number;
      username: string;
      nickname: string;
      email: string | null;
      phone: string | null;
      avatar: string | null;
      roles: string[];
      permissions: string[];
    }
  }
}
