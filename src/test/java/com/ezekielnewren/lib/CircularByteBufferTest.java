package com.ezekielnewren.lib;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class CircularByteBufferTest {

    static final String alphabet = "abdefghijklmnopqrstuvwxyz";

    static byte[] getAlphabet() {
        return alphabet.getBytes(StandardCharsets.UTF_8);
    }

    @Test(expected = java.lang.NegativeArraySizeException.class)
    public void constructorFailOnNegativeSize() {
        CircularByteBuffer cbb = new CircularByteBuffer(-1);
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void constructorFailOnZeroSize() {
        CircularByteBuffer cbb = new CircularByteBuffer(0);
    }

    @Test
    public void constructorSucceedOnGoodInput() {
        CircularByteBuffer cbb = new CircularByteBuffer(8192);
    }

    @Test
    public void goodReadWrite() {
        CircularByteBuffer cbb = new CircularByteBuffer(50);

        try {
            ByteBuffer bb = ByteBuffer.allocate(1000);
            bb.put(getAlphabet());
            bb.flip();
            int write = cbb.write(bb);
            assertEquals(alphabet.length(), write);

            bb.clear();
            int read = cbb.read(bb);
            assertEquals(alphabet.length(), read);

        } catch (IOException e) {
            fail();
        }
    }






}
