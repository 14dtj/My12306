import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * Created by tjDu on 2016/11/7.
 */
public class MysqlDBJDBC {
    private static Connection conn = null;
    private static String url = "jdbc:mysql://localhost:3306/my12306?"
            + "user=root&password=123456&useUnicode=true&characterEncoding=UTF8&useSSL=true&serverTimezone=UTC";
    private static String[] seatTypes = {"商务座", "一等座", "二等座", "无座"};
    private static double[] seatBasePrice = {100, 50, 0, 0};
    private static int[] carriage = {0, 5, 10, 15};
    private static Date today;

    private static void setUpConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void initTickets() {
        setUpConnection();
        Calendar cal = Calendar.getInstance();
        today = new Date(cal.getTimeInMillis());
        try {
            String sql = "select * from route;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String trainNum = resultSet.getString(1);
                String route = resultSet.getString(2);
                String[] routes = route.split("-");
                for (int i = 0; i < routes.length; i++) {
                    for (int j = i + 1; j < routes.length; j++) {
                        for (int k = 0; k < seatTypes.length; k++) {
                            sql = "insert into ticket_info(trainNum,departure,destination," +
                                    "seatType,price,date,remain) values(?,?,?,?,?,?,?)";
                            ps = conn.prepareStatement(sql);
                            ps.setString(1, trainNum);
                            ps.setString(2, routes[i]);
                            ps.setString(3, routes[j]);
                            ps.setString(4, seatTypes[k]);
                            ps.setDouble(5, (j - i) * 40 + seatBasePrice[k]);
                            ps.setDate(6, today);
                            ps.setInt(7, 100);
                            ps.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDb();
        }
    }

    public static void initPassBy() {
        setUpConnection();
        String sql = "select id,departure,destination,route from route,ticket_info " +
                "where route.trainNum=ticket_info.trainNum;";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String departure = resultSet.getString(2);
                int id = resultSet.getInt(1);
                String destination = resultSet.getString(3);
                String route = resultSet.getString(4);
                String[] cities = route.split("-");
                int startIndex = 0;
                int endIndex = 0;
                for (int i = 0; i < cities.length; i++) {
                    if (cities[i].equals(departure)) {
                        startIndex = i;
                    }
                    if (cities[i].equals(destination)) {
                        endIndex = i;
                        break;
                    }
                }
                for (int i = startIndex + 1; i < endIndex; i++) {
                    sql = "insert into pass_by(ticketId,city) values(?,?);";
                    ps = conn.prepareStatement(sql);
                    ps.setInt(1, id);
                    ps.setString(2, cities[i]);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDb();
        }
    }
//0.054s
    public static void checkRemain(String departure, String destination, String seatType) {
        setUpConnection();
        String sql = "select remain,trainNum from ticket_info where departure = ? and destination = ? " +
                "AND seatType=?;";
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1, departure);
            ps.setString(2, destination);
            ps.setString(3, seatType);
            long start = System.currentTimeMillis();
            ResultSet resultSet = ps.executeQuery();
            long end = System.currentTimeMillis();
            while (resultSet.next()) {
                System.out.println(resultSet.getString(2) + ":" + resultSet.getInt(1));
            }
            System.out.println("历经时间：" + (end - start) / 1000.0 + "s");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDb();
        }
    }
//0.044s
    public static void buyTicket(String trainNum, String departure, String destination, String seatType) {
        setUpConnection();
        int startCarriage = getCarrigeNum(seatType);
        PreparedStatement ps = null;
        try {
            conn.setAutoCommit(false);
            String sql = "update ticket_info set remain = if(remain>0,remain-1,remain) where trainNum=? and seatType=? and (departure=? or destination=?);";
            ps = conn.prepareStatement(sql);
            ps.setString(1, trainNum);
            ps.setString(2, seatType);
            ps.setString(3, departure);
            ps.setString(4, destination);
            long start = System.currentTimeMillis();
            ps.executeUpdate();
            long end = System.currentTimeMillis();
            sql = "select remain from ticket_info where trainNum=? AND departure=? AND destination=? AND seatType=?;";
            ps = conn.prepareStatement(sql);
            ps.setString(1, trainNum);
            ps.setString(2, departure);
            ps.setString(3, destination);
            ps.setString(4, seatType);
            ResultSet result = ps.executeQuery();
            int remain = 0;
            if (result.next()) {
                remain = result.getInt(1);
            }
            System.out.println("-------------------------------");
            System.out.println("车次: " + trainNum);
            System.out.println("出发地：" + departure);
            System.out.println("到达地：" + destination);
            System.out.println("席别：" + seatType);
            int carriageNum = (100 - remain) / 20 + startCarriage;
            System.out.println("车厢号:" + carriageNum);
            int seatNum = (100 - remain) % 20;
            System.out.println("座位号：" + seatNum);
            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("-------------------------------");
            System.out.println("历经时间：" + (end - start) / 1000.0 + "s");
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            closeDb();
        }
    }

    private static int getCarrigeNum(String seatType) {
        for (int i = 0; i < seatTypes.length; i++) {
            if (seatType.equals(seatTypes[i])) {
                return carriage[i];
            }
        }
        return -1;
    }

    private static void closeDb() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
