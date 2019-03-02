import java.io.FileInputStream;
import java.nio.charset.Charset;


/**
 * 1257 specifics:
 * 0x20 = SPC (30)
 * 0x2D = '-' (45)
 * 0x41..0x5A = A..Z (65..90)
 * 0x61..0x7A = a..z (97..122)
 * 0xC0..0xDE = ARGH! (192..222)
 * 0xE0..0xFE = argh! (224..254)
 */
public class AnagramApp {

  static FileInputStream fis;
  static byte[] buff = new byte[2048000];
  static Charset cs = Charset.forName("Windows-1257");
  static StringBuilder sb = new StringBuilder(10000);

  static char[] lowerCaseMap = new char[256];
  static final char TO_LOWER_CASE = 32;

  static {
    for (char c = 0; c < 256; c++) {
      if ('A' <= c && 'Z' >= c) lowerCaseMap[c] = (char)(c + TO_LOWER_CASE);
      else if(0xC0 <= c && 0xDE >= c) lowerCaseMap[c] = (char)(c + TO_LOWER_CASE);
      else lowerCaseMap[c] = c;
    }
  }

  /**
   * for each byte from file --> there is char (at corresponding place)
   */
  static char mappedFromByte(byte b) {
    char c = 0 < b
            ? (char) b
            : (char) (256 + b);
    return lowerCaseMap[c];
  }

  /**
   * for each letter there is a count at specific letter-index
   * (array is bigger than needed),
   * but there are ' ', '-', umlaude chars, etc.
   */
  static byte[] argsWordLetterCounts = new byte[256];
  static byte[] currentWordLetterCounts = new byte[256];

//  static String vaeHulk() {
//    char[] chars = {'v', 0xE4, 'e', 'h', 'u', 'l', 'k'};
//    return new String(chars);
//  }
//
//  static long copyArrayTimer = 0;
//  static long copyArrayCount = 0;

  public static void main(String[] args) throws Exception {
    long startNanoTime = System.nanoTime();

    fis = new FileInputStream(args[0]);
    Decider decider = new Decider(args[1]);

    int startOfWord = 0;
    int size = fis.read(buff);
    for (int i = 0; i < size; i++) {
      if (10 != buff[i] && 13 != buff[i]) {
        decider.addByteForCurrentWord(buff[i]);
      } else {
        if (decider.isAnAnagram()) {
          addWordToResult(buff, startOfWord, i);
        }
        decider.clear();
        startOfWord = i+1;
      }
    }

    String result = sb.toString();

    double duration = (System.nanoTime() - startNanoTime) / 1000;
    System.out.println(duration + result);
//    System.out.println("copy-array: " + copyArrayTimer / 1000 + " times: " + copyArrayCount);
  }

  static void addWordToResult(byte[] buff, int offset, int nextCharIndex) {
    sb.append(',');
    sb.append(new String(buff, offset, nextCharIndex-offset, cs));
  }

  static class Decider {
    final String word;
    final int wordLength;

    boolean alreadyFailed;
    int currentWordExtraLettersCount;
    char[] currentWordBuffer;

    public Decider(final String word) {
      this.word = word;
      this.wordLength = word.length();
      byte[] wordBytes = word.getBytes(cs);
      currentWordBuffer = new char[wordLength];

      for (int i = 0; i < wordLength; i++) {
        argsWordLetterCounts[
                lowerCaseMap[
                        mappedFromByte(wordBytes[i])
                        ]
                ]++;
      }

      clear();
    }

    void clear() {
      alreadyFailed = false;
      currentWordExtraLettersCount = wordLength;
    }

    void addByteForCurrentWord(byte b) {
      if (alreadyFailed) return;
      if (0 == currentWordExtraLettersCount) { alreadyFailed=true; return; }

      char c = mappedFromByte(b);
      if (0 == argsWordLetterCounts[c]) { alreadyFailed = true; return; }

      currentWordBuffer[wordLength-currentWordExtraLettersCount]=c;
      currentWordExtraLettersCount--;
    }

    public boolean isAnAnagram() {
      if (alreadyFailed || 0 != currentWordExtraLettersCount) return false;

      System.arraycopy(argsWordLetterCounts, 0, currentWordLetterCounts, 0, 256);
      char c;
      for (int i = 0; i < wordLength; i++) {
        c = currentWordBuffer[i];
        if (0 == currentWordLetterCounts[c]) return false;
        currentWordLetterCounts[c]--;
      }

      return true;
    }
  }

}
