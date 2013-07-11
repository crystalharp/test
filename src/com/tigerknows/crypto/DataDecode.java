package com.tigerknows.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataDecode {
    static private class Element {
        int prefix;

        int suffix;

        public Element(int prefix, int suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }

    final static int MAX_CODES = 4096;

    final static int BYTE_SIZE = 8;

    final static int EXCESS = 4;

    final static int ALPHA = 256;

    final static int MASK = 15;

    static int[] s;

    static int size;

    static Element[] h;

    static int leftOver;

    static boolean bitsLeftOver;

    static InputStream in;

    static OutputStream out;

    private static int trans(int sor) {
        return (((((sor & 15) << 4) ^ sor) << 24) >>> 24);
    }

    /*
     * private static void setFiles(String args) throws IOException { String inputFile,
     * outputFile;
     * 
     * inputFile = "MoksInUnitTest\\" + args + ".mir"; if (!inputFile.endsWith(".mir")) {
     * System.out.println("The filename must end with \"mir\" extension"); System.exit(1); }
     * in = new FileInputStream(inputFile); outputFile = inputFile.substring(0,
     * inputFile.length() - 4); out = new FileOutputStream(outputFile + ".dec");
     *  }
     */

    private static void output(int code) throws IOException {
        size = -1;
        while (code >= ALPHA) {
            s[++size] = h[code].suffix;
            code = h[code].prefix;
        }
        s[++size] = code;
        for (int i = size; i >= 0; i--)
            out.write(s[i]);
    }

    private static int getCode() throws IOException {
        int c = in.read();
        if (c == -1)
            return -1;
        c = trans(c);

        int code;
        if (bitsLeftOver)
            code = (leftOver << BYTE_SIZE) + c;
        else {
            int d = in.read();
            if (d == -1)
                return -1;
            d = trans(d);

            code = (c << EXCESS) + (d >> EXCESS);
            leftOver = d & MASK;
        }
        bitsLeftOver = !bitsLeftOver;
        return code;
    }

    public static synchronized void decode(InputStream input, OutputStream output) throws IOException {
        int codeUsed = ALPHA;
        s = new int[MAX_CODES];
        h = new Element[MAX_CODES];
        in = input;
        out = output;
        bitsLeftOver = false;
        leftOver = 0;
        int pcode = getCode(), ccode;
        if (pcode >= 0) {
            s[0] = pcode;
            out.write(s[0]);
            size = 0;

            do {
                ccode = getCode();
                if (ccode < 0)
                    break;
                if (ccode < codeUsed) {
                    output(ccode);
                    if (codeUsed < MAX_CODES)
                        h[codeUsed++] = new Element(pcode, s[size]);
                } else {
                    h[codeUsed++] = new Element(pcode, s[size]);
                    output(ccode);
                }
                pcode = ccode;
            } while (true);
        }

    }

    public static byte[] decode(byte[] input, int start) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        decode(new ByteArrayInputStream(input, start, input.length - start), bOut);
        return bOut.toByteArray();
    }

    /*    public static void main(String[] args) throws IOException {
     setFiles("poi_mok");
     decode(in, out);
     out.close();
     in.close();
     setFiles("testtext");
     decode(in, out);
     in.close();
     out.close();
     }*/
}
