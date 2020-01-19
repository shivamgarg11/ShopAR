package com.example.deon.furnituar;

import android.util.JsonWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class WriteJson {
    JsonWriter writer;
    String target3DObj;

    public WriteJson(String inputTarget) {
        target3DObj = inputTarget;
    }
    public void writeJson(OutputStream out) throws IOException {
        writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        jsonFinal(writer, target3DObj);
    }

    public void jsonFinal(JsonWriter writer, String target3DObj) throws IOException{
        writer.beginObject();
        writer.name("filename").value(target3DObj);
        writer.endObject();
    }
}
