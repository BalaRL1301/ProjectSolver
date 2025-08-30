import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BalaSolver_SSS {
    public static class Share {
        public final BigInteger x;
        public final BigInteger y;

        public Share(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Share(x=" + x + ", y=" + y + ")";
        }
    }

    public static void main(String[] args) {
        String filePath = "input.json";
        
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
        
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
            JsonObject keys = root.getAsJsonObject("keys");
            int n = keys.get("n").getAsInt();
            int k = keys.get("k").getAsInt();

            List<Share> allShares = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getKey().equals("keys")) {
                    continue;
                }
                BigInteger x = new BigInteger(entry.getKey());
                JsonObject shareData = entry.getValue().getAsJsonObject();
                String valueStr = shareData.get("value").getAsString();
                int base = shareData.get("base").getAsInt();
                BigInteger y = new BigInteger(valueStr, base);
                allShares.add(new Share(x, y));
            }

            System.out.println("Successfully parsed " + n + " shares. Threshold k = " + k);
            System.out.println("Starting search for the correct polynomial...");

            List<List<Share>> combinations = new ArrayList<>();
            generateCombinations(allShares, k, 0, new ArrayList<>(), combinations);

            for (List<Share> currentCombination : combinations) {
                BigInteger candidateSecret = reconstructSecret(currentCombination);
                
                List<Share> consistentShares = new ArrayList<>();
                List<Share> inconsistentShares = new ArrayList<>();

                for (Share shareToCheck : allShares) {
                    List<Share> testSet = new ArrayList<>(currentCombination.subList(0, k - 1));
                    testSet.add(shareToCheck);
                    
                    try {
                        if (reconstructSecret(testSet).equals(candidateSecret)) {
                            consistentShares.add(shareToCheck);
                        } else {
                            inconsistentShares.add(shareToCheck);
                        }
                    } catch (ArithmeticException e) {
                        inconsistentShares.add(shareToCheck);
                    }
                }

                // Original check for exactly ONE wrong share
                if (consistentShares.size() == n - 1) {
                    System.out.println("\n----------------- SOLUTION FOUND -----------------");
                    System.out.println("SECRET (Constant Value): " + candidateSecret);
                    System.out.println("WRONG SHARE: " + inconsistentShares.get(0));
                    System.out.println("--------------------------------------------------");
                    return;
                }

                // ***************************************************
                // *** NEW CODE ADDED HERE ***
                // Check for the case where ALL shares are correct
                if (consistentShares.size() == n) {
                    System.out.println("\n----------------- SOLUTION FOUND -----------------");
                    System.out.println("All " + n + " shares are correct and consistent.");
                    System.out.println("SECRET (Constant Value): " + candidateSecret);
                    System.out.println("--------------------------------------------------");
                    return; // Exit after finding the solution
                }
                // ***************************************************
            }
            
            System.out.println("Could not find a solution.");

        } catch (IOException e) {
            System.err.println("Error reading the file: " + filePath);
            e.printStackTrace();
        }
    }

    public static BigInteger reconstructSecret(List<Share> shares) {
        int k = shares.size();
        BigInteger secret = BigInteger.ZERO;

        for (int j = 0; j < k; j++) {
            Share currentShare = shares.get(j);
            BigInteger xj = currentShare.x;
            BigInteger yj = currentShare.y;

            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int m = 0; m < k; m++) {
                if (j == m) {
                    continue;
                }
                BigInteger xm = shares.get(m).x;
                
                numerator = numerator.multiply(xm);
                denominator = denominator.multiply(xm.subtract(xj));
            }
            
            BigInteger term = yj.multiply(numerator).divide(denominator);
            secret = secret.add(term);
        }
        return secret;
    }

    private static void generateCombinations(List<Share> allShares, int k, int start, 
                                             List<Share> currentCombination, List<List<Share>> allCombinations) {
        if (currentCombination.size() == k) {
            allCombinations.add(new ArrayList<>(currentCombination));
            return;
        }

        if (start >= allShares.size()) {
            return;
        }

        for (int i = start; i < allShares.size(); i++) {
            currentCombination.add(allShares.get(i));
            generateCombinations(allShares, k, i + 1, currentCombination, allCombinations);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }
}