package com.lowcode.bi.query;

import com.lowcode.bi.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SqlSecurityValidator {

    private static final List<String> FORBIDDEN_KEYWORDS = Arrays.asList(
            "DROP", "ALTER", "CREATE", "TRUNCATE", "RENAME", "COMMENT",
            "INSERT", "UPDATE", "DELETE", "MERGE", "UPSERT", "REPLACE",
            "GRANT", "REVOKE", "DENY",
            "EXEC", "EXECUTE", "SP_", "XP_",
            "UNION ALL SELECT", "UNION SELECT",
            "INTO OUTFILE", "INTO DUMPFILE",
            "LOAD DATA", "LOAD_FILE",
            "COPY", "COPY FROM", "COPY TO",
            "--", "/*", "*/", "#",
            "SLEEP", "BENCHMARK",
            "OR 1=1", "OR '1'='1",
            "pg_sleep", "sleep",
            "information_schema", "pg_catalog", "sys."
    );

    private static final List<String> DANGEROUS_FUNCTIONS = Arrays.asList(
            "sleep(", "benchmark(", "pg_sleep(",
            "load_file(", "into outfile", "into dumpfile",
            "system(", "shell(", "exec(",
            "eval(", "execute(",
            "read_file(", "write_file("
    );

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("--.*$", Pattern.MULTILINE);

    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'([^']*)'");

    public String validateAndSanitize(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new BusinessException("SQL语句不能为空");
        }

        String normalized = normalizeSql(sql);
        String upperSql = normalized.toUpperCase();

        if (!upperSql.trim().startsWith("SELECT") && !upperSql.trim().startsWith("WITH")) {
            throw new BusinessException("只允许SELECT查询语句，禁止执行其他类型的SQL");
        }

        return normalized;
    }

    public void validateForbiddenOperations(String sql) {
        String upperSql = sql.toUpperCase();
        String strippedSql = stripStringLiterals(sql).toUpperCase();

        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (strippedSql.contains(keyword.toUpperCase())) {
                throw new BusinessException("SQL包含禁止的关键字: " + keyword);
            }
        }

        String lowerSql = sql.toLowerCase();
        for (String function : DANGEROUS_FUNCTIONS) {
            if (lowerSql.contains(function.toLowerCase())) {
                throw new BusinessException("SQL包含禁止的函数: " + function);
            }
        }

        if (containsMultipleStatements(sql)) {
            throw new BusinessException("SQL不允许包含多条语句");
        }

        if (countUnmatchedParentheses(sql) != 0) {
            throw new BusinessException("SQL括号不匹配");
        }
    }

    private String normalizeSql(String sql) {
        String normalized = sql.trim();

        normalized = COMMENT_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = LINE_COMMENT_PATTERN.matcher(normalized).replaceAll(" ");

        normalized = normalized.replaceAll("\\s+", " ");

        return normalized;
    }

    private String stripStringLiterals(String sql) {
        return STRING_LITERAL_PATTERN.matcher(sql).replaceAll("''");
    }

    private boolean containsMultipleStatements(String sql) {
        int semicolonCount = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (inString) {
                if (c == stringChar) {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == stringChar) {
                        i++;
                    } else {
                        inString = false;
                    }
                }
            } else {
                if (c == '\'' || c == '"') {
                    inString = true;
                    stringChar = c;
                } else if (c == ';') {
                    semicolonCount++;
                    if (semicolonCount > 1) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int countUnmatchedParentheses(String sql) {
        int count = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (inString) {
                if (c == stringChar) {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == stringChar) {
                        i++;
                    } else {
                        inString = false;
                    }
                }
            } else {
                if (c == '\'' || c == '"') {
                    inString = true;
                    stringChar = c;
                } else if (c == '(') {
                    count++;
                } else if (c == ')') {
                    count--;
                }
            }
        }

        return count;
    }

    public boolean isSafeQuery(String sql) {
        try {
            validateAndSanitize(sql);
            validateForbiddenOperations(sql);
            return true;
        } catch (Exception e) {
            log.warn("Unsafe query detected: {}", e.getMessage());
            return false;
        }
    }

    public List<String> extractParameters(String sql) {
        java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = paramPattern.matcher(sql);

        java.util.Set<String> params = new java.util.HashSet<>();
        while (matcher.find()) {
            params.add(matcher.group(1));
        }

        return new java.util.ArrayList<>(params);
    }
}
