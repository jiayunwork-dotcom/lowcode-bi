package com.lowcode.bi.expression;

import com.lowcode.bi.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExpressionParser {

    private static final Set<String> ALLOWED_FUNCTIONS = new HashSet<>(Arrays.asList(
            "SUM", "AVG", "COUNT", "MAX", "MIN", "ABS", "ROUND", "CEIL", "FLOOR",
            "IF", "CASE", "WHEN", "THEN", "ELSE", "END",
            "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND", "DATE", "NOW", "DATEDIFF", "DATEADD", "DATETIME",
            "CONCAT", "SUBSTRING", "LEFT", "RIGHT", "LENGTH", "TRIM", "UPPER", "LOWER", "REPLACE",
            "TO_STRING", "TO_NUMBER", "TO_DATE",
            "ISNULL", "ISNOTNULL", "COALESCE",
            "||", "AND", "OR", "NOT"
    ));

    private static final Set<String> FORBIDDEN_PATTERNS = new HashSet<>(Arrays.asList(
            "Runtime", "System", "Process", "File", "java.", "javax.", "sun.",
            "exec", "execute", "eval", "new ", "class", "import", "package",
            "getClass", "getDeclared", "invoke", "forName",
            "UNION", "JOIN", "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE",
            "CREATE", "ALTER", "DROP", "TRUNCATE"
    ));

    private static final Pattern FIELD_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("([A-Z_]+\\s*\\(");

    public ExpressionValidationResult validate(String expression) {
        return validate(expression, Collections.emptySet());
    }

    public ExpressionValidationResult validate(String expression, Set<String> availableFields) {
        ExpressionValidationResult result = new ExpressionValidationResult();
        result.setOriginalExpression(expression);

        if (expression == null || expression.trim().isEmpty()) {
            result.setValid(false);
            result.addError(0, "表达式不能为空");
            return result;
        }

        String upperExpr = expression.toUpperCase();

        for (String forbidden : FORBIDDEN_PATTERNS) {
            if (upperExpr.contains(forbidden.toUpperCase())) {
                result.setValid(false);
                int position = upperExpr.indexOf(forbidden.toUpperCase());
                result.addError(position, "包含不允许的关键字或模式: " + forbidden);
            }
        }

        Matcher funcMatcher = FUNCTION_PATTERN.matcher(upperExpr);
        while (funcMatcher.find()) {
            String funcName = funcMatcher.group(1).trim();
            if (!ALLOWED_FUNCTIONS.contains(funcName)) {
                result.setValid(false);
                result.addError(funcMatcher.start(), "不支持的函数: " + funcName);
            }
        }

        Matcher fieldMatcher = FIELD_PATTERN.matcher(expression);
        Set<String> referencedFields = new HashSet<>();
        while (fieldMatcher.find()) {
            String fieldName = fieldMatcher.group(1).trim();
            referencedFields.add(fieldName);
            if (availableFields.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (String available : availableFields) {
                if (available.equalsIgnoreCase(fieldName) ||
                        available.endsWith("." + fieldName) ||
                        available.endsWith("]" + fieldName)) {
                    found = true;
                    break;
                }
            }
            if (!found && !availableFields.contains(fieldName)) {
                result.setValid(false);
                result.addError(fieldMatcher.start(), "字段不存在: " + fieldName);
            }
        }
        result.setReferencedFields(referencedFields);

        try {
            String testExpr = expression
                    .replaceAll("\\[[^\\]]+\\]", "1");
            if (!checkParenthesesBalance(testExpr)) {
                result.setValid(false);
                result.addError(0, "括号不匹配");
            }
            if (!checkOperatorsValid(testExpr)) {
                result.setValid(false);
                result.addError(0, "运算符使用不正确");
            }
        } catch (Exception e) {
            result.setValid(false);
            result.addError(0, "表达式语法错误: " + e.getMessage());
        }

        if (result.getErrors().isEmpty()) {
            result.setValid(true);
        }

        return result;
    }

    public String parseToSql(String expression, Map<String, String> fieldMappings) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        String sqlExpression = expression;

        sqlExpression = sqlExpression
                .replaceAll("(?i)\\bAND\\b", "AND")
                .replaceAll("(?i)\\bOR\\b", "OR")
                .replaceAll("(?i)\\bNOT\\b", "NOT")
                .replaceAll("\\|\\|", "||");

        Matcher fieldMatcher = FIELD_PATTERN.matcher(sqlExpression);
        StringBuffer sb = new StringBuffer();
        while (fieldMatcher.find()) {
            String fieldName = fieldMatcher.group(1).trim();
            String sqlField = fieldMappings.getOrDefault(fieldName, fieldMatcher.group(0));
            fieldMatcher.appendReplacement(sb, Matcher.quoteReplacement(sqlField));
        }
        fieldMatcher.appendTail(sb);
        sqlExpression = sb.toString();

        sqlExpression = sqlExpression
                .replaceAll("(?i)\\bIF\\s*\\(", "CASE WHEN ")
                .replaceAll("(?i)\\bDATEDIFF\\s*\\(", "DATEDIFF(")
                .replaceAll("(?i)\\bDATEADD\\s*\\(", "DATE_ADD(")
                .replaceAll("(?i)\\bTO_STRING\\s*\\(", "CAST(")
                .replaceAll("(?i)\\bTO_NUMBER\\s*\\(", "CAST(")
                .replaceAll("(?i)\\bTO_DATE\\s*\\(", "CAST(")
                .replaceAll("(?i)\\bISNULL\\s*\\(", "ISNULL(")
                .replaceAll("(?i)\\bISNOTNULL\\s*\\(", "ISNOTNULL(")
                .replaceAll("(?i)\\bCOALESCE\\s*\\(", "COALESCE(");

        return sqlExpression;
    }

    private boolean checkParenthesesBalance(String expr) {
        int balance = 0;
        for (char c : expr.toCharArray()) {
            if (c == '(') balance++;
            else if (c == ')') balance--;
            if (balance < 0) return false;
        }
        return balance == 0;
    }

    private boolean checkOperatorsValid(String expr) {
        String trimmed = expr.trim();
        if (trimmed.matches(".*[+\\-*/]$")) {
            return false;
        }
        if (trimmed.matches("^[+\\-*/].*")) {
            return false;
        }
        return true;
    }

    public static class ExpressionValidationResult {
        private String originalExpression;
        private String parsedExpression;
        private boolean valid;
        private Set<String> referencedFields = new HashSet<>();
        private List<ExpressionError> errors = new ArrayList<>();

        public void addError(int position, String message) {
            errors.add(new ExpressionError(position, message));
        }

        public String getOriginalExpression() { return originalExpression; }
        public void setOriginalExpression(String originalExpression) { this.originalExpression = originalExpression; }
        public String getParsedExpression() { return parsedExpression; }
        public void setParsedExpression(String parsedExpression) { this.parsedExpression = parsedExpression; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public Set<String> getReferencedFields() { return referencedFields; }
        public void setReferencedFields(Set<String> referencedFields) { this.referencedFields = referencedFields; }
        public List<ExpressionError> getErrors() { return errors; }
        public void setErrors(List<ExpressionError> errors) { this.errors = errors; }
    }

    public static class ExpressionError {
        private int position;
        private String message;

        public ExpressionError(int position, String message) {
            this.position = position;
            this.message = message;
        }

        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
