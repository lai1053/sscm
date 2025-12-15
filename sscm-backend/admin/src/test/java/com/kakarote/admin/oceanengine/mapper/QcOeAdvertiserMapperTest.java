package com.kakarote.admin.oceanengine.mapper;

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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class QcOeAdvertiserMapperTest {

    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:adv_mapper;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS wk_qc_oe_advertiser");
            conn.createStatement().execute("CREATE TABLE wk_qc_oe_advertiser (\n" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY,\n" +
                    "advertiser_id BIGINT,\n" +
                    "last_cost_date DATE,\n" +
                    "is_deleted INT DEFAULT 0\n" +
                    ")");
            conn.createStatement().execute("INSERT INTO wk_qc_oe_advertiser(advertiser_id, last_cost_date, is_deleted) VALUES (1001, NULL, 0)");
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(QcOeAdvertiserMapper.class);
        parseMapper(configuration, "mapper/oceanengine/QcOeAdvertiserMapper.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void updateLastCostDateShouldKeepMaxStatDate() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            QcOeAdvertiserMapper mapper = session.getMapper(QcOeAdvertiserMapper.class);
            LocalDate first = LocalDate.now().minusDays(5);
            LocalDate earlier = LocalDate.now().minusDays(10);
            LocalDate later = LocalDate.now().minusDays(1);

            mapper.updateLastCostDateIfNewer(1001L, Date.valueOf(first));
            assertThat(selectLastCostDate(session)).isEqualTo(Date.valueOf(first));

            mapper.updateLastCostDateIfNewer(1001L, Date.valueOf(earlier));
            assertThat(selectLastCostDate(session)).isEqualTo(Date.valueOf(first));

            mapper.updateLastCostDateIfNewer(1001L, Date.valueOf(later));
            assertThat(selectLastCostDate(session)).isEqualTo(Date.valueOf(later));
        }
    }

    private void parseMapper(Configuration configuration, String resource) throws Exception {
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperBuilder.parse();
        }
    }

    private Date selectLastCostDate(SqlSession session) throws Exception {
        java.sql.ResultSet rs = session.getConnection()
                .prepareStatement("SELECT last_cost_date FROM wk_qc_oe_advertiser WHERE advertiser_id = 1001")
                .executeQuery();
        rs.next();
        return rs.getDate(1);
    }
}
