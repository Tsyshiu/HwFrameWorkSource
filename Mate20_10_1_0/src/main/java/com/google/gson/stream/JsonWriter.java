package com.google.gson.stream;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

public class JsonWriter implements Closeable, Flushable {
    private static final String[] HTML_SAFE_REPLACEMENT_CHARS = ((String[]) REPLACEMENT_CHARS.clone());
    private static final String[] REPLACEMENT_CHARS = new String[128];
    private String deferredName;
    private boolean htmlSafe;
    private String indent;
    private boolean lenient;
    private final Writer out;
    private String separator;
    private boolean serializeNulls;
    private int[] stack = new int[32];
    private int stackSize = 0;

    static {
        for (int i = 0; i <= 31; i++) {
            REPLACEMENT_CHARS[i] = String.format("\\u%04x", Integer.valueOf(i));
        }
        REPLACEMENT_CHARS[34] = "\\\"";
        REPLACEMENT_CHARS[92] = "\\\\";
        REPLACEMENT_CHARS[9] = "\\t";
        REPLACEMENT_CHARS[8] = "\\b";
        REPLACEMENT_CHARS[10] = "\\n";
        REPLACEMENT_CHARS[13] = "\\r";
        REPLACEMENT_CHARS[12] = "\\f";
        HTML_SAFE_REPLACEMENT_CHARS[60] = "\\u003c";
        HTML_SAFE_REPLACEMENT_CHARS[62] = "\\u003e";
        HTML_SAFE_REPLACEMENT_CHARS[38] = "\\u0026";
        HTML_SAFE_REPLACEMENT_CHARS[61] = "\\u003d";
        HTML_SAFE_REPLACEMENT_CHARS[39] = "\\u0027";
    }

    public JsonWriter(Writer out2) {
        push(6);
        this.separator = ":";
        this.serializeNulls = true;
        if (out2 == null) {
            throw new NullPointerException("out == null");
        }
        this.out = out2;
    }

    public final void setIndent(String indent2) {
        if (indent2.length() == 0) {
            this.indent = null;
            this.separator = ":";
            return;
        }
        this.indent = indent2;
        this.separator = ": ";
    }

    public final void setLenient(boolean lenient2) {
        this.lenient = lenient2;
    }

    public boolean isLenient() {
        return this.lenient;
    }

    public final void setHtmlSafe(boolean htmlSafe2) {
        this.htmlSafe = htmlSafe2;
    }

    public final boolean isHtmlSafe() {
        return this.htmlSafe;
    }

    public final void setSerializeNulls(boolean serializeNulls2) {
        this.serializeNulls = serializeNulls2;
    }

    public final boolean getSerializeNulls() {
        return this.serializeNulls;
    }

    public JsonWriter beginArray() throws IOException {
        writeDeferredName();
        return open(1, "[");
    }

    public JsonWriter endArray() throws IOException {
        return close(1, 2, "]");
    }

    public JsonWriter beginObject() throws IOException {
        writeDeferredName();
        return open(3, "{");
    }

    public JsonWriter endObject() throws IOException {
        return close(3, 5, "}");
    }

    private JsonWriter open(int empty, String openBracket) throws IOException {
        beforeValue();
        push(empty);
        this.out.write(openBracket);
        return this;
    }

    private JsonWriter close(int empty, int nonempty, String closeBracket) throws IOException {
        int context = peek();
        if (context != nonempty && context != empty) {
            throw new IllegalStateException("Nesting problem.");
        } else if (this.deferredName != null) {
            throw new IllegalStateException("Dangling name: " + this.deferredName);
        } else {
            this.stackSize--;
            if (context == nonempty) {
                newline();
            }
            this.out.write(closeBracket);
            return this;
        }
    }

    private void push(int newTop) {
        if (this.stackSize == this.stack.length) {
            int[] newStack = new int[(this.stackSize * 2)];
            System.arraycopy(this.stack, 0, newStack, 0, this.stackSize);
            this.stack = newStack;
        }
        int[] iArr = this.stack;
        int i = this.stackSize;
        this.stackSize = i + 1;
        iArr[i] = newTop;
    }

    private int peek() {
        if (this.stackSize != 0) {
            return this.stack[this.stackSize - 1];
        }
        throw new IllegalStateException("JsonWriter is closed.");
    }

    private void replaceTop(int topOfStack) {
        this.stack[this.stackSize - 1] = topOfStack;
    }

