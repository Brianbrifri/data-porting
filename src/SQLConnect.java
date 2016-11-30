import java.sql.*;

public class SQLConnect {
    //Driver and URL
    private String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private String DB_URL = "jdbc:mysql://lacuna.dhcp.umsl.edu:3306/millerkei";

    //User credentials
    String USER = "umsl_cs_team";
    String PASS = "!kmcR0ck5";

    private Connection conn;
    private Statement stmt;

    public void connect()
    {

//        USER = usr;
//        PASS = pw;
//        Connection conn = null;
//        Statement stmt = null;

        try{
            //Register JDBC
            Class.forName(JDBC_DRIVER);

            System.out.println("Connecting to a selected database..."); //Just for testing. Don't really "need"
            conn = DriverManager.getConnection(DB_URL, USER, PASS); //Connect
//            System.out.println("Connected to database successfully"); //same
//
//            //Basic query
//            System.out.println("Inserting into table");
//            stmt = conn.createStatement();
//
//            String sql = "INSERT INTO VOLTRON " +
//                    "VALUES (200, 'Tom Brown', 2008)";
//            stmt.executeUpdate(sql);


        } catch(Exception e) {
            e.printStackTrace();
        }
//        }finally{
//            //Close down
//            try{
//                if(stmt!=null)
//                    conn.close();
//            }catch(SQLException se){
//            }//does nothing
//            try{
//                if(conn!=null)
//                    conn.close();
//            }catch(SQLException se){
//                se.printStackTrace();
//            }
//        }
    }//end main

    public void disconnect() {
        try {
            conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public void insertObservationWith(String trialID, String emplID, String actorType, String anchorID, String mmntID, String textResponse, String obsDt) throws SQLException {

        //connect();


        PreparedStatement sql = conn.prepareStatement("INSERT INTO millerkei.P2_EQS_OBS (TRIAL_ID, EMPLID, ACTOR_TYPE, ANCHOR_ID, MMNT_ID, TEXT_RESPONSE, OBS_DT) VALUES (?, ?, ?, ?, ?, ?, ?);");
        sql.setString(1, trialID);
        sql.setString(2, emplID);
        sql.setString(3, actorType);
        sql.setString(4, anchorID);
        sql.setString(5, mmntID);
        sql.setString(6, textResponse);
        sql.setString(7, obsDt);
        sql.executeQuery();

        //disconnect();
    }
}
