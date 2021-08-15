package net.ultragrav.serializer.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.ultragrav.serializer.JsonMeta;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
    @Getter
    @AllArgsConstructor
    private static class JsonPair<T> {
        String key;
        T value;
    }

    public static JsonMeta readJson(String str) {
        return (JsonMeta) new JsonReader(str).read();
    }

    private static class JsonReader {
        CharacterIterator it;

        public JsonReader(String str) {
            this.it = new StringCharacterIterator(str);
        }

        private Object read() {
            char ch = currNWSP();

            if (ch == '{') {
                return readObject();
            }
            if (ch == '[') {
                return readArray();
            }

            if (ch == '"') {
                return readString();
            }
            if (Character.isDigit(ch) || ch == '-') {
                return readNumber();
            }

            if (ch == 't') { // Can only be true in valid JSON
                it.next();
                it.next();
                it.next();
                it.next();

                return true;
            }
            if (ch == 'f') { // Can only be false in valid JSON
                it.next();
                it.next();
                it.next();
                it.next();
                it.next();

                return false;
            }
            if (ch == 'n') { // Can only be null in valid JSON
                it.next();
                it.next();
                it.next();
                it.next();

                return null;
            }
            throw new IllegalArgumentException("Invalid JSON format at index: " + it.getIndex() + ", current token: " + ch);
        }

        private JsonMeta readObject() {
            JsonMeta meta = new JsonMeta();

            if (currNWSP() != '{') throw new IllegalArgumentException("Invalid Object at index: " + it.getIndex());

            char ch = nextNWSP();
            if (ch == '}') {
                it.next();
                return meta;
            }

            while (true) {
                String key = readString();

                ch = currNWSP();
                if (ch != ':')
                    throw new IllegalArgumentException("Invalid JSON format, missing ':' at index: " + it.getIndex());
                ch = nextNWSP();
                Object value = read();

                meta.set(key, value);

                ch = currNWSP();
                if (ch == '}') {
                    it.next();
                    return meta;
                }
                if (ch != ',')
                    throw new IllegalArgumentException("Invalid JSON format, missing ',' at index: " + it.getIndex());
                it.next();
            }
        }

        private List<Object> readArray() {
            List<Object> objs = new ArrayList<>();

            if (currNWSP() != '[') throw new IllegalArgumentException("Invalid Array at index: " + it.getIndex());

            char ch = nextNWSP();
            if (ch == ']') {
                it.next();
                return objs;
            }

            while (true) {
                Object obj = read();
                objs.add(obj);

                ch = currNWSP();
                if (ch == ']') {
                    it.next();
                    return objs;
                }
                if (ch != ',')
                    throw new IllegalArgumentException("Invalid JSON format, missing ',' at index: " + it.getIndex());
                it.next();
            }
        }

        private String readString() {
            if (currNWSP() != '"') {
                return readStringShort();
            }

            StringBuilder builder = new StringBuilder();

            boolean escaped = false;
            while (true) {
                char ch = it.next();

                if (escaped) {
                    switch (ch) {
                        case '"':
                        case '\\':
                        case '/':
                            break; // No change
                        case 'b':
                            ch = '\b';
                            break;
                        case 'f':
                            ch = '\f';
                            break;
                        case 'n':
                            ch = '\n';
                            break;
                        case 'r':
                            ch = '\r';
                            break;
                        case 't':
                            ch = '\t';
                            break;
                        case 'u':
                            int unicode = 0;
                            for (int i = 3; i >= 0; i--) {
                                unicode |= hexValue(it.next()) << (i << 2);
                            }
                            ch = (char) unicode;
                            break;
                    }
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') break;
                builder.append(ch);
            }
            it.next();
            return builder.toString();
        }

        private String readStringShort() {
            StringBuilder builder = new StringBuilder();
            it.previous();
            while (true) {
                char ch = it.next();
                if (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_') break;
                builder.append(ch);
            }
            return builder.toString();
        }

        private Object readNumber() {
            StringBuilder builder = new StringBuilder();

            char ch = currNWSP();
            while (true) {
                if (ch != '-' && !Character.isDigit(ch) && ch != 'e' && ch != 'E' && ch != '+' && ch != '.') {
                    break;
                }
                builder.append(ch);
                ch = it.next();
            }

            String str = builder.toString();
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
            }
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid JSON number");
            }
        }

        private char currNWSP() {
            if (!isWhitespace(it.current())) return it.current();
            return nextNWSP();
        }

        private char nextNWSP() {
            char ch = it.next();
            while (isWhitespace(ch)) ch = it.next();
            return ch;
        }
    }

    private static boolean isWhitespace(char ch) {
        return ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t';
    }

    private static boolean isHex(char ch) {
        return Character.isDigit(ch) || ch == 'a' || ch == 'b' || ch == 'c' || ch == 'd' || ch == 'e' || ch == 'f'
                || ch == 'A' || ch == 'B' || ch == 'C' || ch == 'D' || ch == 'E' || ch == 'F';
    }

    private static int hexValue(char ch) {
        int i = (int) ch;
        if (i < 48) throw new IllegalArgumentException("Invalid hex character");

        if (i < 58) {
            return i - 48;
        }

        if (i > 96) i -= 32;

        if (i < 65 || i > 70) throw new IllegalArgumentException("Invalid hex character");

        return i - 55;
    }
}
