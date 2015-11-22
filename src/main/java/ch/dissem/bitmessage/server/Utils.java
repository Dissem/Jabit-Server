package ch.dissem.bitmessage.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

public class Utils {
    public static Set<String> readOrCreateList(String filename, String content) {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                if (file.createNewFile()) {
                    try (FileWriter fw = new FileWriter(file)) {
                        fw.write(content);
                    }
                }
            }
            return readList(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveList(String filename, Stream<String> content) {
        try {
            File file = new File(filename);
            try (FileWriter fw = new FileWriter(file)) {
                content.forEach(l -> {
                    try {
                        fw.write(l);
                        fw.write(System.lineSeparator());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> readList(File file) {
        Set<String> result = new HashSet<>();
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("BM-")) {
                    result.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean zero(byte[] nonce) {
        for (byte b : nonce) {
            if (b != 0) return false;
        }
        return true;
    }
}
