package com.kakarote.admin.oceanengine.mapper;

import com.kakarote.admin.oceanengine.entity.QcOeAdsAdvertiserDaily;
import com.kakarote.admin.oceanengine.entity.QcOeQcAdvertiserDaily;
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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyUpsertMapperTest {

    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:daily_upsert;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_qc_oe_ads_advertiser_daily");
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
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_qc_oe_qc_advertiser_daily");
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
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(QcOeAdsAdvertiserDailyMapper.class);
        configuration.addMapper(QcOeQcAdvertiserDailyMapper.class);

        parseMapper(configuration, "mapper/oceanengine/QcOeAdsAdvertiserDailyMapper.xml");
        parseMapper(configuration, "mapper/oceanengine/QcOeQcAdvertiserDailyMapper.xml");

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void adsUpsertShouldUpdateOnDuplicate() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            QcOeAdsAdvertiserDailyMapper mapper = session.getMapper(QcOeAdsAdvertiserDailyMapper.class);
            Date statDate = Date.valueOf("2024-01-01");
            QcOeAdsAdvertiserDaily first = new QcOeAdsAdvertiserDaily();
            first.setAdvertiserId(101L);
            first.setStatDate(statDate);
            first.setChannel("ADS");
            first.setStatCost(new java.math.BigDecimal("10.00"));
            first.setShowCnt(1L);
            first.setClickCnt(2L);
            first.setGmtCreate(new java.util.Date());
            first.setGmtModified(new java.util.Date());

            QcOeAdsAdvertiserDaily second = new QcOeAdsAdvertiserDaily();
            second.setAdvertiserId(101L);
            second.setStatDate(statDate);
            second.setChannel("ADS");
            second.setStatCost(new java.math.BigDecimal("20.00"));
            second.setShowCnt(2L);
            second.setClickCnt(3L);
            second.setGmtCreate(new java.util.Date());
            second.setGmtModified(new java.util.Date());

            mapper.upsertBatch(Arrays.asList(first));
            mapper.upsertBatch(Arrays.asList(second));

            try (PreparedStatement ps = session.getConnection().prepareStatement(
                    "SELECT COUNT(*) AS cnt, MAX(stat_cost) AS cost, MAX(show_cnt) AS showCnt FROM wk_qc_oe_ads_advertiser_daily WHERE advertiser_id=? AND stat_date=?")) {
                ps.setLong(1, 101L);
                ps.setDate(2, statDate);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt("cnt")).isEqualTo(1);
                    assertThat(rs.getBigDecimal("cost")).isEqualByComparingTo("20.00");
                    assertThat(rs.getLong("showCnt")).isEqualTo(2L);
                }
            }
        }
    }

    @Test
    void qcUpsertShouldUpdateOnDuplicate() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            QcOeQcAdvertiserDailyMapper mapper = session.getMapper(QcOeQcAdvertiserDailyMapper.class);
            Date statDate = Date.valueOf("2024-02-01");
            QcOeQcAdvertiserDaily first = new QcOeQcAdvertiserDaily();
            first.setAdvertiserId(201L);
            first.setStatDate(statDate);
            first.setChannel("QIANCHUAN");
            first.setStatCost(new java.math.BigDecimal("5.00"));
            first.setShowCnt(1L);
            first.setClickCnt(1L);
            first.setPayOrderCount(1L);
            first.setPayGmv(new java.math.BigDecimal("9.00"));
            first.setRoi(new java.math.BigDecimal("1.8"));
            first.setGmtCreate(new java.util.Date());
            first.setGmtModified(new java.util.Date());

            QcOeQcAdvertiserDaily second = new QcOeQcAdvertiserDaily();
            second.setAdvertiserId(201L);
            second.setStatDate(statDate);
            second.setChannel("QIANCHUAN");
            second.setStatCost(new java.math.BigDecimal("7.00"));
            second.setShowCnt(3L);
            second.setClickCnt(2L);
            second.setPayOrderCount(2L);
            second.setPayGmv(new java.math.BigDecimal("12.00"));
            second.setRoi(new java.math.BigDecimal("1.7"));
            second.setGmtCreate(new java.util.Date());
            second.setGmtModified(new java.util.Date());

            mapper.upsertBatch(Arrays.asList(first));
            mapper.upsertBatch(Arrays.asList(second));

            try (PreparedStatement ps = session.getConnection().prepareStatement(
                    "SELECT COUNT(*) AS cnt, MAX(stat_cost) AS cost, MAX(show_cnt) AS showCnt, MAX(pay_order_count) AS orderCnt FROM wk_qc_oe_qc_advertiser_daily WHERE advertiser_id=? AND stat_date=?")) {
                ps.setLong(1, 201L);
                ps.setDate(2, statDate);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt("cnt")).isEqualTo(1);
                    assertThat(rs.getBigDecimal("cost")).isEqualByComparingTo("7.00");
                    assertThat(rs.getLong("showCnt")).isEqualTo(3L);
                    assertThat(rs.getLong("orderCnt")).isEqualTo(2L);
                }
            }
        }
    }

    private void parseMapper(Configuration configuration, String resource) throws Exception {
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperBuilder.parse();
        }
    }
}
