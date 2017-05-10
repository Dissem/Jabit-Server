/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.bitmessage.server;

import ch.dissem.bitmessage.entity.BitmessageAddress;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

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

    public static String qrCode(BitmessageAddress address) {
        StringBuilder link = new StringBuilder();
        link.append("bitmessage:");
        link.append(address.getAddress());
        if (address.getAlias() != null) {
            link.append("?label=").append(address.getAlias());
        }
        if (address.getPubkey() != null) {
            link.append(address.getAlias() == null ? '?' : '&');
            ByteArrayOutputStream pubkey = new ByteArrayOutputStream();
            try {
                address.getPubkey().writeUnencrypted(pubkey);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // This makes the QR code quite big, so it's not active. But sometimes it might be useful:
            // link.append("pubkey=").append(Base64.getUrlEncoder().encodeToString(pubkey.toByteArray()));
        }
        QRCode code;
        try {
            code = Encoder.encode(link.toString(), ErrorCorrectionLevel.L, null);
        } catch (WriterException e) {
            LOG.error(e.getMessage(), e);
            return "";
        }
        ByteMatrix matrix = code.getMatrix();
        StringBuilder result = new StringBuilder();
        for (int i=0; i<2; i++){
            for (int j=0;j<matrix.getWidth()+8; j++){
                result.append('█');
            }
            result.append('\n');
        }
        for (int i = 0; i < matrix.getHeight(); i += 2) {
            result.append("████");
            for (int j = 0; j < matrix.getWidth(); j++) {
                if (matrix.get(i, j) > 0) {
                    if (matrix.getHeight() > i + 1 && matrix.get(i + 1, j) > 0) {
                        result.append(' ');
                    } else {
                        result.append('▄');
                    }
                } else {
                    if (matrix.getHeight() > i + 1 && matrix.get(i + 1, j) > 0) {
                        result.append('▀');
                    } else {
                        result.append('█');
                    }
                }
            }
            result.append("████\n");
        }
        for (int i=0; i<2; i++){
            for (int j=0;j<matrix.getWidth()+8; j++){
                result.append('█');
            }
            result.append('\n');
        }
        return result.toString();
    }

    public static boolean zero(byte[] nonce) {
        for (byte b : nonce) {
            if (b != 0) return false;
        }
        return true;
    }
}
