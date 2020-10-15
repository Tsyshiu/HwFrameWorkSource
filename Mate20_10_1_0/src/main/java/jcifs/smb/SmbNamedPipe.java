package jcifs.smb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class SmbNamedPipe extends SmbFile {
    public static final int PIPE_TYPE_CALL = 256;
    public static final int PIPE_TYPE_DCE_TRANSACT = 1536;
    public static final int PIPE_TYPE_RDONLY = 1;
    public static final int PIPE_TYPE_RDWR = 3;
    public static final int PIPE_TYPE_TRANSACT = 512;
    public static final int PIPE_TYPE_WRONLY = 2;
    InputStream pipeIn;
    OutputStream pipeOut;
    int pipeType;

    public SmbNamedPipe(String url, int pipeType2) throws MalformedURLException, UnknownHostException {
        super(url);
        this.pipeType = pipeType2;
        this.type = 16;
    }

    public SmbNamedPipe(String url, int pipeType2, NtlmPasswordAuthentication auth) throws MalformedURLException, UnknownHostException {
        super(url, auth);
        this.pipeType = pipeType2;
        this.type = 16;
    }

    public SmbNamedPipe(URL url, int pipeType2, NtlmPasswordAuthentication auth) throws MalformedURLException, UnknownHostException {
        super(url, auth);
        this.pipeType = pipeType2;
        this.type = 16;
    }

    public InputStream getNamedPipeInputStream() throws IOException {
        if (this.pipeIn == null) {
            if ((this.pipeType & 256) == 256 || (this.pipeType & 512) == 512) {
                this.pipeIn = new TransactNamedPipeInputStream(this);
            } else {
                this.pipeIn = new SmbFileInputStream(this, (this.pipeType & -65281) | 32);
            }
        }
        return this.pipeIn;
    }

    public OutputStream getNamedPipeOutputStream() throws IOException {
        if (this.pipeOut == null) {
            if ((this.pipeType & 256) == 256 || (this.pipeType & 512) == 512) {
                this.pipeOut = new TransactNamedPipeOutputStream(this);
            } else {
                this.pipeOut = new SmbFileOutputStream(this, false, (this.pipeType & -65281) | 32);
            }
        }
        return this.pipeOut;
    }
}
