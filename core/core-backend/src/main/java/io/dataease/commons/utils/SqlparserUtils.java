package io.dataease.commons.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import io.dataease.api.dataset.dto.SqlVariableDetails;
import io.dataease.exception.DEException;
import io.dataease.i18n.Translator;
import io.dataease.utils.JsonUtil;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.dataease.chart.manage.ChartDataManage.START_END_SEPARATOR;
import static org.apache.calcite.sql.SqlKind.*;

public class SqlparserUtils {
    public static final String regex = "\\$\\{(.*?)\\}";
    private static final String SubstitutedParams = "DATAEASE_PATAMS_BI";
    private static final String SubstitutedSql = " 'DE-BI' = 'DE-BI' ";
    private static final String SubstitutedSqlVirtualData = " 1 > 2 ";

    public static String removeVariables(final String sql) {
        String tmpSql = sql;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(sql);
        boolean hasVariables = false;
        while (matcher.find()) {
            hasVariables = true;
            tmpSql = tmpSql.replace(matcher.group(), SubstitutedParams);
        }
        if (!hasVariables && !tmpSql.contains(SubstitutedParams)) {
            return tmpSql;
        }

        SqlParser.Config config =
                SqlParser.configBuilder()
                        .setLex(Lex.JAVA)
                        .setIdentifierMaxLength(256)
                        .build();
        SqlParser sqlParser = SqlParser.create(tmpSql, config);
        SqlNode sqlNode;
        try {
            sqlNode = sqlParser.parseStmt();
        } catch (SqlParseException e) {
            throw new RuntimeException("使用 Calcite 进行语法分析发生了异常", e);
        }
        // 递归遍历语法树
        getDependencies(sqlNode, false);
        return sqlNode.toString();
    }

    private static void getDependencies(SqlNode sqlNode, Boolean fromOrJoin) {

        if (sqlNode.getKind() == JOIN) {
            SqlJoin sqlKind = (SqlJoin) sqlNode;

        } else if (sqlNode.getKind() == IDENTIFIER) {

        } else if (sqlNode.getKind() == AS) {
            SqlBasicCall sqlKind = (SqlBasicCall) sqlNode;
        } else if (sqlNode.getKind() == SELECT) {
            SqlSelect sqlKind = (SqlSelect) sqlNode;
            List<SqlNode> list = sqlKind.getSelectList().getList();
            for (SqlNode i : list) {
                getDependencies(i, false);
            }
            SqlNode from = sqlKind.getFrom().accept(getSqlShuttle());
            sqlKind.setFrom(from);
            if (sqlKind.getWhere() != null) {
                SqlNode newWhere = sqlKind.getWhere().accept(getSqlShuttle());
                sqlKind.setWhere(newWhere);
            }

        } else {
            // TODO 这里可根据需求拓展处理其他类型的 sqlNode
        }
    }

    public static SqlShuttle getSqlShuttle() {
        return new SqlShuttle() {

            @Override
            public @Nullable SqlNode visit(final SqlCall call) {
                CallCopyingArgHandler argHandler = new CallCopyingArgHandler(call, false);
                call.getOperator().acceptCall(this, call, false, argHandler);
                if (argHandler.result().toString().contains(SubstitutedParams)) {
                    SqlNode sqlNode1 = null;
                    try {
                        sqlNode1 = SqlParser.create(SubstitutedSql).parseExpression();
                    } catch (Exception e) {

                    }
                    return sqlNode1;
                }
                return argHandler.result();
            }
        };
    }

    public static String handleVariableDefaultValue(String sql, String sqlVariableDetails, boolean isEdit, boolean isFromDataSet, List<SqlVariableDetails> parameters) {
        if (StringUtils.isEmpty(sql)) {
            DEException.throwException(Translator.get("i18n_sql_not_empty"));
        }
        if (sql.trim().endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }

        if (StringUtils.isNotEmpty(sqlVariableDetails)) {
            TypeReference<List<SqlVariableDetails>> listTypeReference = new TypeReference<List<SqlVariableDetails>>() {
            };
            List<SqlVariableDetails> defaultsSqlVariableDetails = JsonUtil.parseList(sqlVariableDetails, listTypeReference);
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(sql);

            while (matcher.find()) {
                SqlVariableDetails defaultsSqlVariableDetail = null;
                for (SqlVariableDetails sqlVariableDetail : defaultsSqlVariableDetails) {
                    if (matcher.group().substring(2, matcher.group().length() - 1).equalsIgnoreCase(sqlVariableDetail.getVariableName())) {
                        defaultsSqlVariableDetail = sqlVariableDetail;
                        break;
                    }
                }
                SqlVariableDetails filterParameter = null;
                if (ObjectUtils.isNotEmpty(parameters)) {
                    for (SqlVariableDetails parameter : parameters) {
                        if (parameter.getVariableName().equalsIgnoreCase(defaultsSqlVariableDetail.getVariableName())) {
                            filterParameter = parameter;
                        }
                    }
                }
                if (filterParameter != null) {
                    sql = sql.replace(matcher.group(), transFilter(filterParameter));
                } else {
                    if (defaultsSqlVariableDetail != null && StringUtils.isNotEmpty(defaultsSqlVariableDetail.getDefaultValue())) {
                        if (!isEdit && isFromDataSet && defaultsSqlVariableDetail.getDefaultValueScope().equals(SqlVariableDetails.DefaultValueScope.ALLSCOPE)) {
                            sql = sql.replace(matcher.group(), defaultsSqlVariableDetail.getDefaultValue());
                        }
                        if (isEdit) {
                            sql = sql.replace(matcher.group(), defaultsSqlVariableDetail.getDefaultValue());
                        }
                    }
                }
            }
        }

        try {
            sql = removeVariables(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sql;
    }


    private static String transFilter(SqlVariableDetails sqlVariableDetails) {
        if (sqlVariableDetails.getOperator().equals("in")) {
            return "'" + String.join("','", sqlVariableDetails.getValue()) + "'";
        } else if (sqlVariableDetails.getOperator().equals("between")) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(sqlVariableDetails.getType().size() > 1 ? (String) sqlVariableDetails.getType().get(1).replace("DD", "dd") : "YYYY");
            if (StringUtils.endsWith(sqlVariableDetails.getId(), START_END_SEPARATOR)) {
                return simpleDateFormat.format(new Date(Long.parseLong((String) sqlVariableDetails.getValue().get(1))));
            } else {
                return simpleDateFormat.format(new Date(Long.parseLong((String) sqlVariableDetails.getValue().get(0))));
            }
        } else {
            return (String) sqlVariableDetails.getValue().get(0);
        }
    }
}
