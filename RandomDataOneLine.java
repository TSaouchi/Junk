import java.util.Random;

public class RandomDataOneLine {
    public static void main(String[] args) {
        Random random = new Random();

        int randomInt = random.nextInt(100);                                // 0–99
        double randomDouble = random.nextDouble();                          // 0.0–1.0
        boolean randomBool = random.nextBoolean();                          // true/false
        String randomString = random.ints(10, 0, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length())
                                     .mapToObj(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i))
                                     .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                                     .toString();                           // random length=10 string

        System.out.println("Int: " + randomInt);
        System.out.println("Double: " + randomDouble);
        System.out.println("Boolean: " + randomBool);
        System.out.println("String: " + randomString);
    }
}
