package ch.zhaw;

import java.util.*;
import java.util.stream.Collectors;

import org.bson.BsonNull;
import org.bson.Document;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import ch.qos.logback.classic.Level; 
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import com.mongodb.client.MongoCollection; 

public class DB {
    public DB(){
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger("org.mongodb.driver").setLevel(Level.OFF);
    }
    ConnectionString connectionString = new ConnectionString("mongodb+srv://kaeseno1:eXuzhJ-ZV6KUH4t@cluster0.4pnoho7.mongodb.net/?retryWrites=true&w=majority");
    MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(connectionString)
        .serverApi(ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build())
        .build();
    MongoClient mongoClient = MongoClients.create(settings);
    MongoDatabase database = mongoClient.getDatabase("Nutritional_values");

    MongoCollection<Document> col = database.getCollection("Nutritional_values");
    MongoCollection<Document> colboard = database.getCollection("board");

    ArrayList<Document> quizList = new ArrayList<>();
    int count = 0;

    public double getAverage (String type){
        ArrayList<Document> list = col.aggregate(Arrays.asList(new Document("$group", 
        new Document("_id", 
        new BsonNull())
                .append("Avg", 
        new Document("$avg", 
        new Document("$toDouble", "$"+type)))))).into(new ArrayList<Document>());

        return Math.round(list.get(0).getDouble("Avg")*100.0)/100.0;
    }

    public double getMax (String type){
        ArrayList<Document> list = col.aggregate(Arrays.asList(new Document("$group", 
        new Document("_id", 
        new BsonNull())
                .append("Max", 
        new Document("$max", 
        new Document("$toDouble", "$"+type)))))).into(new ArrayList<Document>());
        
        return list.get(0).getDouble("Max");
    }
    
    public void getThree (String type, Double t_avg){
        ArrayList<Document> above = col.aggregate(Arrays.asList(new Document("$match", 
        new Document(type, 
        new Document("$gte",t_avg))), 
        new Document("$project", 
        new Document("name", "$name")
                .append("value", 
        new Document("$toDouble","$"+type))), 
        new Document("$sample", 
        new Document("size", 2L)))).into(new ArrayList<Document>());

        ArrayList<Document> below = col.aggregate(Arrays.asList(new Document("$match", 
        new Document(type, 
        new Document("$lt", t_avg))), 
        new Document("$project", 
        new Document("name", "$name")
                .append("value", 
        new Document("$toDouble","$"+type))), 
        new Document("$sample", 
        new Document("size", 1L)))).into(new ArrayList<Document>()); 
 
        for (Document doc : above) {
            quizList.add(doc);
        }
        for (Document doc : below) {
            quizList.add(doc);
        }
        int num = 1;
        for (Document doc : quizList) {
            System.out.println(num+": "+doc.getString("name"));
            num++;
        }
    }

