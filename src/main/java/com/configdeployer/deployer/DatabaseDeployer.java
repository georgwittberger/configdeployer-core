package com.configdeployer.deployer;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.configdeployer.binding.Database;
import com.configdeployer.binding.DatabaseAddEntry;
import com.configdeployer.binding.DatabaseDeleteEntry;
import com.configdeployer.binding.DatabaseEntryColumnType;
import com.configdeployer.binding.DatabaseEntryColumnValue;
import com.configdeployer.binding.DatabaseUpdateEntry;

public class DatabaseDeployer
{

    private static final Logger logger = LoggerFactory.getLogger(DatabaseDeployer.class);
    private final Database databaseProfile;

    public DatabaseDeployer(Database databaseProfile)
    {
        this.databaseProfile = databaseProfile;
    }

    public void deploy() throws DeployerException
    {
        BasicDataSource dataSource = getDataSource();
        Connection connection = null;
        try
        {
            connection = dataSource.getConnection();
            logger.info("Connection to database '{}' successfully opened.", databaseProfile.getJdbcUrl());
            for (Object entry : databaseProfile.getAddOrUpdateOrDelete())
            {
                if (entry instanceof DatabaseAddEntry)
                {
                    applyEntry((DatabaseAddEntry) entry, connection);
                }
                else if (entry instanceof DatabaseUpdateEntry)
                {
                    applyEntry((DatabaseUpdateEntry) entry, connection);
                }
                else if (entry instanceof DatabaseDeleteEntry)
                {
                    applyEntry((DatabaseDeleteEntry) entry, connection);
                }
            }
            connection.commit();
            logger.info("Changes to database '{}' successfully committed.", databaseProfile.getJdbcUrl());
        }
        catch (SQLException e)
        {
            if (connection != null)
            {
                try
                {
                    connection.rollback();
                    logger.info("Changes to database '{}' successfully rolled back.", databaseProfile.getJdbcUrl());
                }
                catch (SQLException rbe)
                {
                    logger.error("Could not rollback transaction!", rbe);
                }
            }
            throw new DeployerException("Could not deploy configuration to database: " + databaseProfile.getJdbcUrl(),
                    e);
        }
        finally
        {
            if (connection != null)
            {
                try
                {
                    connection.close();
                    logger.info("Connection to database '{}' successfully closed.", databaseProfile.getJdbcUrl());
                }
                catch (SQLException e)
                {
                    logger.error("Could not close connection!", e);
                }
            }
            try
            {
                dataSource.close();
                logger.info("Connection pool for database '{}' successfully closed.", databaseProfile.getJdbcUrl());
            }
            catch (SQLException e)
            {
                logger.error("Could not close connection pool!", e);
            }
        }
    }

    private BasicDataSource getDataSource()
    {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(databaseProfile.getDriverClass());
        dataSource.setUrl(databaseProfile.getJdbcUrl());
        if (databaseProfile.getUsername() != null)
        {
            dataSource.setUsername(databaseProfile.getUsername());
        }
        if (databaseProfile.getPassword() != null)
        {
            dataSource.setPassword(databaseProfile.getPassword());
        }
        dataSource.setDefaultAutoCommit(false);
        return dataSource;
    }

    private void applyEntry(DatabaseAddEntry entry, Connection connection) throws SQLException
    {
        if (!rowsExist(entry.getTable(), entry.getSet(), connection))
        {
            int insertedRowsCount = executeInsert(entry.getTable(), entry.getSet(), connection);
            if (insertedRowsCount > 0)
            {
                logger.info("Inserted {} rows into table '{}'.", insertedRowsCount, entry.getTable());
            }
            else
            {
                logger.error("No rows were inserted into table '{}'.", entry.getTable());
            }
        }
        else
        {
            logger.info("Skipped inserting into table '{}' because rows with specified values already exist.",
                    entry.getTable());
        }
    }

    private void applyEntry(DatabaseUpdateEntry entry, Connection connection) throws SQLException
    {
        if (StringUtils.isBlank(entry.getCondition()))
        {
            throw new SQLException("Update operation without condition is not permitted! (Table: " + entry.getTable()
                    + ")");
        }
        int updatedRowsCount = executeUpdate(entry.getTable(), entry.getCondition(), entry.getSet(), connection);
        if (updatedRowsCount > 0)
        {
            logger.info("Updated {} rows in table '{}' matching condition '{}'.", updatedRowsCount, entry.getTable(),
                    entry.getCondition());
        }
        else
        {
            logger.info("No rows were updated in table '{}' because none matched condition '{}'.", entry.getTable(),
                    entry.getCondition());
        }
    }

    private void applyEntry(DatabaseDeleteEntry entry, Connection connection) throws SQLException
    {
        if (StringUtils.isBlank(entry.getCondition()))
        {
            throw new SQLException("Delete operation without condition is not permitted! (Table: " + entry.getTable()
                    + ")");
        }
        int deletedRowsCount = executeDelete(entry.getTable(), entry.getCondition(), connection);
        if (deletedRowsCount > 0)
        {
            logger.info("Deleted {} rows from table '{}' matching condition '{}'.", deletedRowsCount, entry.getTable(),
                    entry.getCondition());
        }
        else
        {
            logger.info("No rows were deleted from table '{}' because none matched condition '{}'.", entry.getTable(),
                    entry.getCondition());
        }
    }

    private boolean rowsExist(String tableName, List<DatabaseEntryColumnValue> columnValues, Connection connection)
            throws SQLException
    {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE ";
        sql += joinColumnNames(columnValues, " = ?", " AND ");
        PreparedStatement statement = null;
        try
        {
            statement = connection.prepareStatement(sql);
            mapColumnValuesToStatement(columnValues, statement);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() && resultSet.getInt(1) > 0;
        }
        finally
        {
            closeStatement(statement);
        }
    }

