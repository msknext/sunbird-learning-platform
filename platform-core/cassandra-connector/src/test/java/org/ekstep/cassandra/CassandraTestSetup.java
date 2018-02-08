package org.ekstep.cassandra;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.ekstep.cassandra.connector.util.CassandraConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.datastax.driver.core.Session;

public class CassandraTestSetup {

	private static Session session = null;

	@AfterClass
	public static void afterTest() {
		tearEmbeddedCassandraSetup();
	}

	@BeforeClass
	public static void before() {
		setupEmbeddedCassandra();
	}

	private static void setupEmbeddedCassandra() {
		try {
			EmbeddedCassandraServerHelper.startEmbeddedCassandra("/cassandra-unit.yaml", 100000L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void tearEmbeddedCassandraSetup() {
		try {
			if (!session.isClosed())
				session.close();
			EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void executeScript(String... querys) {

		try {
			session = CassandraConnector.getSession();
			for (String query : querys) {
				session.execute(query);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}