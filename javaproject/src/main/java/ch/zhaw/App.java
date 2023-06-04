package ch.zhaw;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class App 

{
    public static void main(String[] args){

        System.out.println( "Welcome to the Food Quiz!");

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger("org.mongodb.driver").setLevel(Level.OFF);        

        DB database = new DB();
        Scanner keyScan = new Scanner(System.in);

        HashMap <String, String> values = new HashMap <String, String>();
        values.put("calories", "Calories");
        values.put("total_fat", "fat");
        values.put("protein", "protein");
        values.put("fiber", "fiber");
        values.put("sugars", "sugars");

        for(String key : values.keySet()){
            System.out.println("\nQuestion about "+values.get(key)+" (maximum: "+database.getMax(key)+", average: "+database.getAverage(key)+")");
            System.out.println("Please order the following foods by "+values.get(key)+" in ascending order:");
            
            database.getThree(key, database.getAverage(key));

            String answer = "";
            Set<String> answersSet = new HashSet<>();
            while (true){
                try{
                    answer = keyScan.nextLine();
                    String[] answers = answer.split(",");
                    if (answers.length != 3 || !answer.matches("[1-3](,(?![^,]*\\b\\1\\b)[1-3]){2}") || answersSet.containsAll(Arrays.asList(answers))) {
                        throw new Exception();
                    }
                    answersSet.addAll(Arrays.asList(answers));
                    break;
                } 
                catch (Exception e){
                    System.out.println("Invalid input. Please enter a valid answer (e.g. '1,2,3').");
                }
            }
                                                    
            database.solution(answer);
            System.out.println("\n"+values.get(key).substring(0,1).toUpperCase()+values.get(key).substring(1)+" for 100g:");
            database.printValues();
        }

        System.out.println("\nYou achieved "+database.getPoints()+" points. Congratulations! Please enter your name.");
        
        String name = "";
        while (true) {
            try {
                name = keyScan.nextLine();
                if (!name.matches("[a-zA-Z]{2,}")) {
                    throw new Exception();
                }
                break;
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a valid name (letters from the alphabet only, without ö, ä, ü, é...).");
            }
        }
        database.compare(name, database.getPoints());                     
        database.leader(name, database.getPoints());
        database.leaderboard();
        database.ranking();

        keyScan.close();
    }
}