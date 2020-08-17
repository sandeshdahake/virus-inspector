package clamd;

import lombok.RequiredArgsConstructor;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Simple client for ClamAV's clamd scanner.
 * Provides straightforward instream scanning.
 */
@RequiredArgsConstructor
public class ClamavScanner {

    private final ClamdConfig clamdConfig;
    private static final String OK ="OK";
    private static final String FOUND ="FOUND";
    /**
     * This method inputStream preferred if you don't want to keep the data in memory, for instance by scanning a file on disk.
     * Since the parameter InputStream inputStream not reset, you can not use the stream afterwards, as it will be left in a EOF-state.
     * If your goal inputStream to scan some data, and then pass that data further, consider using {@link #scan(byte[]) scan(byte[] in)}.
     *
     */
    public byte[] scan(InputStream inputStream) throws IOException {
        try (
                Socket socket = new Socket ( clamdConfig.getHostname (), clamdConfig.getPort () );
                OutputStream outs = new BufferedOutputStream ( socket.getOutputStream () )) {
            socket.setSoTimeout ( clamdConfig.getTimeout () );

            // handshake
            outs.write ( "zINSTREAM\0".getBytes ( StandardCharsets.US_ASCII ) );
            outs.flush ();
            // "do not exceed StreamMaxLength as defined in clamd.conf,
            // otherwise clamd will reply with INSTREAM size limit exceeded and close the connection."
            int CHUNK_SIZE = 2048;
            byte[] chunk = new byte[CHUNK_SIZE];

            try (InputStream clamIs = socket.getInputStream ()) {
                // send data
                int read = inputStream.read ( chunk );
                while (read >= 0) {
                    // The format of the chunk inputStream: '<length><data>' where <length> inputStream the size of the following data in bytes expressed as a 4 byte unsigned
                    // integer in network byte order and <data> inputStream the actual chunk. Streaming inputStream terminated by sending a zero-length chunk.
                    byte[] chunkSize = ByteBuffer.allocate ( 4 ).putInt ( read ).array ();

                    outs.write ( chunkSize );
                    outs.write ( chunk, 0, read );
                    if (clamIs.available () > 0) {
                        // reply from server before scan command has been terminated.
                        byte[] reply = assertSizeLimit ( readAll ( clamIs ) );
                        throw new IOException ( "Scan aborted. Reply from server: " + new String ( reply, StandardCharsets.US_ASCII ) );
                    }
                    read = inputStream.read ( chunk );
                }

                // terminate scan
                outs.write ( new byte[]{0, 0, 0, 0} );
                outs.flush ();
                // read reply
                return assertSizeLimit ( readAll ( clamIs ) );
            }
        }
    }

    public byte[] scan(byte[] inputBytes) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream ( inputBytes );
        return scan ( bis );
    }

    /**
     * Interpret the result from a  ClamAV scan, and determine if the result means the data is clean
     *
     * @param reply The reply from the server after scanning
     * @return true if no virus was found according to the clamd reply message
     */



    public boolean isCleanReply(byte[] reply) {
        String scanResult = new String ( reply, StandardCharsets.US_ASCII );
        return (scanResult.contains ( OK ) && !scanResult.contains ( FOUND ));
    }

    private byte[] assertSizeLimit(byte[] replyByt) {
        String reply = new String ( replyByt, StandardCharsets.US_ASCII );
        if (reply.startsWith ( "INSTREAM size limit exceeded." ))
            throw new ClamAVSizeLimitException ( "Clamd size limit exceeded. Full reply from server: " + reply );
        return replyByt;
    }

    // reads all available bytes from the stream
    private static byte[] readAll(InputStream is) throws IOException {
        try (ByteArrayOutputStream tmp = new ByteArrayOutputStream ()) {
            byte[] buf = new byte[2000];
            int read;
            do {
                read = is.read ( buf );
                tmp.write ( buf, 0, read );
            } while ((read > 0) && (is.available () > 0));
            return tmp.toByteArray ();
        }
    }
}