    public void solution (String answer){

        ArrayList<Document> corrList = quizList; //correct list
        ArrayList<Document> usrList = new ArrayList<Document>(); //user input
        int points = 0;

        String [] userinputStrg = answer.split("\\s*\\,\\s*");
        ArrayList<Integer> userinputint = new ArrayList<Integer>();

        for (String ans : userinputStrg){
        userinputint.add(Integer.parseInt(ans));
        }
        for (int i : userinputint){
            usrList.add(quizList.get(i-1));
        }     
        corrList = corrList.stream()
                .sorted((x, y) -> Double.compare(x.getDouble("value"), y.getDouble("value")))
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0;i<3;i++){
            if(corrList.get(i).getString("name").equals(usrList.get(i).getString("name"))){
                points++;
            }
        }
        if(points==3 || points==2){
                System.out.print("\nYour answer is right! You get 2 points.\n");
                points=2;
            }
            else if(points==1){
                System.out.print("\nYour answer is partially correct! You get 1 point.\n");
            }
            else{
                System.out.print("\nYour answer is not correct! You get 0 points.\n");
            }
        count+=points;
    }

    public void printValues (){
        int num = 1;

        for (Document doc : quizList) {
            System.out.println(num+": "+doc.getDouble("value"));
            num++;
        }
        quizList.clear();
    }
    
    public int getPoints (){
        return count; 
    }

    public void leader (String name, int points){
        Document person = new Document("name", name)
                   .append("points", points);

        colboard.insertOne(person);
    }

    public void compare(String username, int currentPoints) {
        Document query = new Document("name", username);
        Document sort = new Document("points", -1L);
        Document group = new Document("_id", "$name")
                .append("points", new Document("$push", "$points"));
        Document project = new Document("_id", 0L)
                .append("name", "$_id")
                .append("points", 1L);
    
        AggregateIterable<Document> iterable = colboard.aggregate(Arrays.asList(
                new Document("$match", query),
                new Document("$sort", sort),
                new Document("$group", group),
                new Document("$project", project)
        ));
        List<Document> leaderboard = new ArrayList<>();
        iterable.into(leaderboard);
    
        if (leaderboard.isEmpty()) {
            System.out.println("\n"+username+", this is your first time playing!");
        } else {
            List<Integer> points = leaderboard.get(0).get("points", List.class);
            int previousBestScore = points.get(0);
            int currentRanking = 1;
            int previousBestRanking = 1;
            boolean improvement = false;
            for (int i = 0; i < points.size(); i++) {
                int score = points.get(i);
                if (currentPoints < score) {
                    currentRanking++;
                    if (score > previousBestScore) {
                        previousBestScore = score;
                        previousBestRanking = currentRanking;
                    }
                } else if (currentPoints == score) {
                    // If the score is equal to the current score, then it is not an improvement
                    System.out.println(username + ", you scored " + currentPoints + " points. This is " + currentRanking + "th place but unfortunately not an improvement to your best score.");
                    return;
                } else {
                    improvement = true;
                    break;
                }
            }
    
            if (improvement) {
                System.out.println(username + ", you scored " + currentPoints + " points. This makes your best result new " + previousBestRanking + "nd place. Congratulations!");
            } else {
                System.out.println(username + ", you are in " + currentRanking + "th place but unfortunately not an improvement to your best score.");
            }
        }
    }
        
    
    private static String getOrdinalSuffix(int number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return "th";
        } else if (number % 10 == 1) {
            return "st";
        } else if (number % 10 == 2) {
            return "nd";
        } else if (number % 10 == 3) {
            return "rd";
        } else {
            return "th";
        }
    }
        
    public void leaderboard (){
        AggregateIterable<Document> iterable = colboard.aggregate(Arrays.asList(
            new Document("$sort", new Document("points", -1L)),
            new Document("$limit", 3L),
            new Document("$project", new Document("_id", 0L)
                    .append("name", 1L)
                    .append("points", 1L))));
    
        List<Document> leaderboard = new ArrayList<>();
        iterable.into(leaderboard);

        System.out.println("\nLeaderboard:");
        int num = 1;
        for (Document doc : leaderboard) {
            System.out.println(num+": "+doc.getString("name")+" has "+doc.getInteger("points")+" Points.");
            num++;
        }
    }    

    public void ranking() {
        AggregateIterable<Document> iterable = colboard.aggregate(Arrays.asList(
                new Document("$sort", new Document("points", -1L)),
                new Document("$group", new Document("_id", "$name")
                        .append("points", new Document("$push", "$points"))),
                new Document("$project", new Document("_id", 0L)
                        .append("name", "$_id")
                        .append("points", 1L))
        ));
        List<Document> leaderboard = new ArrayList<>();
        iterable.into(leaderboard);
    
        System.out.println("\nRanking Summary:");
        for (Document doc : leaderboard) {
            String name = doc.getString("name");
            List<Integer> points = doc.get("points", List.class);
            System.out.print(name + ": ");
            for (int i = 0; i < points.size(); i++) {
                System.out.print(points.get(i));
                if (i < points.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }
    }  
}