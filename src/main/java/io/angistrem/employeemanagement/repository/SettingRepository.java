
package io.angistrem.employeemanagement.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.angistrem.employeemanagement.model.Settings;
import io.angistrem.employeemanagement.util.AppPaths;

import java.io.File;

public class SettingRepository {

    private static final String PATH = AppPaths.getAppDirectory()
            + File.separator + "settings.json";

    private final ObjectMapper mapper = new ObjectMapper();

    public Settings load() {
        try {
            File file = new File(PATH);
            if (!file.exists()) return new Settings();
            return mapper.readValue(file, Settings.class);
        } catch (Exception e) {
            return new Settings();
        }
    }

    public void save(Settings settings) {
        try {
            File file = new File(PATH);
            file.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, settings);
        } catch (Exception ignored) {}
    }
}
