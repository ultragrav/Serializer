# Serializer
A simple but versatile serialization system to convert objects and primitives into byte arrays or files.

## Installation
### Maven:
```xml
<repository>
    <id>UltraGrav</id>
    <url>https://mvn.ultragrav.net</url>
</repository>

<dependency>
    <groupId>net.ultragrav</groupId>
    <artifactId>Serializer</artifactId>
    <version>1.2.1</version>
</dependency>
```

## Examples
```java
import net.ultragrav.serializer.*;
import java.io.FileOutputStream;

class Examples {
    public void writeToFileExample() {
        GravSerializer serializer = new GravSerializer();
        serializer.writeString("Hi!");
        serializer.writeInt(5);
        
        try {
            File out = new File("output.serialized");
            if (!out.exists()) {
                out.createNewFile();
            }
    
            serializer.writeToStream(new FileOutputStream(out));

            File out2 = new File("output-compressed.serialized");
            if (!out2.exists()) {
                out2.createNewFile();
            }

            serializer.writeToStream(new FileOutputStream(out2), <Compressor>);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    public void readFromFileExample() {
        try {
            File in = new File("output.serialized");
            if (!in.exists()) {
                return;
            }

            File in2 = new File("output-compressed.serialized");
            if (!out2.exists()) {
                return;
            }
            
            GravSerializer serializer = new GravSerializer(in); // Not compressed
            GravSerializer serializer2 = new GravSerializer(in2, true); // Compressed

            System.out.println("Serializer 1: " + serializer.readString() + ", " + serializer.readInt());
            System.out.println("Serializer 2: " + serializer2.readString() + ", " + serializer2.readInt());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
```
