package com.gigi.validator;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class JsonValidator {

    private static final String REQUIRED = "<required>";
    private static final String UUID = "#REGEX_UUID";
    private static final String IP = "#REGEX_IPv4";

    private static final Pattern UUID_REGEX = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    private static final Pattern IP_REGEX = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private Map<String, Object> template;

    private List<String> errors = new ArrayList<>();

    public JsonValidator(String resource) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        try {
            template = mapper.readValue(getClass().getClassLoader().getResourceAsStream(resource), Map.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isValid(Map<String, Object> jsonMap) {
        return isValid(template, jsonMap, null, true);
    }

    private <T, U> boolean isValid(T template, U value, String currentField, boolean required) {

        if (required && value == null) {
            errors.add(currentField + " is mandatory");
            return false;
        } else if (!required && value == null) {
            return true;
        }

        if (template instanceof Map) {
            if (!(value instanceof Map)) {
                errors.add("The type of field " + currentField + " must be of type object");
                return false;
            }
            boolean valid = true;
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) template).entrySet()) {

                String templateKey = entry.getKey();
                Object templateValue = entry.getValue();

                boolean req = templateKey.contains(REQUIRED);
                if (req) {
                    templateKey = templateKey.replace(templateKey.substring(templateKey.indexOf(REQUIRED), templateKey.length()), "");
                }
                Object val = ((Map) value).get(templateKey);
                valid = valid && isValid(templateValue, val, templateKey, req);
            }
            return valid;
        }
        if (template instanceof List) {
            if (!(value instanceof List)) {
                errors.add("The type of field " + currentField + " must be of type array");
                return false;
            }
            boolean valid = true;
            for (Object o : (List) value) {
                valid = valid && isValid(((List) template).get(0), o, null, false);
            }
            return valid;
        }
        if (template instanceof String) {
            if (!(value instanceof String)) {
                errors.add("The type of field " + currentField + " must be of type string");
                return false;
            }
            String val = (String) value;
            if (required && val.equals("")) {
                errors.add(currentField + " must not be empty");
                return false;
            }
            switch ((String)template) {
                case UUID: {
                    if(!UUID_REGEX.matcher(val).matches()) {
                        errors.add(val + " value for field " + currentField + " is not a valid uuid");
                        return false;
                    }
                    return true;
                }
                case IP: {
                    if(!IP_REGEX.matcher(val).matches()) {
                        errors.add(val + " value for field " + currentField + " is not a valid ipV4 address");
                        return false;
                    }
                    return true;
                }
                default: return true;
            }
        }
        if (template instanceof Integer) {
            if (!(value instanceof Integer)) {
                errors.add("The type of field " + currentField + " must be of type integer");
                return false;
            }
            return true;
        }
        if (template instanceof Boolean) {
            if (!(value instanceof Boolean)) {
                errors.add("The type of field " + currentField + " must be of type boolean");
                return false;
            }
            return true;
        }
        if (template instanceof Double) {
            if (!(value instanceof Double)) {
                errors.add("The type of field " + currentField + " must be of type decimal");
                return false;
            }
            return true;
        }
        throw new RuntimeException("Invalid type of mapped object received for validation: " + template.getClass().getSimpleName());
    }

    public List<String> getErrors() {
        return errors;
    }
}