    public JsonWriter name(String name) throws IOException {
        if (name == null) {
            throw new NullPointerException("name == null");
        } else if (this.deferredName != null) {
            throw new IllegalStateException();
        } else if (this.stackSize == 0) {
            throw new IllegalStateException("JsonWriter is closed.");
        } else {
            this.deferredName = name;
            return this;
        }
    }

    private void writeDeferredName() throws IOException {
        if (this.deferredName != null) {
            beforeName();
            string(this.deferredName);
            this.deferredName = null;
        }
    }

    public JsonWriter value(String value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        writeDeferredName();
        beforeValue();
        string(value);
        return this;
    }

    public JsonWriter jsonValue(String value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        writeDeferredName();
        beforeValue();
        this.out.append((CharSequence) value);
        return this;
    }

    public JsonWriter nullValue() throws IOException {
        if (this.deferredName != null) {
            if (this.serializeNulls) {
                writeDeferredName();
            } else {
                this.deferredName = null;
                return this;
            }
        }
        beforeValue();
        this.out.write("null");
        return this;
    }

    public JsonWriter value(boolean value) throws IOException {
        writeDeferredName();
        beforeValue();
        this.out.write(value ? "true" : "false");
        return this;
    }

    public JsonWriter value(Boolean value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        writeDeferredName();
        beforeValue();
        this.out.write(value.booleanValue() ? "true" : "false");
        return this;
    }

    public JsonWriter value(double value) throws IOException {
        writeDeferredName();
        if (this.lenient || (!Double.isNaN(value) && !Double.isInfinite(value))) {
            beforeValue();
            this.out.append((CharSequence) Double.toString(value));
            return this;
        }
        throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
    }

    public JsonWriter value(long value) throws IOException {
        writeDeferredName();
        beforeValue();
        this.out.write(Long.toString(value));
        return this;
    }

    public JsonWriter value(Number value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        writeDeferredName();
        String string = value.toString();
        if (this.lenient || (!string.equals("-Infinity") && !string.equals("Infinity") && !string.equals("NaN"))) {
            beforeValue();
            this.out.append((CharSequence) string);
            return this;
        }
        throw new IllegalArgumentException("Numeric values must be finite, but was " + value);
    }

    @Override // java.io.Flushable
    public void flush() throws IOException {
        if (this.stackSize == 0) {
            throw new IllegalStateException("JsonWriter is closed.");
        }
        this.out.flush();
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        this.out.close();
        int size = this.stackSize;
        if (size > 1 || (size == 1 && this.stack[size - 1] != 7)) {
            throw new IOException("Incomplete document");
        }
        this.stackSize = 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x002f  */
    private void string(String value) throws IOException {
        String replacement;
        String[] replacements = this.htmlSafe ? HTML_SAFE_REPLACEMENT_CHARS : REPLACEMENT_CHARS;
        this.out.write("\"");
        int last = 0;
        int length = value.length();
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            if (c < 128) {
                replacement = replacements[c];
                if (replacement == null) {
                }
                if (last < i) {
                    this.out.write(value, last, i - last);
                }
                this.out.write(replacement);
                last = i + 1;
            } else {
                if (c == 8232) {
                    replacement = "\\u2028";
                } else if (c == 8233) {
                    replacement = "\\u2029";
                }
                if (last < i) {
                }
                this.out.write(replacement);
                last = i + 1;
            }
        }
        if (last < length) {
            this.out.write(value, last, length - last);
        }
        this.out.write("\"");
    }

    private void newline() throws IOException {
        if (this.indent != null) {
            this.out.write("\n");
            int size = this.stackSize;
            for (int i = 1; i < size; i++) {
                this.out.write(this.indent);
            }
        }
    }

    private void beforeName() throws IOException {
        int context = peek();
        if (context == 5) {
            this.out.write(44);
        } else if (context != 3) {
            throw new IllegalStateException("Nesting problem.");
        }
        newline();
        replaceTop(4);
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    private void beforeValue() throws IOException {
        switch (peek()) {
            case 1:
                replaceTop(2);
                newline();
                return;
            case 2:
                this.out.append(',');
                newline();
                return;
            case 3:
            case 5:
            default:
                throw new IllegalStateException("Nesting problem.");
            case 4:
                this.out.append((CharSequence) this.separator);
                replaceTop(5);
                return;
            case 6:
                break;
            case 7:
                if (!this.lenient) {
                    throw new IllegalStateException("JSON must have only one top-level value.");
                }
                break;
        }
        replaceTop(7);
    }
}
