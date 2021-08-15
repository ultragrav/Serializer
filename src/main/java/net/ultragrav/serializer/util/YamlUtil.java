package net.ultragrav.serializer.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.ultragrav.serializer.JsonMeta;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

public class YamlUtil {
    public static JsonMeta fromYaml(String str) {
        String[] lines = str.replaceAll("\r", "").split("\n");
        return null;
    }

    private static class YamlReader {
        private String[] lines;
        private int index;

        public YamlReader(String[] strs) {
            this.lines = strs;
            this.index = 0;
        }
    }

    private static int getIndent(String str) {
        int indent = 0;
        while (str.charAt(indent) == ' ') indent++;
        return indent;
    }
}
