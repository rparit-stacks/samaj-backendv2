package com.rps.samaj.exam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validates the flexible JSON "paper" attached to an exam (sections, questions, options).
 */
@Component
public class ExamPaperValidator {

    private static final Set<String> QUESTION_TYPES = Set.of(
            "MCQ",
            "MCQ_MULTI",
            "TRUE_FALSE",
            "DESCRIPTIVE",
            "SHORT_ANSWER",
            "NUMERIC"
    );

    public void validateOrNull(JsonNode paper) {
        if (paper == null || paper.isNull() || paper.isMissingNode()) {
            return;
        }
        if (!paper.isObject()) {
            throw badRequest("Paper must be a JSON object");
        }
        if (!paper.has("version") || !paper.get("version").isIntegralNumber()) {
            throw badRequest("Paper.version is required and must be a number");
        }
        int version = paper.get("version").asInt();
        if (version < 1) {
            throw badRequest("Paper.version must be >= 1");
        }
        if (!paper.has("sections") || !paper.get("sections").isArray()) {
            throw badRequest("Paper.sections must be an array");
        }
        ArrayNode sections = (ArrayNode) paper.get("sections");
        Set<String> sectionIds = new HashSet<>();
        for (JsonNode section : sections) {
            validateSection(section, sectionIds);
        }
    }

    private void validateSection(JsonNode section, Set<String> sectionIds) {
        if (!section.isObject()) {
            throw badRequest("Each section must be an object");
        }
        requireText(section, "id", "Section id");
        requireText(section, "title", "Section title");
        String sid = text(section, "id");
        if (!sectionIds.add(sid)) {
            throw badRequest("Duplicate section id: " + sid);
        }
        if (section.has("customFields")) {
            validateCustomFields(section.get("customFields"), "Section " + sid);
        }
        if (!section.has("questions") || !section.get("questions").isArray()) {
            throw badRequest("Section.questions must be an array for section " + sid);
        }
        Set<String> questionIds = new HashSet<>();
        for (JsonNode q : section.get("questions")) {
            validateQuestion(q, sid, questionIds);
        }
    }

    private void validateQuestion(JsonNode q, String sectionId, Set<String> questionIds) {
        if (!q.isObject()) {
            throw badRequest("Each question must be an object in section " + sectionId);
        }
        requireText(q, "id", "Question id");
        String qid = text(q, "id");
        if (!questionIds.add(qid)) {
            throw badRequest("Duplicate question id in section " + sectionId + ": " + qid);
        }
        requireText(q, "type", "Question type");
        String type = text(q, "type").toUpperCase(Locale.ROOT);
        if (!QUESTION_TYPES.contains(type)) {
            throw badRequest("Unknown question type '" + type + "' for question " + qid);
        }
        requireText(q, "prompt", "Question prompt");

        if (q.has("customFields")) {
            validateCustomFields(q.get("customFields"), "Question " + qid);
        }

        JsonNode options = q.get("options");
        switch (type) {
            case "MCQ" -> validateMcqOptions(options, qid, false);
            case "MCQ_MULTI" -> validateMcqOptions(options, qid, true);
            case "TRUE_FALSE" -> validateTrueFalse(options, qid);
            case "DESCRIPTIVE", "SHORT_ANSWER", "NUMERIC" -> {
                if (options != null && !options.isNull() && options.isArray() && options.size() > 0) {
                    throw badRequest("Question type " + type + " must not have options (" + qid + ")");
                }
            }
            default -> {
            }
        }
    }

    private void validateMcqOptions(JsonNode options, String qid, boolean multi) {
        if (options == null || !options.isArray()) {
            throw badRequest("MCQ question must have an options array (" + qid + ")");
        }
        ArrayNode arr = (ArrayNode) options;
        if (arr.size() < 2) {
            throw badRequest("MCQ must have at least 2 options (" + qid + ")");
        }
        int correct = 0;
        for (JsonNode opt : arr) {
            if (!opt.isObject()) {
                throw badRequest("Each option must be an object (" + qid + ")");
            }
            requireText(opt, "id", "Option id");
            requireText(opt, "label", "Option label");
            if (opt.has("correct") && opt.get("correct").isBoolean() && opt.get("correct").asBoolean()) {
                correct++;
            }
        }
        if (!multi && correct != 1) {
            throw badRequest("MCQ must have exactly one correct option (" + qid + ")");
        }
        if (multi && correct < 1) {
            throw badRequest("MCQ_MULTI must have at least one correct option (" + qid + ")");
        }
    }

    private void validateTrueFalse(JsonNode options, String qid) {
        if (options == null || !options.isArray() || ((ArrayNode) options).size() != 2) {
            throw badRequest("TRUE_FALSE must have exactly 2 options (" + qid + ")");
        }
        int correct = 0;
        for (JsonNode opt : options) {
            if (!opt.isObject()) {
                throw badRequest("Each option must be an object (" + qid + ")");
            }
            requireText(opt, "label", "Option label");
            if (opt.has("correct") && opt.get("correct").isBoolean() && opt.get("correct").asBoolean()) {
                correct++;
            }
        }
        if (correct != 1) {
            throw badRequest("TRUE_FALSE must have exactly one correct option (" + qid + ")");
        }
    }

    private void validateCustomFields(JsonNode node, String ctx) {
        if (node.isNull()) {
            return;
        }
        if (!node.isArray()) {
            throw badRequest(ctx + ": customFields must be an array");
        }
        int i = 0;
        for (JsonNode row : node) {
            if (!row.isObject()) {
                throw badRequest(ctx + ": customFields[" + i + "] must be an object");
            }
            requireText(row, "key", ctx + " customFields[" + i + "].key");
            requireText(row, "value", ctx + " customFields[" + i + "].value");
            i++;
        }
    }

    private static void requireText(JsonNode parent, String field, String label) {
        if (!parent.has(field) || !parent.get(field).isTextual() || parent.get(field).asText().isBlank()) {
            throw badRequest(label + " is required");
        }
    }

    private static String text(JsonNode parent, String field) {
        return parent.get(field).asText();
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
