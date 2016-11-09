import java.util.List;

/**
 * Created by tjDu on 2016/11/7.
 */
public class Main {
    public static void main(String[] args) {
       // MysqlDBJDBC.buyTicket("G101", "北京南", "沧州西", "商务座");
         MongoDBJDBC.buyTicket("G101", "北京南", "上海虹桥", "一等座");
          //MongoDBJDBC.checkRemain("北京南", "上海虹桥", "一等座");
       // MongoDBJDBC.insertDoc();
       // MysqlDBJDBC.checkRemain("北京南","上海虹桥","一等座");
    }

}
