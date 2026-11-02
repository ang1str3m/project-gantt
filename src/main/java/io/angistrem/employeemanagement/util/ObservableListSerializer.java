package io.angistrem.employeemanagement.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.angistrem.employeemanagement.model.SubTask;
import javafx.collections.ObservableList;

import java.io.IOException;

public class ObservableListSerializer extends JsonSerializer<ObservableList<SubTask>> {

    @Override
    public void serialize(ObservableList<SubTask> value, JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        // Просто сериализуем как обычный List
        gen.writeObject(value);
    }
}