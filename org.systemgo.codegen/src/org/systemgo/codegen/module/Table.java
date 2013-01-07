package org.systemgo.codegen.module;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.systemgo.codegen.pub.DbUtil;
import org.systemgo.codegen.pub.TypeConverter;

public class Table {
	private String tableName;

	private String tablespaceName;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTablespaceName() {
		return tablespaceName;
	}

	public void setTablespaceName(String tablespaceName) {
		this.tablespaceName = tablespaceName;
	}

	public static List<Table> queryTable(String tableName) {
		Connection conn = DbUtil.getConn();
		ResultSetHandler<List<Table>> rsh = new BeanListHandler<Table>(
				Table.class);
		QueryRunner qr = new QueryRunner();
		List<Table> listTable = null;
		try {
			listTable = qr
					.query(conn,
							"SELECT aa.table_name tableName,aa.tablespace_name  tablespaceName FROM all_tables aa where aa.owner = 'DEVFRAMEWORK' and aa.table_name like ?",
							rsh, tableName + "%");
		} catch (SQLException e) {
			DbUtils.printStackTrace(e);
		}

		return listTable;
	}

	/* get columns in database style */
	public static List<Column> getColumns(String currentTableName) {
		List<Column> columns = new ArrayList<Column>();
		String tableName = currentTableName.toUpperCase();
		Connection connection = null;
		String sql = "select * from " + tableName + " where 1=0";
		Statement st = null;
		ResultSet rs = null;
		try {
			connection = DbUtil.getConn();
			DatabaseMetaData dbmd = connection.getMetaData();
			ResultSet pkrs = dbmd.getPrimaryKeys(null, null, tableName);
			List<String> pks = new ArrayList<String>();
			while (pkrs.next()) {
				String pk = pkrs.getString("COLUMN_NAME");
				pks.add(pk);
			}
			st = connection.createStatement();
			rs = st.executeQuery(sql);
			ResultSetMetaData rsmd = rs.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				Column column = new Column();
				String name = rsmd.getColumnName(i);
				String type = rsmd.getColumnClassName(i);
				column.setName(name.toUpperCase());
				column.setType(TypeConverter.convert(type));
				column.setNumeric(TypeConverter.isNumeric(rsmd.getColumnType(i)));
				if (pks.contains(name)) {
					column.setPk(true);
				}
				if (ResultSetMetaData.columnNoNulls == rsmd.isNullable(i)) {
					column.setNullable(false);
				}
				columns.add(column);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
			}
			DbUtils.closeQuietly(connection);
		}
		return columns;
	}

}
