package com.ezekielnewren.lib;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class CircularByteBuffer implements ByteChannel {
    final Object mutex = new Object();
    byte[] buff;

    boolean open = true;
    boolean eof = false;
    int rp;
    int wp;
    int readable;

    /**
     * This constructor assumes that this class will have exclusive
     * access to this buffer and will use it as it's internal array.
     * @param _buff
     */
    public CircularByteBuffer(byte[] _buff) {
        this.buff = _buff;
    }

    public CircularByteBuffer(int size) {
        this(new byte[size]);
    }

    /**
     * even the unit test should not touch this function
     */
    private void pause() throws IOException {
        synchronized(mutex) {
            try {
                mutex.wait();
            } catch (InterruptedException e) {
                close();
                throw new ClosedByInterruptException();
            }
        }
    }

    public int readable() {
        synchronized(mutex) {
            return readable;
        }
    }

    public int writable() {
        synchronized(mutex) {
            return capacity()-readable;
        }
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        if (byteBuffer == null) throw new NullPointerException();
        if (!isOpen()) throw new ClosedChannelException();
        synchronized(mutex) {
            try {
                int read;
                while ((read=Math.min(readable(), byteBuffer.remaining())) == 0 && isOpen() && !eof) pause();
                if (!isOpen()) throw new AsynchronousCloseException();

                if (readable() == 0 && eof) return -1;
                int remaining = buff.length-rp;
                if (read <= remaining) {
                    byteBuffer.put(buff, rp, read);
                } else {
                    byteBuffer.put(buff, rp, remaining);
                    byteBuffer.put(buff, 0, read-remaining);
                }

                rp =  (rp+read)%capacity();
                readable -= read;
                return read;
            } finally {
                mutex.notifyAll();
            }
        }
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        if (byteBuffer == null) throw new NullPointerException();
        if (!isOpen()) throw new ClosedChannelException();
        synchronized(mutex) {
            try {
                int write;
                while ((write=Math.min(writable(), byteBuffer.remaining())) == 0 && isOpen() && !eof) pause();
                if (!isOpen()) throw new AsynchronousCloseException();
                if (eof) throw new NonWritableChannelException();

                int remaining = buff.length-wp;
                if (write <= remaining) {
                    byteBuffer.get(buff, wp, write);
                } else {
                    byteBuffer.get(buff, wp, remaining);
                    byteBuffer.get(buff, 0, write-remaining);
                }

                wp = (wp+write)%capacity();
                readable += write;
                return write;
            } finally {
                mutex.notifyAll();
            }
        }
    }

    public void readFully(ByteBuffer byteBuffer) throws IOException {
        synchronized(mutex) {
            while (byteBuffer.remaining() > 0) {
                if (read(byteBuffer) < 0) throw new EOFException();
            }
        }
    }

    public void writeFully(ByteBuffer byteBuffer) throws IOException {
        synchronized(mutex) {
            while (byteBuffer.remaining() > 0) {
                if (write(byteBuffer) < 0) throw new EOFException();
            }
        }
    }

    @Override
    public boolean isOpen() {
        synchronized(mutex) {
            return open;
        }
    }

    public int capacity() {
        return buff.length;
    }

    @Override
    public void close() {
        synchronized(mutex) {
            open = false;
            mutex.notifyAll();
        }
    }

    public boolean isEOF() {
        synchronized(mutex) {
            return eof;
        }
    }

    public void setEOF() {
        synchronized(mutex) {
            try {
                eof = true;
            } finally {
                mutex.notifyAll();
            }
        }
    }
}
