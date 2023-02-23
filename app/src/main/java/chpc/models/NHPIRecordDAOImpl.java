package chpc.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NHPIRecordDAOImpl implements NHPIRecordDAO {
  private Connection conn;

  public NHPIRecordDAOImpl(Connection conn) {
    this.conn = conn;
  }

  @Override
  public List<NHPIRecord> getAllRecords() throws SQLException {
    String query = "SELECT * FROM nhpiRecords;";
    List<NHPIRecord> records = new ArrayList<>();
    try (Statement statement = conn.createStatement()) {
      System.out.println("Getting all nhpi records...");
      ResultSet results = statement.executeQuery(query);
      while (results.next()) {
        String resultGeo = results.getString("geo");
        String resultRefDate = results.getString("refDate");
        float resultValue = results.getFloat("value");
        records.add(new NHPIRecord(resultGeo, resultRefDate, resultValue));
      }
      System.out.println("Successfully get all records");
      return records;
    } catch (SQLException e) {
      System.out.println("Something went wrong when trying to get all records");
      throw e;
    }
  }

  @Override
  public List<NHPIRecord> getRecordsByGeoAndDateRange(String geo, int startYear, int startMonth, int endYear,
      int endMonth) throws SQLException {
    String query = "SELECT * FROM nhpiRecords WHERE geo = '" + geo + "' AND refDate IN (";
    var dates = this.generateDateRange(startYear, startMonth, endYear, endMonth);
    for (String date : dates) {
      query += "'" + date + "',";
    }
    // remove extra comma at the end
    query = query.substring(0, query.length() - 1);

    query += ");";

    List<NHPIRecord> records = new ArrayList<>();
    try (Statement statement = conn.createStatement()) {
      System.out
          .println("Getting nhpi records for " + geo + " from " + dates.get(0) + " to " + dates.get(dates.size() - 1));
      ResultSet results = statement.executeQuery(query);
      while (results.next()) {
        String resultGeo = results.getString("geo");
        String resultRefDate = results.getString("refDate");
        float resultValue = results.getFloat("value");
        records.add(new NHPIRecord(resultGeo, resultRefDate, resultValue));
      }
      System.out.println("Successfully get records!");
      return records;
    } catch (SQLException e) {
      System.out.println("Something went wrong when trying to records");
      throw e;
    }
  }

  @Override
  public List<String> getAllGeos() throws SQLException {
    String query = "SELECT DISTINCT geo FROM nhpiRecords";
    var geos = new ArrayList<String>();
    try (Statement statement = conn.createStatement()) {
      System.out
          .println("Getting all available geos!");
      ResultSet results = statement.executeQuery(query);
      while (results.next()) {
        String resultGeo = results.getString("geo");
        geos.add(resultGeo);
      }
      System.out.println("Successfully get all geos!");
      return geos;
    } catch (SQLException e) {
      System.out.println("Something went wrong when trying to records");
      throw e;
    }
  }

  @Override
  public void resetAndSeed() throws SQLException {
    this.dropNHPIRecordsTable(this.conn);
    this.createNHPIRecordsTable(conn);
    List<NHPIRecord> records = this.readNHPIRecordFromCSV("18100205.csv");
    this.insertDataIntoNHPIRecordsTable(conn, records);
  }

  private List<NHPIRecord> readNHPIRecordFromCSV(String fileName) {
    List<NHPIRecord> nhpiRecords = new ArrayList<>();
    InputStream csvInputStream = getFileFromResourceAsStream(fileName);

    try (InputStreamReader streamReader = new InputStreamReader(
        csvInputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader)) {

      String line = reader.readLine();
      // ignore first line and read next line
      while ((line = reader.readLine()) != null) {
        if (!line.contains("House only")) {
          continue;
        }

        List<String> attributes = new ArrayList<String>();

        Matcher m = Pattern.compile(
            "\"([^\"]*)\"")
            .matcher(line);
        while (m.find()) {
          attributes.add(m.group(1));
        }

        String geo = attributes.get(0);
        String refDate = attributes.get(1);
        float value = -1;
        String valueString = attributes.get(10);
        if (!valueString.isEmpty()) {
          value = Float.parseFloat(valueString);
        }
        NHPIRecord nhpiRecord = new NHPIRecord(refDate, geo, value);
        nhpiRecords.add(nhpiRecord);
      }

    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return nhpiRecords;
  }

  private InputStream getFileFromResourceAsStream(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);

    if (inputStream == null) {
      throw new IllegalArgumentException("file not found! " + fileName);
    } else {
      return inputStream;
    }
  }

  private void createNHPIRecordsTable(Connection conn) throws SQLException {
    String sql = "CREATE TABLE IF NOT EXISTS nhpiRecords " +
        "(" +
        "id SERIAL PRIMARY KEY," +
        "geo VARCHAR(200) NOT NULL," +
        "refDate VARCHAR(20) NOT NULL," +
        "value REAL NOT NULL" +
        ");";

    try (Statement statement = conn.createStatement()) {
      System.out.println("Creating nhpiRecords table...");
      statement.executeUpdate(sql);
      System.out.println("Table nhpiRecords created!");
    } catch (SQLException e) {
      System.out.println("Something went wrong! Failed to create table nhpiRecord");
      System.err.println(e.getMessage());
      throw e;
    }
  }

  private void insertDataIntoNHPIRecordsTable(Connection conn, List<NHPIRecord> records) throws SQLException {
    String sql = "insert into nhpiRecords(geo, refDate, value) values (?, ?, ?)";
    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      System.err.println("Batch inserting into nhpiRecords...");
      for (NHPIRecord record : records) {
        statement.clearParameters();
        statement.setString(1, record.getGeo());
        statement.setString(2, record.getRefDate());
        statement.setDouble(3, record.getValue());
        statement.addBatch();
      }

      statement.executeBatch();

      System.err.println("Batch insert complete!");
    } catch (SQLException e) {
      System.out.println("Something went wrong! Failed to batch insert into table nhpiRecords");
      System.err.println(e.getMessage());
      throw e;
    }
  }

  private void dropNHPIRecordsTable(Connection conn) throws SQLException {
    String sql = "DROP TABLE IF EXISTS nhpiRecords";
    try (Statement statement = conn.createStatement()) {
      System.out.println("Dropping table nhpiRecords...");
      statement.executeUpdate(sql);
      System.out.println("Table nhpiRecords dropped!");
    } catch (SQLException e) {
      System.out.println("Something went wrong! Failed to drop table nhpiRecords");
      System.err.println(e.getMessage());
      throw e;
    }
  }

  /**
   * Generate a array of date string based on a range.
   * Ex: 2012-08 to 2013-02 will generate:
   * ['2012-08','2012-09','2012-10',...,'2013-01','2012-02']
   * 
   * @param startYear  Start year
   * @param startMonth Start month
   * @param endYear    End year
   * @param endMonth   End month
   * @return Array of String that represent the date range
   */
  private List<String> generateDateRange(int startYear, int startMonth, int endYear, int endMonth) {
    var dates = new ArrayList<String>();

    for (int y = startYear; y <= endYear; y++) {
      for (int m = (y == startYear ? startMonth : 1); m <= (y == endYear ? endMonth : 12); m++) {
        if (m < 10) {
          dates.add(y + "-0" + m);
        } else {
          dates.add(y + "-" + m);
        }
      }
    }

    return dates;
  }

  public static void main(String[] args) {
    String uri = "jdbc:postgresql://localhost:5432/nhpi";
    Properties props = new Properties();
    props.setProperty("user", "admin");
    props.setProperty("password", "admin");
    props.setProperty("ssl", "false");
    Db.initializeDb(uri, props);
    Connection conn = Db.getConnection();
    NHPIRecordDAOImpl nhpiRecordDAO = new NHPIRecordDAOImpl(conn);
    try {
      nhpiRecordDAO.resetAndSeed();
      // List<NHPIRecord> records = nhpiRecordDAO.getAllRecords();
      // for (NHPIRecord record : records) {
      // System.out.println(record);
      // }

      // var geos = nhpiRecordDAO.getAllGeos();
      // for (String geo : geos) {
      // System.out.println(geo);
      // }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
