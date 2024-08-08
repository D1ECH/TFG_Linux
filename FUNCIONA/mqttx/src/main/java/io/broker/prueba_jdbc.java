package io.broker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class prueba_jdbc {

    public static void main(String[] args) {
        //step 0
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Drivers loaded!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //step 1: Connect to the database
        try {
            Connection con= DriverManager.getConnection("jdbc:mysql://localhost:3306/trust_management","root","");
            System.out.println(con);
            Statement stmt = con.createStatement();
            String sql = "SELECT * FROM actor";
            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next())
                System.out.println(rs.getInt("actor_id")+"|"+rs.getString("first_name"));




        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

}
