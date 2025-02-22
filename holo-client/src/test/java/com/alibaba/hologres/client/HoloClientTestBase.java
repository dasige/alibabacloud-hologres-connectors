/*
 * Copyright (c) 2021. Alibaba Group Holding Limited
 */

package com.alibaba.hologres.client;

import com.alibaba.hologres.client.exception.ExceptionCode;
import com.alibaba.hologres.client.exception.HoloClientException;
import com.alibaba.hologres.client.impl.util.ConnectionUtil;
import com.alibaba.hologres.client.model.HoloVersion;
import com.alibaba.hologres.client.model.TableSchema;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 测试基类.
 */
public class HoloClientTestBase {
	public static final Logger LOG = LoggerFactory.getLogger(HoloClientTestBase.class);
	protected static Properties properties;
	protected static HoloVersion holoVersion = new HoloVersion(0, 0, 0);

	public static int getShardCount(HoloClient client, TableSchema schema) throws HoloClientException {
		return get(client.sql(conn -> {
			int shardCount = -1;
			try (PreparedStatement ps = conn.prepareStatement("select g.property_value from hologres.hg_table_properties t,hologres.hg_table_group_properties g\n" +
					"where t.property_key='table_group' and g.property_key='shard_count' and table_namespace=? and table_name=? and t.property_value = g.tablegroup_name")) {
				ps.setObject(1, schema.getTableNameObj().getSchemaName());
				ps.setObject(2, schema.getTableNameObj().getTableName());
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						shardCount = rs.getInt(1);
					} else {
						throw new SQLException("table " + schema.getTableNameObj().getFullName() + " not exists");
					}
				}
			}
			return shardCount;
		}));
	}

	private static <T> T get(CompletableFuture<T> future) throws HoloClientException {
		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new HoloClientException(ExceptionCode.INTERNAL_ERROR, "interrupt", e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof HoloClientException) {
				throw (HoloClientException) cause;
			} else {
				throw new HoloClientException(ExceptionCode.INTERNAL_ERROR, "", cause);
			}
		}
	}

	/**
	 * url=jdbc:postgres://.....
	 * user=
	 * password=
	 */
	@BeforeClass
	public static void loadProperties() throws Exception {
		Class.forName("org.postgresql.Driver");
		properties = new Properties();

		File file = new File("endpoint.properties");
		if (file.exists()) {
			try (InputStream is = new FileInputStream(file)) {
				properties.load(is);
			}
		} else {
			String temp = System.getenv("holo_client_test_url");
			if (temp == null) {
				properties = null;
				return;
			}
			properties.setProperty("url", temp);

			temp = System.getenv("holo_client_test_user");
			if (temp == null) {
				properties = null;
				return;
			}
			properties.setProperty("user", temp);

			temp = System.getenv("holo_client_test_password");
			if (temp == null) {
				properties = null;
				return;
			}
			properties.setProperty("password", temp);
		}

		holoVersion = ConnectionUtil.getHoloVersion(buildConnection());
	}

	@Before
	public void before() throws Exception {
		doBefore();
	}

	protected void doBefore() throws Exception {

	}

	@After
	public void after() throws Exception {
	}

	protected void execute(Connection conn, String[] sqls) throws SQLException {
		for (String sql : sqls) {
			try (Statement stat = conn.createStatement()) {
				LOG.info("try execute {}", sql);
				stat.execute(sql);
			}
		}
	}

	protected static Connection buildConnection() throws SQLException {
		return DriverManager.getConnection(properties.getProperty("url"), properties);
	}

	protected HoloConfig buildConfig() {
		HoloConfig config = new HoloConfig();
		config.setJdbcUrl(properties.getProperty("url"));
		config.setUsername(properties.getProperty("user"));
		config.setPassword(properties.getProperty("password"));
		return config;
	}

	@Before
	public void getHoloVersion() throws SQLException {

	}

}
