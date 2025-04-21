import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.function.IntPredicate;


// используется только для парсинга входного файла
public class MyScanner {
    private final int BUFFER_SIZE = 1024;
    private final String sep = System.lineSeparator();
    private final Reader reader;
    private char[] buffer;
    private String currentToken;
    private int currentIndex;
    private int bytesInCurrentBuffer;

    public MyScanner(String file, Charset currentCharset) throws IOException, NoSuchElementException {
        this.reader = new InputStreamReader(new FileInputStream(file), currentCharset);
        this.buffer = new char[BUFFER_SIZE];
        this.currentIndex = 0;
        this.bytesInCurrentBuffer = reader.read(buffer);;
        fillBuffer();
    }

    public MyScanner() throws IOException, NoSuchElementException {
        this.reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        this.buffer = new char[BUFFER_SIZE];
        this.currentIndex = 0;
        this.bytesInCurrentBuffer = reader.read(buffer);;
        fillBuffer();
    }

    private void fillBuffer() throws IOException {
        if (currentIndex >= BUFFER_SIZE) {
            bytesInCurrentBuffer = reader.read(buffer);
            currentIndex = 0;
        }
    }

    public boolean isLineSeparator() throws IOException {
        for (int i = 0; i < sep.length(); i++) {
            if (buffer[currentIndex] != sep.charAt(i)) {
                return false;
            }
            currentIndex++;
            fillBuffer();
        }
        return true;
    }

    public boolean hasNext() throws IOException {
        fillBuffer();
        if (bytesInCurrentBuffer < 1) {
            return false;
        }
        return currentIndex < bytesInCurrentBuffer;
    }

    public String nextToken(IntPredicate isLegalSymbol) throws IOException {
        StringBuilder tokenBuilder = new StringBuilder();
        while (hasNext()) {
            while (isLegalSymbol.test(buffer[currentIndex])) {
                tokenBuilder.append(buffer[currentIndex]);
                currentIndex++;
                fillBuffer();
            }
            while (!isLegalSymbol.test(buffer[currentIndex]) & currentIndex < bytesInCurrentBuffer) {
                currentIndex++;
                fillBuffer();
            }
            if (buffer[currentIndex] == sep.charAt(0)) {
                currentToken = tokenBuilder.toString();
                return currentToken;
            }
            if (!tokenBuilder.isEmpty()) {
                return tokenBuilder.toString();
            }
        }
        return null;
    }

    public void close() throws IOException {
        reader.close();
    }
}

