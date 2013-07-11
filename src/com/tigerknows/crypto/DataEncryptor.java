package com.tigerknows.crypto;

public class DataEncryptor {
    
    private static DataEncryptor sDataEncryptor = null;
    public static DataEncryptor getInstance() {
        if (sDataEncryptor == null) {
            DataEncryptor dataEncryptor = new DataEncryptor(true);
            sDataEncryptor = dataEncryptor;
        }
        return sDataEncryptor;
    }
    
    private byte[] key;

    public DataEncryptor() {
        this.key = defaultKey;
    }

    public DataEncryptor(byte[] k) {
        this.key = k.clone();
    }

    public DataEncryptor(boolean confus) {
        if (confus == true)
            this.key = cnfKey;
        else 
            this.key = defaultKey;
    }

    public byte[] GetKey() {
        return key;
    }

    public void SetKey(byte[] k) {
        this.key = k.clone();
    }

    public void encrypt(byte[] data, int offset, int len) {
        translate(data, offset, len);
    }

    public void decrypt(byte[] data, int offset, int len) {
        translate(data, offset, len);
    }

    public void encrypt(byte[] data, int offset) {
        translate(data, offset, data.length - offset);
    }

    public void decrypt(byte[] data, int offset) {
        translate(data, offset, data.length - offset);
    }

    public void encrypt(byte[] data) {
        translate(data, 0, data.length);
    }

    public void decrypt(byte[] data) {
        translate(data, 0, data.length);
    }

    private void translate(byte[] data, int offset, int len) {
        if (data == null)
            return;
        if (key == null)
            key = defaultKey;
        for (int i = offset; i < offset + len && i < data.length; ++i)
            data[i] = (byte) (data[i] ^ key[i % 128]);
    }

    private static byte[] confusion(byte[] key) {
        byte[] b = key.clone();
        for (int i = 0; i < key.length; i++) {
            b[i] = key[cnf[i]];
        }
        return b;
    }

    private static final byte key0 = (byte) 0xEC;

    private static final byte key1 = (byte) 0x4F;

    private static final byte key2 = (byte) 0x6D;

    private static final byte key3 = (byte) 0xC0;

    private static final byte key4 = (byte) 0xC1;

    private static final byte key5 = (byte) 0x23;

    private static final byte key6 = (byte) 0x97;

    private static final byte key7 = (byte) 0xB8;

    private static final byte key8 = (byte) 0x04;

    private static final byte key9 = (byte) 0x34;

    private static final byte key10 = (byte) 0x2E;

    private static final byte key11 = (byte) 0x86;

    private static final byte key12 = (byte) 0xFE;

    private static final byte key13 = (byte) 0x6F;

    private static final byte key14 = (byte) 0x2A;

    private static final byte key15 = (byte) 0xF8;

    private static final byte key16 = (byte) 0xEE;

    private static final byte key17 = (byte) 0xFD;

    private static final byte key18 = (byte) 0xA6;

    private static final byte key19 = (byte) 0x64;

    private static final byte key20 = (byte) 0x35;

    private static final byte key21 = (byte) 0x37;

    private static final byte key22 = (byte) 0xA9;

    private static final byte key23 = (byte) 0x7F;

    private static final byte key24 = (byte) 0x15;

    private static final byte key25 = (byte) 0x09;

    private static final byte key26 = (byte) 0xD2;

    private static final byte key27 = (byte) 0x38;

    private static final byte key28 = (byte) 0xF1;

    private static final byte key29 = (byte) 0xBB;

    private static final byte key30 = (byte) 0x8A;

    private static final byte key31 = (byte) 0x1F;

    private static final byte key32 = (byte) 0x1D;

    private static final byte key33 = (byte) 0x02;

    private static final byte key34 = (byte) 0x5E;

    private static final byte key35 = (byte) 0x28;

    private static final byte key36 = (byte) 0x8B;

    private static final byte key37 = (byte) 0x77;

    private static final byte key38 = (byte) 0x89;

    private static final byte key39 = (byte) 0x31;

    private static final byte key40 = (byte) 0x00;

    private static final byte key41 = (byte) 0x9F;

    private static final byte key42 = (byte) 0xAB;

    private static final byte key43 = (byte) 0x0D;

    private static final byte key44 = (byte) 0xE4;

    private static final byte key45 = (byte) 0x3C;

    private static final byte key46 = (byte) 0x76;

    private static final byte key47 = (byte) 0x51;

    private static final byte key48 = (byte) 0xAD;

    private static final byte key49 = (byte) 0x81;

    private static final byte key50 = (byte) 0xDF;

    private static final byte key51 = (byte) 0x29;

    private static final byte key52 = (byte) 0xEB;

    private static final byte key53 = (byte) 0xC2;

    private static final byte key54 = (byte) 0xB9;

    private static final byte key55 = (byte) 0xA4;

    private static final byte key56 = (byte) 0xD5;

    private static final byte key57 = (byte) 0x3E;

    private static final byte key58 = (byte) 0xFC;

    private static final byte key59 = (byte) 0xC6;

    private static final byte key60 = (byte) 0x4B;

    private static final byte key61 = (byte) 0xCB;

    private static final byte key62 = (byte) 0x26;

    private static final byte key63 = (byte) 0xB7;

    private static final byte key64 = (byte) 0xE7;

    private static final byte key65 = (byte) 0x39;

