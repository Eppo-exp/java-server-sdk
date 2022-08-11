package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.ShardRange;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Shard Class
 */
public class Shard {
    /**
     * This function is used to get shared value
     * @param input
     * @param maxShardValue
     * @return
     * @throws Exception
     */
    public static int getShard(String input, int maxShardValue) throws Exception {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashText = no.toString(16);
            while (hashText.length() < 32) {
                hashText = "0" + hashText;
            }
            return (int)(Long.parseLong(hashText.substring(0, 8), 16) % maxShardValue);
    }

    /**
     * This function is used to check if shard is in range or not
     * @param shard
     * @param range
     * @return
     */
    public static boolean isShardInRange(int shard, ShardRange range) {
        return shard >= range.start && shard < range.end;
    }
}
