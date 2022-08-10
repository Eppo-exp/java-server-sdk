package helpers;

import dto.SharedRange;

import java.math.BigInteger;
import java.security.MessageDigest;

public class Shard {
    public static int getShard(String input, int subjectShards) throws Exception {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            System.out.println(hashtext.substring(0, 8));
            return (int)(Long.parseLong(hashtext.substring(0, 8), 16) % subjectShards);
    }

    public static boolean isShardInRange(int shard, SharedRange range) {
        return shard >= range.start && shard < range.end;
    }
}