    private static final byte key66 = (byte) 0x33;

    private static final byte key67 = (byte) 0x70;

    private static final byte key68 = (byte) 0xE0;

    private static final byte key69 = (byte) 0x5D;

    private static final byte key70 = (byte) 0xFB;

    private static final byte key71 = (byte) 0x71;

    private static final byte key72 = (byte) 0xE6;

    private static final byte key73 = (byte) 0xCF;

    private static final byte key74 = (byte) 0x50;

    private static final byte key75 = (byte) 0x49;

    private static final byte key76 = (byte) 0x94;

    private static final byte key77 = (byte) 0xC7;

    private static final byte key78 = (byte) 0x85;

    private static final byte key79 = (byte) 0x27;

    private static final byte key80 = (byte) 0x01;

    private static final byte key81 = (byte) 0xDA;

    private static final byte key82 = (byte) 0x18;

    private static final byte key83 = (byte) 0x21;

    private static final byte key84 = (byte) 0x2C;

    private static final byte key85 = (byte) 0x7E;

    private static final byte key86 = (byte) 0x0A;

    private static final byte key87 = (byte) 0x08;

    private static final byte key88 = (byte) 0x4E;

    private static final byte key89 = (byte) 0x69;

    private static final byte key90 = (byte) 0x57;

    private static final byte key91 = (byte) 0xAC;

    private static final byte key92 = (byte) 0x67;

    private static final byte key93 = (byte) 0x25;

    private static final byte key94 = (byte) 0x62;

    private static final byte key95 = (byte) 0x84;

    private static final byte key96 = (byte) 0x87;

    private static final byte key97 = (byte) 0xA8;

    private static final byte key98 = (byte) 0xBF;

    private static final byte key99 = (byte) 0x1E;

    private static final byte key100 = (byte) 0xD8;

    private static final byte key101 = (byte) 0x5F;

    private static final byte key102 = (byte) 0x7C;

    private static final byte key103 = (byte) 0x95;

    private static final byte key104 = (byte) 0x4D;

    private static final byte key105 = (byte) 0xAA;

    private static final byte key106 = (byte) 0x59;

    private static final byte key107 = (byte) 0x5C;

    private static final byte key108 = (byte) 0x8C;

    private static final byte key109 = (byte) 0x58;

    private static final byte key110 = (byte) 0x68;

    private static final byte key111 = (byte) 0x8F;

    private static final byte key112 = (byte) 0xC5;

    private static final byte key113 = (byte) 0xB2;

    private static final byte key114 = (byte) 0x98;

    private static final byte key115 = (byte) 0xB5;

    private static final byte key116 = (byte) 0x3B;

    private static final byte key117 = (byte) 0x73;

    private static final byte key118 = (byte) 0x9E;

    private static final byte key119 = (byte) 0xC9;

    private static final byte key120 = (byte) 0x66;

    private static final byte key121 = (byte) 0x41;

    private static final byte key122 = (byte) 0xDD;

    private static final byte key123 = (byte) 0xB1;

    private static final byte key124 = (byte) 0x2D;

    private static final byte key125 = (byte) 0x0B;

    private static final byte key126 = (byte) 0xFF;

    private static final byte key127 = (byte) 0x7A;

    public static final byte[] defaultKey = {
            key0, key1, key2, key3, key4, key5, key6, key7, key8, key9, key10, key11, key12, key13,
            key14, key15, key16, key17, key18, key19, key20, key21, key22, key23, key24, key25,
            key26, key27, key28, key29, key30, key31, key32, key33, key34, key35, key36, key37,
            key38, key39, key40, key41, key42, key43, key44, key45, key46, key47, key48, key49,
            key50, key51, key52, key53, key54, key55, key56, key57, key58, key59, key60, key61,
            key62, key63, key64, key65, key66, key67, key68, key69, key70, key71, key72, key73,
            key74, key75, key76, key77, key78, key79, key80, key81, key82, key83, key84, key85,
            key86, key87, key88, key89, key90, key91, key92, key93, key94, key95, key96, key97,
            key98, key99, key100, key101, key102, key103, key104, key105, key106, key107, key108,
            key109, key110, key111, key112, key113, key114, key115, key116, key117, key118, key119,
            key120, key121, key122, key123, key124, key125, key126, key127,
    };

    public static final byte[] cnf = new byte[] {
            27, 96, 113, 41, 39, 19, 74, 95, 28, 17, 11, 101, 81, 65, 82, 76, 123, 90, 80, 119,
            100, 116, 47, 34, 85, 9, 42, 75, 110, 115, 36, 32, 43, 98, 92, 26, 7, 12, 44, 38, 122,
            102, 112, 109, 1, 108, 73, 104, 37, 97, 5, 68, 84, 83, 46, 2, 105, 63, 30, 55, 52, 86,
            79, 64, 53, 94, 120, 14, 60, 106, 22, 59, 111, 126, 0, 10, 78, 23, 58, 93, 29, 87, 21,
            91, 72, 125, 71, 13, 20, 33, 3, 88, 15, 25, 69, 56, 8, 124, 50, 114, 48, 103, 18, 66,
            54, 107, 62, 89, 57, 51, 99, 4, 67, 61, 24, 121, 6, 70, 117, 16, 45, 127, 77, 31, 35,
            118, 49, 40
    };

    public static final byte[] cnfKey = confusion(defaultKey);

}
