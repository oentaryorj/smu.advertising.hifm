package larc.recommender.parser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class ParserUtils {
	public static int countRows(String file) throws IOException {
	    BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
        byte[] c = new byte[1024];
        int count = 0;
        int readChars = 0;
        boolean empty = true;
        while ((readChars = is.read(c)) != -1) {        
            for (int i = 0; i < readChars; ++i) {
                if (c[i] == '\n') {
                	++count;
                }
            }
            empty = false;
        }
        is.close();
        return (count == 0 && !empty) ? 1 : count;
	}
}
