package com.configdeployer.deployer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import org.hsqldb.jdbc.JDBCDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import com.configdeployer.binding.Database;

public abstract class AbstractDatabaseDeployerTest
{

    private static final String DB_JDBC_URL = "jdbc:hsqldb:mem:configdeployer";
    private static final String DB_USERNAME = "testuser";
    private static final String DB_PASSWORD = "testpassword";

    private Database databaseProfile;

    @BeforeClass
    public void setUp()
    {
        Connection connection = null;
        try
        {
            connection = DriverManager.getConnection(DB_JDBC_URL, DB_USERNAME, DB_PASSWORD);
            connection.setAutoCommit(true);

            PreparedStatement statement = connection
                    .prepareStatement("CREATE TABLE movie (name VARCHAR(50), year SMALLINT, genre VARCHAR(20))");
            statement.executeUpdate();
            statement.close();

            statement = connection.prepareStatement("INSERT INTO movie (name, year, genre) VALUES (?, ?, ?)");
            for (Movie movie : createExistingMovies())
            {
                statement.setString(1, movie.getName());
                statement.setShort(2, movie.getYear());
                statement.setString(3, movie.getGenre());
                statement.executeUpdate();
            }
            statement.close();
        }
        catch (SQLException e)
        {
            Assert.fail("Could not create or initialize test database.", e);
        }
        finally
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                Assert.fail("Could not close test database connection.", e);
            }
        }

        databaseProfile = new Database();
        databaseProfile.setDriverClass(JDBCDriver.class.getName());
        databaseProfile.setJdbcUrl(DB_JDBC_URL);
        databaseProfile.setUsername(DB_USERNAME);
        databaseProfile.setPassword(DB_PASSWORD);
        createProfile(databaseProfile);
    }

    protected abstract Collection<Movie> createExistingMovies();

    protected abstract void createProfile(Database databaseProfile);

    protected Connection deployProfile()
    {
        try
        {
            new DatabaseDeployer(databaseProfile).deploy();
        }
        catch (DeployerException e)
        {
            Assert.fail("Could not deploy change to database.", e);
        }

        Connection connection = null;
        try
        {
            connection = DriverManager.getConnection(DB_JDBC_URL, DB_USERNAME, DB_PASSWORD);
        }
        catch (SQLException e)
        {
            Assert.fail("Could not connect to test database.", e);
        }
        return connection;
    }

    protected static class Movie
    {

        private final String name;
        private final short year;
        private final String genre;

        public Movie(String name, short year, String genre)
        {
            super();
            this.name = name;
            this.year = year;
            this.genre = genre;
        }

        public String getName()
        {
            return name;
        }

        public short getYear()
        {
            return year;
        }

        public String getGenre()
        {
            return genre;
        }

    }

}
