package com.configdeployer.deployer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.configdeployer.binding.Database;
import com.configdeployer.binding.DatabaseAddEntry;
import com.configdeployer.binding.DatabaseEntryColumnType;
import com.configdeployer.binding.DatabaseEntryColumnValue;

public class DatabaseDeployerAddTest extends AbstractDatabaseDeployerTest
{

    @Test(groups = { "database", "deployer" }, description = "Adding database entries: insert non-existing entry and preserve value of existing entry")
    public void testAddDatabaseEntry()
    {
        Connection connection = deployProfile();
        try
        {
            PreparedStatement statement = connection
                    .prepareStatement("SELECT name, year, genre FROM movie ORDER BY name");
            ResultSet resultSet = statement.executeQuery();
            for (byte rowNumber = 1; rowNumber <= 2; rowNumber++)
            {
                Assert.assertTrue(resultSet.next(), "There are less entries than expected.");
                String name = resultSet.getString("name");
                short year = resultSet.getShort("year");
                String genre = resultSet.getString("genre");
                switch (rowNumber)
                {
                    case 1:
                        Assert.assertEquals(name, "Inception");
                        Assert.assertEquals(year, (short) 2010);
                        Assert.assertEquals(genre, "Action");
                        break;
                    case 2:
                        Assert.assertEquals(name, "Terminator");
                        Assert.assertEquals(year, (short) 1984);
                        Assert.assertEquals(genre, "Sci-Fi");
                        break;
                }
            }
            Assert.assertFalse(resultSet.next(), "There are more entries than expected.");
            resultSet.close();
            statement.close();
        }
        catch (SQLException e)
        {
            Assert.fail("Could not retrieve data from test database.", e);
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
    }

    @Override
    protected Collection<Movie> createExistingMovies()
    {
        Collection<Movie> existingMovies = new ArrayList<AbstractDatabaseDeployerTest.Movie>();
        existingMovies.add(new Movie("Terminator", (short) 1984, "Sci-Fi"));
        return existingMovies;
    }

    @Override
    protected void createProfile(Database databaseProfile)
    {
        DatabaseAddEntry addEntry = new DatabaseAddEntry();
        addEntry.setTable("movie");
        DatabaseEntryColumnValue columnValue = new DatabaseEntryColumnValue();
        columnValue.setColumn("name");
        columnValue.setType(DatabaseEntryColumnType.VARCHAR);
        columnValue.setValue("Inception");
        addEntry.getSet().add(columnValue);
        columnValue = new DatabaseEntryColumnValue();
        columnValue.setColumn("year");
        columnValue.setType(DatabaseEntryColumnType.SMALLINT);
        columnValue.setValue("2010");
        addEntry.getSet().add(columnValue);
        columnValue = new DatabaseEntryColumnValue();
        columnValue.setColumn("genre");
        columnValue.setType(DatabaseEntryColumnType.VARCHAR);
        columnValue.setValue("Action");
        addEntry.getSet().add(columnValue);
        databaseProfile.getAddOrUpdateOrDelete().add(addEntry);

        addEntry = new DatabaseAddEntry();
        addEntry.setTable("movie");
        columnValue = new DatabaseEntryColumnValue();
        columnValue.setColumn("name");
        columnValue.setType(DatabaseEntryColumnType.VARCHAR);
        columnValue.setValue("Terminator");
        addEntry.getSet().add(columnValue);
        columnValue = new DatabaseEntryColumnValue();
        columnValue.setColumn("year");
        columnValue.setType(DatabaseEntryColumnType.SMALLINT);
        columnValue.setValue("1984");
        addEntry.getSet().add(columnValue);
        columnValue = new DatabaseEntryColumnValue();
        columnValue.setColumn("genre");
        columnValue.setType(DatabaseEntryColumnType.VARCHAR);
        columnValue.setValue("Sci-Fi");
        addEntry.getSet().add(columnValue);
        databaseProfile.getAddOrUpdateOrDelete().add(addEntry);
    }

}
