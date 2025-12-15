package com.kakarote.admin.oceanengine.service;

/**
 * 定时刷新 OceanEngine token 的服务
 */
public interface OceanTokenRefreshService {

    /**
     * 按默认阈值（例如 60 分钟）刷新即将过期的 token
     */
    RefreshResult refreshExpiringTokens();

    /**
     * 按指定阈值刷新即将过期的 token
     */
    RefreshResult refreshExpiringTokens(int thresholdMinutes);

    class RefreshResult {
        private int checked;
        private int refreshed;
        private int skipped;
        private int failed;

        public int getChecked() {
            return checked;
        }

        public void setChecked(int checked) {
            this.checked = checked;
        }

        public int getRefreshed() {
            return refreshed;
        }

        public void setRefreshed(int refreshed) {
            this.refreshed = refreshed;
        }

        public int getSkipped() {
            return skipped;
        }

        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }
    }
}