    private int executeInsert(String tableName, List<DatabaseEntryColumnValue> columnValues, Connection connection)
            throws SQLException
    {
        String sql = "INSERT INTO " + tableName + " (";
        sql += joinColumnNames(columnValues, "", ", ");
        sql += ") VALUES (" + StringUtils.repeat("?", ", ", columnValues.size()) + ")";
        PreparedStatement statement = null;
        try
        {
            statement = connection.prepareStatement(sql);
            mapColumnValuesToStatement(columnValues, statement);
            return statement.executeUpdate();
        }
        finally
        {
            closeStatement(statement);
        }
    }

    private int executeUpdate(String tableName, String condition, List<DatabaseEntryColumnValue> columnValues,
            Connection connection) throws SQLException
    {
        String sql = "UPDATE " + tableName + " SET ";
        sql += joinColumnNames(columnValues, " = ?", ", ");
        sql += " WHERE " + condition;
        PreparedStatement statement = null;
        try
        {
            statement = connection.prepareStatement(sql);
            mapColumnValuesToStatement(columnValues, statement);
            return statement.executeUpdate();
        }
        finally
        {
            closeStatement(statement);
        }
    }

    private int executeDelete(String tableName, String condition, Connection connection) throws SQLException
    {
        String sql = "DELETE FROM " + tableName + " WHERE " + condition;
        PreparedStatement statement = null;
        try
        {
            statement = connection.prepareStatement(sql);
            return statement.executeUpdate();
        }
        finally
        {
            closeStatement(statement);
        }
    }

    private String joinColumnNames(List<DatabaseEntryColumnValue> columnValues, String columnSuffix, String separator)
    {
        String joinedColumnNames = "";
        boolean firstColumn = true;
        for (DatabaseEntryColumnValue columnValue : columnValues)
        {
            if (firstColumn)
            {
                firstColumn = false;
            }
            else
            {
                joinedColumnNames += separator;
            }
            joinedColumnNames += columnValue.getColumn() + columnSuffix;
        }
        return joinedColumnNames;
    }

    private void mapColumnValuesToStatement(List<DatabaseEntryColumnValue> columnValues, PreparedStatement statement)
            throws SQLException
    {
        for (int parameterIndex = 1; parameterIndex <= columnValues.size(); parameterIndex++)
        {
            DatabaseEntryColumnValue columnValue = columnValues.get(parameterIndex - 1);
            if (columnValue.getValue() == null)
            {
                statement.setNull(parameterIndex, getSQLTypeByColumnType(columnValue.getType()));
                continue;
            }
            switch (columnValue.getType())
            {
                case BIGINT:
                    statement.setLong(parameterIndex, Long.valueOf(columnValue.getValue()));
                    break;
                case BOOLEAN:
                    statement.setBoolean(parameterIndex, Boolean.parseBoolean(columnValue.getValue()));
                    break;
                case CLOB:
                    Reader reader = new StringReader(columnValue.getValue());
                    statement.setClob(parameterIndex, reader, columnValue.getValue().length());
                    break;
                case DATE:
                    statement.setDate(parameterIndex, Date.valueOf(columnValue.getValue()));
                    break;
                case DOUBLE:
                    statement.setDouble(parameterIndex, Double.valueOf(columnValue.getValue()));
                    break;
                case FLOAT:
                    statement.setFloat(parameterIndex, Float.valueOf(columnValue.getValue()));
                    break;
                case INTEGER:
                    statement.setInt(parameterIndex, Integer.valueOf(columnValue.getValue()));
                    break;
                case NUMERIC:
                    statement.setBigDecimal(parameterIndex, new BigDecimal(columnValue.getValue()));
                    break;
                case SMALLINT:
                    statement.setShort(parameterIndex, Short.valueOf(columnValue.getValue()));
                    break;
                case TIME:
                    statement.setTime(parameterIndex, Time.valueOf(columnValue.getValue()));
                    break;
                case TIMESTAMP:
                    statement.setTimestamp(parameterIndex, Timestamp.valueOf(columnValue.getValue()));
                    break;
                case TINYINT:
                    statement.setByte(parameterIndex, Byte.valueOf(columnValue.getValue()));
                    break;
                case VARCHAR:
                    statement.setString(parameterIndex, columnValue.getValue());
                    break;
                default:
                    statement.setObject(parameterIndex, columnValue.getValue(),
                            getSQLTypeByColumnType(columnValue.getType()));
                    break;
            }
        }
    }

    private int getSQLTypeByColumnType(DatabaseEntryColumnType columnType)
    {
        switch (columnType)
        {
            case BIGINT:
                return Types.BIGINT;
            case BOOLEAN:
                return Types.BOOLEAN;
            case CLOB:
                return Types.CLOB;
            case DATE:
                return Types.DATE;
            case DOUBLE:
                return Types.DOUBLE;
            case FLOAT:
                return Types.FLOAT;
            case INTEGER:
                return Types.INTEGER;
            case NUMERIC:
                return Types.NUMERIC;
            case SMALLINT:
                return Types.SMALLINT;
            case TIME:
                return Types.TIME;
            case TIMESTAMP:
                return Types.TIMESTAMP;
            case TINYINT:
                return Types.TINYINT;
            case VARCHAR:
                return Types.VARCHAR;
        }
        return Types.OTHER;
    }

    private void closeStatement(PreparedStatement statement)
    {
        if (statement != null)
        {
            try
            {
                statement.close();
            }
            catch (SQLException e)
            {
                logger.error("Could not close prepared statement!", e);
            }
        }
    }

}
