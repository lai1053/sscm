package com.kakarote.admin.oceanengine.service.impl;

import com.kakarote.admin.oceanengine.mapper.QcOeAdvertiserMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeAdsAdvertiserDailyMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeQcAdvertiserDailyMapper;
import com.kakarote.admin.oceanengine.mapper.QcOeSaleUserMapper;
import com.kakarote.admin.oceanengine.model.AccountSourceSummaryVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardAdvertiserVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardCompanyVO;
import com.kakarote.admin.oceanengine.model.SalesDashboardSaleVO;
import com.kakarote.admin.oceanengine.model.SalesOverviewVO;
import com.kakarote.admin.oceanengine.model.CustomerTrendPointVO;
import com.kakarote.admin.oceanengine.service.OeIdentityService;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OeSalesDashboardServiceImplTest {

    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:sales_dashboard;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_qc_oe_advertiser");
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_qc_customer_owner");
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_qc_oe_sale_user");
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_admin_user");
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_qc_oe_ads_advertiser_daily");
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_qc_oe_qc_advertiser_daily");
            conn.createStatement().execute("CREATE TABLE wk_qc_oe_advertiser (\n" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                    "advertiser_id BIGINT,\n" +
                    "advertiser_name VARCHAR(128),\n" +
                    "channel VARCHAR(32),\n" +
                    "account_source VARCHAR(32),\n" +
                    "sale_user_id BIGINT,\n" +
                    "adv_company_id BIGINT,\n" +
                    "adv_company_name VARCHAR(128),\n" +
                    "owner_id BIGINT,\n" +
                    "last_cost_date DATE,\n" +
                    "is_deleted INT DEFAULT 0,\n" +
                    "last_sync_time TIMESTAMP\n" +
                    ")");
            conn.createStatement().execute("CREATE TABLE wk_qc_customer_owner (\n" +
                    "id BIGINT PRIMARY KEY,\n" +
                    "owner_name VARCHAR(128),\n" +
                    "mobile VARCHAR(32),\n" +
                    "wechat VARCHAR(64),\n" +
                    "level VARCHAR(32),\n" +
                    "tags VARCHAR(256),\n" +
                    "remark VARCHAR(256),\n" +
                    "is_deleted INT DEFAULT 0,\n" +
                    "create_time TIMESTAMP,\n" +
                    "update_time TIMESTAMP\n" +
                    ")");
            conn.createStatement().execute("CREATE TABLE wk_qc_oe_sale_user (\n" +
                    "id BIGINT PRIMARY KEY,\n" +
                    "sale_id BIGINT,\n" +
                    "sale_name VARCHAR(64),\n" +
                    "login_user_id BIGINT,\n" +
                    "is_deleted INT DEFAULT 0\n" +
                    ")");
            conn.createStatement().execute("CREATE TABLE wk_admin_user (\n" +
                    "user_id BIGINT PRIMARY KEY,\n" +
                    "username VARCHAR(64),\n" +
                    "realname VARCHAR(64)\n" +
                    ")");
            conn.createStatement().execute("CREATE TABLE wk_qc_oe_ads_advertiser_daily (\n" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                    "advertiser_id BIGINT,\n" +
                    "stat_date DATE,\n" +
                    "channel VARCHAR(20),\n" +
                    "stat_cost DECIMAL(18,2),\n" +
                    "show_cnt BIGINT,\n" +
                    "click_cnt BIGINT,\n" +
                    "gmt_create TIMESTAMP,\n" +
                    "gmt_modified TIMESTAMP,\n" +
                    "UNIQUE(advertiser_id, stat_date)\n" +
                    ")");
            conn.createStatement().execute("CREATE TABLE wk_qc_oe_qc_advertiser_daily (\n" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                    "advertiser_id BIGINT,\n" +
                    "stat_date DATE,\n" +
                    "channel VARCHAR(20),\n" +
                    "stat_cost DECIMAL(18,2),\n" +
                    "show_cnt BIGINT,\n" +
                    "click_cnt BIGINT,\n" +
                    "pay_order_count BIGINT,\n" +
                    "pay_gmv DECIMAL(18,2),\n" +
                    "roi DECIMAL(18,2),\n" +
                    "gmt_create TIMESTAMP,\n" +
                    "gmt_modified TIMESTAMP,\n" +
                    "UNIQUE(advertiser_id, stat_date)\n" +
                    ")");

            conn.createStatement().execute("INSERT INTO wk_qc_oe_sale_user(id, sale_id, sale_name, login_user_id, is_deleted) VALUES (1, 1001, 'SaleA', 5001, 0)");
            conn.createStatement().execute("INSERT INTO wk_admin_user(user_id, username, realname) VALUES (5001, 'userA', 'SaleReal')");
            conn.createStatement().execute("INSERT INTO wk_qc_customer_owner(id, owner_name, mobile, wechat, level, tags, remark, is_deleted, create_time, update_time) VALUES" +
                    "(301, 'OwnerA', '13800138000', 'wx_owner_a', 'A', 'tag1,tag2', 'remark', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)," +
                    "(302, 'OwnerB', '13800138001', 'wx_owner_b', 'B', 'tag3', 'remarkB', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

            LocalDate statDate = LocalDate.now().minusDays(1);
            LocalDate prevStatDate = statDate.minusDays(7);
            LocalDate statDate2 = statDate.minusDays(2);
            conn.createStatement().execute("INSERT INTO wk_qc_oe_advertiser(advertiser_id, advertiser_name, channel, account_source, sale_user_id, adv_company_id, adv_company_name, owner_id, last_cost_date, is_deleted) VALUES" +
                    "(101, 'Adv101', 'ADS', 'AD', 1, 201, 'CompA', 301, DATEADD('DAY', -1, CURRENT_DATE), 0)," +
                    "(102, 'Adv102', 'ADS', 'LOCAL', 1, 201, 'CompA', 301, DATEADD('DAY', -16, CURRENT_DATE), 0)," +
                    "(103, 'Adv103', 'QIANCHUAN', NULL, 1, 202, 'CompB', 302, DATEADD('DAY', -31, CURRENT_DATE), 0)," +
                    "(104, 'Adv104', 'ADS', 'AD', NULL, 203, 'CompC', NULL, NULL, 0)," +
                    "(105, 'Adv105', 'QIANCHUAN', NULL, NULL, 204, 'CompD', NULL, NULL, 0)");

            conn.createStatement().execute("INSERT INTO wk_qc_oe_ads_advertiser_daily(advertiser_id, stat_date, channel, stat_cost, show_cnt, click_cnt, gmt_create, gmt_modified) VALUES" +
                    "(101, DATE '" + statDate + "', 'ADS', 100.00, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)," +
                    "(102, DATE '" + statDate + "', 'ADS', 50.00, 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)," +
                    "(101, DATE '" + statDate2 + "', 'ADS', 20.00, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)," +
                    "(102, DATE '" + prevStatDate + "', 'ADS', 200.00, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)," +
                    "(104, DATE '" + statDate + "', 'ADS', 25.00, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

            conn.createStatement().execute("INSERT INTO wk_qc_oe_qc_advertiser_daily(advertiser_id, stat_date, channel, stat_cost, show_cnt, click_cnt, pay_order_count, pay_gmv, roi, gmt_create, gmt_modified) VALUES" +
                    "(103, DATE '" + statDate + "', 'QIANCHUAN', 30.00, 4, 1, 1, 60.00, 2.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)," +
                    "(103, DATE '" + prevStatDate + "', 'QIANCHUAN', 60.00, 4, 1, 2, 100.00, 2.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)," +
                    "(105, DATE '" + statDate + "', 'QIANCHUAN', 5.00, 1, 1, 1, 8.00, 1.6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(QcOeAdvertiserMapper.class);
        configuration.addMapper(QcOeSaleUserMapper.class);
        configuration.addMapper(QcOeAdsAdvertiserDailyMapper.class);
        configuration.addMapper(QcOeQcAdvertiserDailyMapper.class);

        parseMapper(configuration, "mapper/oceanengine/QcOeAdvertiserMapper.xml");
        parseMapper(configuration, "mapper/oceanengine/QcOeSaleUserMapper.xml");
        parseMapper(configuration, "mapper/oceanengine/QcOeAdsAdvertiserDailyMapper.xml");
        parseMapper(configuration, "mapper/oceanengine/QcOeQcAdvertiserDailyMapper.xml");

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void salesDashboardShouldAggregateByWindowAndFilters() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            QcOeAdvertiserMapper advertiserMapper = session.getMapper(QcOeAdvertiserMapper.class);
            QcOeSaleUserMapper saleUserMapper = session.getMapper(QcOeSaleUserMapper.class);
            OeSalesDashboardServiceImpl service = new OeSalesDashboardServiceImpl(
                    saleUserMapper,
                    advertiserMapper,
                    Mockito.mock(QcOeAdsAdvertiserDailyMapper.class),
                    Mockito.mock(QcOeQcAdvertiserDailyMapper.class),
                    Mockito.mock(OeIdentityService.class));

            List<SalesDashboardSaleVO> all = service.dashboardSales(7, "ALL", "ALL");
            assertThat(all).hasSize(1);
            SalesDashboardSaleVO row = all.get(0);
            assertThat(row.getAdvertiserCount()).isEqualTo(3);
            assertThat(row.getCompanyCount()).isEqualTo(2);
            assertThat(row.getAdsCost()).isEqualByComparingTo("170.00");
            assertThat(row.getQcCost()).isEqualByComparingTo("30.00");

            List<SalesDashboardSaleVO> adsOnly = service.dashboardSales(7, "ADS", "ALL");
            assertThat(adsOnly.get(0).getAdvertiserCount()).isEqualTo(2);
            assertThat(adsOnly.get(0).getQcCost()).isEqualByComparingTo("0");

            List<SalesDashboardSaleVO> adSource = service.dashboardSales(7, "ADS", "AD");
            assertThat(adSource.get(0).getAdvertiserCount()).isEqualTo(1);
            assertThat(adSource.get(0).getAdsCost()).isEqualByComparingTo("120.00");
        }
    }

    @Test
    void companyAndAdvertiserDashboardShouldRespectFilters() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            QcOeAdvertiserMapper advertiserMapper = session.getMapper(QcOeAdvertiserMapper.class);
            QcOeSaleUserMapper saleUserMapper = session.getMapper(QcOeSaleUserMapper.class);
            OeSalesDashboardServiceImpl service = new OeSalesDashboardServiceImpl(
                    saleUserMapper,
                    advertiserMapper,
                    Mockito.mock(QcOeAdsAdvertiserDailyMapper.class),
                    Mockito.mock(QcOeQcAdvertiserDailyMapper.class),
                    Mockito.mock(OeIdentityService.class));

            List<SalesDashboardCompanyVO> companies = service.dashboardCompanies(1L, 7, "ALL", "ALL");
            assertThat(companies).extracting(SalesDashboardCompanyVO::getAdvCompanyId).containsExactlyInAnyOrder(201L, 202L);

            SalesDashboardCompanyVO company201 = companies.stream().filter(c -> c.getAdvCompanyId() == 201L).findFirst()
                    .orElseThrow(() -> new IllegalStateException("company 201 not found"));
            assertThat(company201.getAdsCost()).isEqualByComparingTo("170.00");
            assertThat(company201.getQcCost()).isEqualByComparingTo("0");
            assertThat(company201.getOwnerId()).isEqualTo(301L);
            assertThat(company201.getOwnerName()).isEqualTo("OwnerA");
            assertThat(company201.getRiskLevel()).isEqualTo("ACTIVE");
            assertThat(company201.getInactiveDays()).isNotNull();
            assertThat(company201.getInactiveDays()).isGreaterThanOrEqualTo(0);
            assertThat(company201.getInactiveDays()).isLessThanOrEqualTo(2);

            SalesDashboardCompanyVO company202 = companies.stream().filter(c -> c.getAdvCompanyId() == 202L).findFirst()
                    .orElseThrow(() -> new IllegalStateException("company 202 not found"));
            assertThat(company202.getRiskLevel()).isEqualTo("DANGER");
            assertThat(company202.getInactiveDays()).isGreaterThanOrEqualTo(30);

            List<SalesDashboardAdvertiserVO> advertisers = service.dashboardAdvertisers(1L, 201L, 7, "ADS", "ALL");
            assertThat(advertisers).hasSize(2);
            Map<Long, SalesDashboardAdvertiserVO> byAdv = advertisers.stream().collect(Collectors.toMap(SalesDashboardAdvertiserVO::getAdvertiserId, a -> a));
            assertThat(byAdv.get(101L).getAdsCost()).isEqualByComparingTo("120.00");
            assertThat(byAdv.get(102L).getAdsCost()).isEqualByComparingTo("50.00");
            assertThat(byAdv.get(101L).getRiskLevel()).isEqualTo("ACTIVE");
            assertThat(byAdv.get(102L).getRiskLevel()).isEqualTo("WARN");
        }
    }

    @Test
    void accountSourceSummaryShouldIncludeQianchuanRow() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            QcOeAdvertiserMapper advertiserMapper = session.getMapper(QcOeAdvertiserMapper.class);
            QcOeSaleUserMapper saleUserMapper = session.getMapper(QcOeSaleUserMapper.class);
            OeSalesDashboardServiceImpl service = new OeSalesDashboardServiceImpl(
                    saleUserMapper,
                    advertiserMapper,
                    Mockito.mock(QcOeAdsAdvertiserDailyMapper.class),
                    Mockito.mock(QcOeQcAdvertiserDailyMapper.class),
                    Mockito.mock(OeIdentityService.class));

            List<AccountSourceSummaryVO> rows = service.listAccountSourceSummary();
            Map<String, AccountSourceSummaryVO> bySource = rows.stream().collect(Collectors.toMap(AccountSourceSummaryVO::getAccountSource, r -> r));

            AccountSourceSummaryVO adRow = bySource.get("AD");
            assertThat(adRow.getAdvertiserCount()).isEqualTo(2);
            assertThat(adRow.getCompanyCount()).isEqualTo(2);
            assertThat(adRow.getUnassignedAdvertiserCount()).isEqualTo(1);
            assertThat(adRow.getUnassignedCompanyCount()).isEqualTo(1);

            AccountSourceSummaryVO localRow = bySource.get("LOCAL");
            assertThat(localRow.getAdvertiserCount()).isEqualTo(1);
            assertThat(localRow.getCompanyCount()).isEqualTo(1);

            AccountSourceSummaryVO qcRow = bySource.get("QIANCHUAN");
            assertThat(qcRow).isNotNull();
            assertThat(qcRow.getAdvertiserCount()).isEqualTo(2);
            assertThat(qcRow.getCompanyCount()).isEqualTo(2);
            assertThat(qcRow.getUnassignedAdvertiserCount()).isEqualTo(1);
            assertThat(qcRow.getUnassignedCompanyCount()).isEqualTo(1);
        }
    }

    @Test
    void overviewAndTrendShouldWork() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            QcOeAdvertiserMapper advertiserMapper = session.getMapper(QcOeAdvertiserMapper.class);
            QcOeSaleUserMapper saleUserMapper = session.getMapper(QcOeSaleUserMapper.class);
            OeSalesDashboardServiceImpl service = new OeSalesDashboardServiceImpl(
                    saleUserMapper,
                    advertiserMapper,
                    Mockito.mock(QcOeAdsAdvertiserDailyMapper.class),
                    Mockito.mock(QcOeQcAdvertiserDailyMapper.class),
                    Mockito.mock(OeIdentityService.class));

            SalesOverviewVO overview = service.overview(7, "ALL", "ALL", 1L);
            assertThat(overview.getWindowDays()).isEqualTo(7);
            assertThat(overview.getWindowStart()).isNotNull();
            assertThat(overview.getWindowEnd()).isNotNull();
            assertThat(overview.getTotalCost()).isEqualByComparingTo(overview.getAdsCost().add(overview.getQcCost()));
            assertThat(overview.getTotalCompanyCount()).isGreaterThan(0);
            assertThat(overview.getTotalAdvertiserCount()).isGreaterThan(0);
            assertThat(overview.getActiveCompanyCount() + overview.getWarnCompanyCount() + overview.getDangerCompanyCount())
                    .isGreaterThanOrEqualTo(overview.getTotalCompanyCount());

            List<CustomerTrendPointVO> trend = service.customerTrend(201L, 30, "ALL", "ALL");
            assertThat(trend).isNotEmpty();
            for (int i = 1; i < trend.size(); i++) {
                assertThat(trend.get(i).getStatDate()).isAfterOrEqualTo(trend.get(i - 1).getStatDate());
                assertThat(trend.get(i - 1).getStatDate()).isBeforeOrEqualTo(trend.get(i).getStatDate());
            }
            trend.forEach(p -> assertThat(p.getTotalCost()).isEqualByComparingTo(p.getAdsCost().add(p.getQcCost())));
        }
    }

    private void parseMapper(Configuration configuration, String resource) throws Exception {
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperBuilder.parse();
        }
    }
}
