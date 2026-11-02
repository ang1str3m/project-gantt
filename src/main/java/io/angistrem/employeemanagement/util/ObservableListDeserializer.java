/*
 * Copyright (c) 2026.
 */

package io.angistrem.employeemanagement.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.angistrem.employeemanagement.model.SubTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.List;

public class ObservableListDeserializer extends JsonDeserializer<ObservableList<SubTask>> {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public ObservableList<SubTask> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        // Читаем как обычный List
        List<SubTask> list = mapper.readValue(p, new TypeReference<List<SubTask>>() {});
        // Превращаем в ObservableList
        return FXCollections.observableArrayList(list);
    }
}
