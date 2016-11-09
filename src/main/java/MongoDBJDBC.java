import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tjDu on 2016/11/8.
 */
public class MongoDBJDBC {
    private static MongoDatabase mongoDatabase;
    private static String[] seatTypes = {"商务座", "一等座", "二等座", "无座"};
    private static double[] seatBasePrice = {100, 50, 0, 0};
    private static int[] carriage = {0, 5, 10, 15};

    private static void connectDb() {
        try {
            MongoClient mongoClient = new MongoClient("localhost", 27017);
            mongoDatabase = mongoClient.getDatabase("my12306");
            System.out.println("Connect to database successfully");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * remain->卖出票量
     */
    public static void insertDoc() {
        try {
            connectDb();
            MongoCollection<Document> collection = mongoDatabase.getCollection("ticket_info");
            List<String> routes = IOHelper.readFile();
            List<Document> documents = new ArrayList<Document>();
            for (String str : routes) {
                String[] data = str.split(" ");
                String[] route = data[1].split("-");
                for (int i = 0; i < route.length; i++) {
                    for (int j = i + 1; j < route.length; j++) {
                        for (int k = 0; k < seatTypes.length; k++) {
                            Document document = new Document("date", "2016-11-8").
                                    append("departure", route[i]).
                                    append("destination", route[j]).
                                    append("seatType", seatTypes[k]).
                                    append("price", (j - i) * 40 + seatBasePrice[k]).
                                    append("trainNum", data[0]).
                                    append("remain", 100);
                            documents.add(document);
                        }
                    }
                }

            }
            collection.insertMany(documents);
            System.out.println("文档插入成功");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    //0.007s
    public static void checkRemain(String departure, String destination, String seatType) {
        connectDb();
        MongoCollection<Document> collection = mongoDatabase.getCollection("ticket_info");
        BasicDBObject object1 = new BasicDBObject("departure", departure);
        object1.put("destination", destination);
        object1.put("seatType", seatType);
        long start = System.currentTimeMillis();
        FindIterable<Document> findIterable = collection.find(object1);
        long end = System.currentTimeMillis();
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        while (mongoCursor.hasNext()) {
            Document d = mongoCursor.next();
            int remain = 100 - (Integer) d.get("remain");
            System.out.println(d.get("trainNum") + " " + remain);
        }
        System.out.println("历经时间：" + (end - start) / 1000.0 + "s");
    }

    //0.165s
    public static void buyTicket(String trainNum, String departure, String destination, String seatType) {
        MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", 27017));
        DB db = mongoClient.getDB("my12306");
        DBCollection collection = db.getCollection("ticket_info");
        BasicDBObject updateQuery = new BasicDBObject();
        updateQuery.append("$inc",
                new BasicDBObject().append("remain", 1));
        DBObject clause1 = new BasicDBObject("departure", departure);
        DBObject clause2 = new BasicDBObject("destination", destination);
        BasicDBList or = new BasicDBList();
        or.add(clause1);
        or.add(clause2);
        DBObject orquery = new BasicDBObject("$or", or);
        BasicDBList and = new BasicDBList();
        and.add(new BasicDBObject("trainNum", trainNum));
        and.add(new BasicDBObject("seatType", seatType));
        and.add(orquery);
        BasicDBObject searchQuery = new BasicDBObject("$and", and);
        long start = System.currentTimeMillis();
        collection.updateMulti(searchQuery, updateQuery);
        long end = System.currentTimeMillis();
        //打印车票
        BasicDBObject object1 = new BasicDBObject("departure", departure);
        object1.put("destination", destination);
        object1.put("seatType", seatType);
        object1.put("trainNum", trainNum);
        DBCursor cursor = collection.find(object1);
        if (cursor.hasNext()) {
            int startCarriage = getCarrigeNum(seatType);
            DBObject o = cursor.next();
            int remain = 100 - (Integer) o.get("remain");
            System.out.println("-------------------------------");
            System.out.println("车次: " + trainNum);
            System.out.println("出发地：" + departure);
            System.out.println("到达地：" + destination);
            System.out.println("席别：" + seatType);
            int carriageNum = (100 - remain) / 20 + startCarriage;
            System.out.println("车厢号:" + carriageNum);
            int seatNum = remain % 20;
            System.out.println("座位号：" + seatNum);
            System.out.println("-------------------------------");
        }
        System.out.println("历经时间：" + (end - start) / 1000.0 + "s");
    }

    private static int getCarrigeNum(String seatType) {
        for (int i = 0; i < seatTypes.length; i++) {
            if (seatType.equals(seatTypes[i])) {
                return carriage[i];
            }
        }
        return -1;
    }
}
