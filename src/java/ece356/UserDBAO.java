package ece356;

import java.sql.*;
import java.util.*;

public class UserDBAO {

    public static final String url = "jdbc:mysql://eceweb.uwaterloo.ca:3306";
    //public static final String url = "jdbc:mysql://eceweb.uwaterloo.ca:3306/";
    public static final String nid = "bmsaadat";
    public static final String user = "user_" + nid;
    public static final String pwd = "user_" + nid;

    public static Connection getConnection()
            throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        Connection con = DriverManager.getConnection(url, user, pwd);
        Statement stmt = null;
        try {
            con.createStatement();
            stmt = con.createStatement();
            stmt.execute("USE ece356db_" + nid);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
        return con;
    }

    public static void writeReview(ReviewData review)
            throws ClassNotFoundException, SQLException {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = getConnection();
            pstmt = con.prepareStatement("INSERT INTO review (doc_username, patient_username, date, rating, comment) VALUES (?, ?, NOW(), ?, ?);");
            pstmt.setString(1, review.getDoctorUsername());
            pstmt.setString(2, review.getPatientUsername());
            pstmt.setInt(3, review.getRating());
            pstmt.setString(4, review.getComment());
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (con != null) {
                con.close();
            }
        }
    }
    
    public static void addFriend(PatientData friendA, PatientData friendB)
            throws ClassNotFoundException, SQLException {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = getConnection();
            
            // Find if this request is already there
            String query = "SELECT * FROM friend where (sent_username = ? AND received_username = ?) OR (sent_username = ? AND received_username = ?)";
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, friendA.getUserName());
            pstmt.setString(2, friendB.getUserName());
            pstmt.setString(3, friendB.getUserName());
            pstmt.setString(4, friendA.getUserName());

            ResultSet resultSet;
            resultSet = pstmt.executeQuery();
            resultSet.next();
            
            if (resultSet == null) {
                pstmt = con.prepareStatement("INSERT INTO friend (sent_username, received_username) VALUES (?, ?);");
                pstmt.setString(1, friendA.getUserName());
                pstmt.setString(2, friendB.getUserName());
                pstmt.executeUpdate();
            } else {
                boolean isAccepted = resultSet.getBoolean("isAccepted");
                if (!isAccepted) {
                    String update = "UPDATE friend SET isAccepted = ? where (sent_username = ? AND received_username = ?) OR (sent_username = ? AND received_username = ?);";
                    pstmt = con.prepareStatement(update);
                    pstmt.setBoolean(1, true);
                    pstmt.setString(2, friendA.getUserName());
                    pstmt.setString(3, friendB.getUserName());
                    pstmt.setString(4, friendB.getUserName());
                    pstmt.setString(5, friendA.getUserName());
                    pstmt.executeUpdate();
                }
            }
            
            
            
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (con != null) {
                con.close();
            }
        }
    }

    public static DoctorData queryDoctor(String userName)
            throws ClassNotFoundException, SQLException {
        Connection con = null;
        PreparedStatement pstmt = null;
        DoctorData ret;
        try {
            con = getConnection();

            // Query for general doctor information
            String query = "SELECT * FROM doctorView where username = ?";
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, userName);

            ResultSet resultSet;
            resultSet = pstmt.executeQuery();
            resultSet.next();
            ret = new DoctorData();
            ret.userName = resultSet.getString("username");
            ret.firstName = resultSet.getString("first_name");
            ret.lastName = resultSet.getString("last_name");
            ret.middleInitial = resultSet.getString("middle_initial");
            ret.gender = resultSet.getString("gender");
            ret.emailAddress = resultSet.getString("email_address");
            ret.yearsLicensed = resultSet.getInt("yearsLicensed");
            ret.averageRating = resultSet.getInt("averageRating");
            ret.numberOfReviews = resultSet.getInt("numberOfReviews");

            // Query for work addresses of doctor
            query = "SELECT * FROM doctorWorkAddressView where doc_address_username = ?";
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, userName);
            resultSet = pstmt.executeQuery();

            ArrayList<WorkAddressData> workAddressList = new ArrayList<WorkAddressData>();
            ret.workAddressList = workAddressList;
            while (resultSet.next()) {
                WorkAddressData workAddress = new WorkAddressData();
                workAddress.city = resultSet.getString("city");
                workAddress.state = resultSet.getString("state");
                workAddress.postalCode = resultSet.getString("postal_code");
                workAddress.streetName = resultSet.getString("street_name");
                workAddress.streetNumber = resultSet.getInt("street_number");
                workAddress.unitNumber = resultSet.getString("street_unit_number");
                workAddressList.add(workAddress);
            }

            // Query for specializations of doctor
            query = "SELECT * FROM doctorSpecializationView where doc_spec_username = ?";
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, userName);
            resultSet = pstmt.executeQuery();

            ArrayList<String> specializationList = new ArrayList<String>();
            ret.specializationList = specializationList;
            while (resultSet.next()) {
                String specialization = resultSet.getString("specTypeName");
                specializationList.add(specialization);
            }

            // Query for reviews of doctor
            query = "SELECT * FROM review where doc_username = ? order by date desc";
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, userName);
            resultSet = pstmt.executeQuery();

            ArrayList<ReviewData> reviewList = new ArrayList<ReviewData>();
            ret.reviewList = reviewList;
            while (resultSet.next()) {
                ReviewData review = new ReviewData();
                review.comment = resultSet.getString("comment");
                review.reviewId = resultSet.getString("reviewId");
                review.doctorUsername = resultSet.getString("doc_username");
                review.patientUsername = resultSet.getString("patient_username");
                review.date = resultSet.getDate("date");
                review.rating = resultSet.getInt("rating");
                reviewList.add(review);
            }

            return ret;
        } catch (Exception e) {
            System.out.println("EXCEPTION:%% " + e);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (con != null) {
                con.close();
            }
        }
        return null;
    }

    public static ArrayList<PatientData> queryPatients(String username, String state, String city)
            throws ClassNotFoundException, SQLException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ArrayList<PatientData> ret;
        try {
            con = getConnection();
            
            ArrayList<String> keys = new ArrayList();
            ArrayList<String> values = new ArrayList();
            if (username != null && username != "") {
                keys.add("patient_username");
                values.add(username);
            }
            
            if (state != null && state != "") {
                keys.add("home_address_state");
                values.add(state);
            }
            
            if (city != null && city != "") {
                keys.add("home_address_city");
                values.add(city);
            }
            

            // Query for general doctor information
            String query = "SELECT * FROM patientSearchView";
            if (!keys.isEmpty()) {
                query = query + " where";
                for (String key : keys) {
                    query = query + " " + key + " LIKE ?";
                    query += " AND";
                }
                query = query.substring(0, query.length()-4);
                System.out.println(query);
            }
            pstmt = con.prepareStatement(query);
            
            if (!values.isEmpty()) {
                int count = 1;
                for(String value : values) {
                    pstmt.setString(count, "%" + value + "%");
                    count++;
                }
            }

            ResultSet resultSet;
            resultSet = pstmt.executeQuery();           

            ret = new ArrayList();

            while (resultSet.next()) {
                PatientData patient = new PatientData();
                patient.userName = resultSet.getString("patient_username");
                patient.city = resultSet.getString("home_address_city");
                patient.state = resultSet.getString("home_address_state");
                patient.numberOfReviews = resultSet.getInt("numberOfReviews");
                patient.lastReviewDate = resultSet.getTimestamp("lastReviewDate");
                ret.add(patient);
            }

            return ret;
        } catch (Exception e) {
            System.out.println("EXCEPTION:%% " + e);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            if (con != null) {
                con.close();
            }
        }
        return null;
    }
}
