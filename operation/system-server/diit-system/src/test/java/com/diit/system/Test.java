package com.diit.system;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SymmetricCrypto;

import java.nio.charset.StandardCharsets;

public class Test {

    public static void main(String[] args) {
        String str = "Abc@Diit!123";
        SymmetricCrypto sm4 = SmUtil.sm4("diitdiitdiitdiit".getBytes(StandardCharsets.UTF_8));
        System.out.println(sm4.encryptHex(str));
    }
}
