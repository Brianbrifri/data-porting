import java.sql.*;

public class SQLConnect {
    //Driver and URL
    String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    String DB_URL = "jdbc:mysql://lacuna.dhcp.umsl.edu:3306/millerkei";

    //User credentials
    String USER = "";
    String PASS = "";

    public void connect(String usr, String pw)
    {



        USER = usr;
        PASS = pw;
        Connection conn = null;
        Statement stmt = null;

        try{
            //Register JDBC
            Class.forName("com.mysql.jdbc.Driver");



            System.out.println("Connecting to a selected database..."); //Just for testing. Don't really "need"
            conn = DriverManager.getConnection(DB_URL, USER, PASS); //Connect
            System.out.println("Connected to database successfully"); //same

            //Basic query
            //System.out.println("Inserting into table");
            stmt = conn.createStatement();

            String sql = "INSERT INTO VOLTRON " +
                    "VALUES (200, 'Tom Brown', 2008)";
            //stmt.executeUpdate(sql);


        }catch(SQLException se){
            //Handle errors
            se.printStackTrace();
        }catch(Exception e){

            e.printStackTrace();
        }finally{
            //Close down
            try{
                if(stmt!=null)
                    conn.close();
            }catch(SQLException se){
            }//does nothing
            try{
                if(conn!=null)
                    conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
    }//end main
}
