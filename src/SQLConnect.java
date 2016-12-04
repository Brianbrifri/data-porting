import java.sql.*;

public class SQLConnect {

    private Connection conn;

    public boolean connect(StringBuilder usr, StringBuilder pswd)
    {

        try{
            //Register JDBC
            String JDBC_DRIVER = "com.mysql.jdbc.Driver";
            Class.forName(JDBC_DRIVER).newInstance();

            System.out.println("Connecting to a millerkei DB..."); //Just for testing. Don't really "need"
            String DB_URL = "jdbc:mysql://lacuna.dhcp.umsl.edu:3306/millerkei";
            conn = DriverManager.getConnection(DB_URL, usr.toString(), pswd.toString()); //Connect
            usr.setLength(0);
            pswd.setLength(0);
            return true;

        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }


    }//end main

    public void disconnect() {
        try {
            conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public void insertObservationWith(String trialID, String emplID, String actorType, String anchorID, String mmntID, String textResponse, String obsDt) throws SQLException {

        PreparedStatement sql = conn.prepareStatement("INSERT INTO P2_EQS_OBS (TRIAL_ID, EMPLID, ACTOR_TYPE, ANCHOR_ID, MMNT_ID, TEXT_RESPONSE, OBS_DT) VALUES (?, ?, ?, ?, ?, ?, ?);");
        sql.setString(1, trialID);
        sql.setString(2, emplID);
        sql.setString(3, actorType);
        sql.setString(4, anchorID);
        sql.setString(5, mmntID);
        sql.setString(6, textResponse);
        sql.setString(7, obsDt);
        sql.executeUpdate();

    }

    public String getEmplidMappingFrom(String ssoid) throws SQLException {
        String emplid = "";
        PreparedStatement sql = conn.prepareStatement("SELECT EMPLID FROM SSOID_MAPPINGS WHERE SSO_ID = ?;");
        sql.setString(1, ssoid);
        ResultSet rs = sql.executeQuery();
        while(rs.next()) {
            emplid = rs.getString("EMPLID");
        }
        return emplid;
    }

    public String getWritingQualityFrom(String level) throws SQLException {
        String mmnt = null;
        PreparedStatement sql = conn.prepareStatement("SELECT ANCHOR_ID FROM P2_WRITING_ANCHORS WHERE ANCHOR_TEXT LIKE ?;");
        sql.setString(1, level);
        ResultSet rs = sql.executeQuery();
        while(rs.next()) {
            mmnt = rs.getString("ANCHOR_ID");
        }
        return mmnt;
    }

    public String getTermFrom(String date) throws SQLException {
        String term = "0000";
        PreparedStatement sql = conn.prepareStatement("SELECT STRM FROM TERMS WHERE ? BETWEEN TERM_BEGIN_DT AND TERM_END_DT;");
        sql.setString(1, date);
        ResultSet rs = sql.executeQuery();
        while(rs.next()) {
            term = rs.getString("STRM");
        }
        return term;
    }

    public String getCourseIdFrom(String desc) throws SQLException {
        String courseID = "108900";
        PreparedStatement sql = conn.prepareStatement("SELECT COURSE_ID FROM COURSE_MAPPINGS WHERE COURSEDESC = ?;");
        sql.setString(1, desc);
        ResultSet rs = sql.executeQuery();
        while(rs.next()) {
            System.out.println("Got results");
            courseID = rs.getString("COURSE_ID");
        }
        return courseID;
    }
}
