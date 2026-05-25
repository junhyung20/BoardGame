package com.example.boardgame.socket.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParser;

import java.util.UUID;

public class SocketMessage {
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_REQUEST_ID = "requestId";
    public static final String FIELD_FIELDS = "fields";

    private static final Gson GSON = new Gson();

    private final String type;
    private final String requestId;
    private final JsonObject fields;

    private SocketMessage(String type, String requestId, JsonObject fields) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Message type is required");
        }
        this.type = type;
        this.requestId = requestId == null ? "" : requestId;
        this.fields = copyObject(fields);
    }

    public static SocketMessage command(String type) {
        return builder(type)
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    public static Builder builder(String type) {
        return new Builder(type);
    }

    public static SocketMessage parse(String wireText) {
        JsonObject root = JsonParser.parseString(wireText).getAsJsonObject();
        String type = valueAsString(root.get(FIELD_TYPE));
        String requestId = valueAsString(root.get(FIELD_REQUEST_ID));
        return new SocketMessage(type, requestId, objectOrEmpty(root.get(FIELD_FIELDS)));
    }

    public String toWireText() {
        JsonObject root = new JsonObject();
        root.addProperty(FIELD_TYPE, type);
        root.addProperty(FIELD_REQUEST_ID, requestId);

        root.add(FIELD_FIELDS, fields.deepCopy());

        return GSON.toJson(root);
    }

    public String getType() {
        return type;
    }

    public String getRequestId() {
        return requestId;
    }

    public String get(String key) {
        JsonElement value = fields.get(key);
        return value == null ? null : valueAsString(value);
    }

    public String getOrDefault(String key, String defaultValue) {
        JsonElement value = fields.get(key);
        return value == null || value.isJsonNull() ? defaultValue : valueAsString(value);
    }

    public int getInt(String key, int defaultValue) {
        JsonElement value = fields.get(key);
        if (value == null || value.isJsonNull()) {
            return defaultValue;
        }
        return value.getAsInt();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        JsonElement value = fields.get(key);
        if (value == null || value.isJsonNull()) {
            return defaultValue;
        }
        return value.getAsBoolean();
    }

    public JsonElement getField(String key) {
        return copy(fields.get(key));
    }

    public JsonObject getObject(String key) {
        JsonElement value = fields.get(key);
        if (value == null || value.isJsonNull()) {
            return new JsonObject();
        }
        return value.getAsJsonObject().deepCopy();
    }

    public JsonObject getFields() {
        return fields.deepCopy();
    }

    private static JsonObject objectOrEmpty(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private static String valueAsString(JsonElement element) {
        if (element == null || element instanceof JsonNull || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return GSON.toJson(element);
    }

    private static JsonObject copyObject(JsonObject object) {
        if (object == null) {
            return new JsonObject();
        }
        return object.deepCopy();
    }

    private static JsonElement copy(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        return value.deepCopy();
    }

    public static class Builder {
        private final String type;
        private String requestId = "";
        private final JsonObject fields = new JsonObject();

        private Builder(String type) {
            this.type = type;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder put(String key, String value) {
            if (key != null && !key.trim().isEmpty()) {
                fields.add(key, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
            }
            return this;
        }

        public Builder put(String key, int value) {
            return put(key, new JsonPrimitive(value));
        }

        public Builder put(String key, long value) {
            return put(key, new JsonPrimitive(value));
        }

        public Builder put(String key, boolean value) {
            return put(key, new JsonPrimitive(value));
        }

        public Builder put(String key, JsonElement value) {
            if (key != null && !key.trim().isEmpty()) {
                fields.add(key, copy(value));
            }
            return this;
        }

        public SocketMessage build() {
            return new SocketMessage(type, requestId, fields);
        }
    }
}
