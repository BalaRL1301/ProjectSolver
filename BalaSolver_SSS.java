import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
                // With k points, we define a candidate polynomial.
                // Now we check all n shares against this polynomial.
                
                List<Share> consistentShares = new ArrayList<>();
                List<Share> inconsistentShares = new ArrayList<>();

                // *** REVISED AND CORRECTED VERIFICATION LOGIC ***
                for (Share shareToCheck : allShares) {
                    try {
                        BigInteger expectedY = evaluatePolynomial(currentCombination, shareToCheck.x);
                        if (expectedY.equals(shareToCheck.y)) {
                            consistentShares.add(shareToCheck);
                        } else {
                            inconsistentShares.add(shareToCheck);
                        }
                    } catch (ArithmeticException e) {
                        // This will happen if a point in currentCombination has the same x as shareToCheck
                        // This is fine, we just need to handle it. If they are the same point, they are consistent.
                        boolean foundMatch = false;
                        for(Share comboShare : currentCombination) {
                            if (comboShare.x.equals(shareToCheck.x) && comboShare.y.equals(shareToCheck.y)) {
                                consistentShares.add(shareToCheck);
                                foundMatch = true;
                                break;
                            }
                        }
                        if (!foundMatch) {
                           inconsistentShares.add(shareToCheck);
                        }
                    }
                }
                // *** END OF REVISED LOGIC ***

                if (consistentShares.size() == n - 1) {
                    System.out.println("\n----------------- SOLUTION FOUND -----------------");
                    System.out.println("SECRET (Constant Value): " + reconstructSecret(currentCombination));
                    System.out.println("WRONG SHARE: " + inconsistentShares.get(0));
                    System.out.println("--------------------------------------------------");
                    return;
                }

                if (consistentShares.size() == n) {
                    System.out.println("\n----------------- SOLUTION FOUND -----------------");
                    System.out.println("All " + n + " shares are correct and consistent.");
                    System.out.println("SECRET (Constant Value): " + reconstructSecret(currentCombination));
                    System.out.println("--------------------------------------------------");
                    return;
                }
            }
            
            System.out.println("Could not find a solution.");

        } catch (IOException e) {
            System.err.println("Error reading the file: " + filePath);
            e.printStackTrace();
        }
    }
    
    /**
     * Reconstructs the secret (the polynomial's value at x=0).
     */
    public static BigInteger reconstructSecret(List<Share> shares) {
        return evaluatePolynomial(shares, BigInteger.ZERO);
    }

    /**
     * Evaluates the polynomial defined by the given points at a specific x-value using Lagrange Interpolation.
     * P(x) = SUM { y_j * L_j(x) }
     * L_j(x) = PRODUCT { (x - x_m) / (x_j - x_m) } for m != j
     */
    public static BigInteger evaluatePolynomial(List<Share> points, BigInteger xValue) {
        int k = points.size();
        BigInteger result = BigInteger.ZERO;

        for (int j = 0; j < k; j++) {
            Share currentPoint = points.get(j);
            BigInteger xj = currentPoint.x;
            BigInteger yj = currentPoint.y;

            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int m = 0; m < k; m++) {
                if (j == m) {
                    continue;
                }
                BigInteger xm = points.get(m).x;
                
                numerator = numerator.multiply(xValue.subtract(xm));
                denominator = denominator.multiply(xj.subtract(xm));
            }
            
            BigInteger term = yj.multiply(numerator).divide(denominator);
            result = result.add(term);
        }
        return result;
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