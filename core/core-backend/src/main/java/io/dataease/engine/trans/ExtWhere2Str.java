package io.dataease.engine.trans;

import io.dataease.api.chart.dto.ChartExtFilterDTO;
import io.dataease.api.dataset.union.model.SQLMeta;
import io.dataease.api.dataset.union.model.SQLObj;
import io.dataease.dto.dataset.DatasetTableFieldDTO;
import io.dataease.engine.constant.SQLConstants;
import io.dataease.engine.utils.Utils;
import org.apache.calcite.func.scalar.ScalarFunctions;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Junjun
 */
public class ExtWhere2Str {

    public static void extWhere2sqlOjb(SQLMeta meta, List<ChartExtFilterDTO> fields, List<DatasetTableFieldDTO> originFields) {
        SQLObj tableObj = meta.getTable();
        if (ObjectUtils.isEmpty(tableObj)) {
            return;
        }
        List<SQLObj> list = new ArrayList<>();
        if (ObjectUtils.isNotEmpty(fields)) {
            for (ChartExtFilterDTO request : fields) {
                List<String> value = request.getValue();

                List<String> whereNameList = new ArrayList<>();
                List<DatasetTableFieldDTO> fieldList = new ArrayList<>();
                if (request.getIsTree()) {
                    fieldList.addAll(request.getDatasetTableFieldList());
                } else {
                    fieldList.add(request.getDatasetTableField());
                }

                for (DatasetTableFieldDTO field : fieldList) {
                    if (ObjectUtils.isEmpty(value) || ObjectUtils.isEmpty(field)) {
                        continue;
                    }
                    String whereName = "";

                    String originName;
                    if (ObjectUtils.isNotEmpty(field.getExtField()) && field.getExtField() == 2) {
                        // 解析origin name中有关联的字段生成sql表达式
                        originName = Utils.calcFieldRegex(field.getOriginName(), tableObj, originFields);
                    } else if (ObjectUtils.isNotEmpty(field.getExtField()) && field.getExtField() == 1) {
                        originName = String.format(SQLConstants.FIELD_NAME, tableObj.getTableAlias(), field.getDataeaseName());
                    } else {
                        originName = String.format(SQLConstants.FIELD_NAME, tableObj.getTableAlias(), field.getDataeaseName());
                    }

                    if (field.getDeType() == 1) {
                        String date_format;
                        if (StringUtils.containsIgnoreCase(request.getOperator(), "between")) {
                            date_format = "yyyy-MM-dd HH:mm:ss";
                        } else {
                            date_format = ScalarFunctions.get_date_format(value.get(0));
                        }
                        if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                            // 此处获取标准格式的日期
                            whereName = String.format(SQLConstants.DE_CAST_DATE_FORMAT, originName, StringUtils.isEmpty(field.getDateFormat()) ? SQLConstants.DEFAULT_DATE_FORMAT : field.getDateFormat(), date_format);
                        }
                        if (field.getDeExtractType() == 2 || field.getDeExtractType() == 3 || field.getDeExtractType() == 4) {
                            String cast = String.format(SQLConstants.CAST, originName, SQLConstants.DEFAULT_INT_FORMAT);
                            // 此处获取标准格式的日期
                            whereName = String.format(SQLConstants.FROM_UNIXTIME, cast, date_format);
                            whereName = String.format(SQLConstants.UNIX_TIMESTAMP, whereName);
                        }
                        if (field.getDeExtractType() == 1) {
                            // 此处获取标准格式的日期
                            whereName = String.format(SQLConstants.CAST_DATE_FORMAT, originName, StringUtils.isEmpty(field.getDateFormat()) ? SQLConstants.DEFAULT_DATE_FORMAT : field.getDateFormat(), date_format);
                        }
                    } else if (field.getDeType() == 2 || field.getDeType() == 3) {
                        if (field.getDeExtractType() == 0 || field.getDeExtractType() == 5) {
                            whereName = String.format(SQLConstants.CAST, originName, SQLConstants.DEFAULT_FLOAT_FORMAT);
                        }
                        if (field.getDeExtractType() == 1) {
                            whereName = String.format(SQLConstants.UNIX_TIMESTAMP, originName);
                        }
                        if (field.getDeExtractType() == 2 || field.getDeExtractType() == 4) {
                            whereName = originName;
                        }
                        if (field.getDeExtractType() == 3) {
                            whereName = String.format(SQLConstants.CAST, originName, SQLConstants.DEFAULT_FLOAT_FORMAT);
                        }
                    } else {
                        whereName = originName;
                    }
                    whereNameList.add(whereName);
                }

                String whereName = "";
                if (request.getIsTree()) {
                    whereName = "CONCAT(" + StringUtils.join(whereNameList, ",',',") + ")";
                } else {
                    whereName = whereNameList.get(0);
                }
                String whereTerm = Utils.transFilterTerm(request.getOperator());
                String whereValue = "";

                if (StringUtils.containsIgnoreCase(request.getOperator(), "in")) {
                    whereValue = "('" + StringUtils.join(value, "','") + "')";
                } else if (StringUtils.containsIgnoreCase(request.getOperator(), "like")) {
                    whereValue = "'%" + value.get(0) + "%'";
                } else if (StringUtils.containsIgnoreCase(request.getOperator(), "between")) {
                    if (request.getDatasetTableField().getDeType() == 1) {
                        if (request.getDatasetTableField().getDeExtractType() == 2
                                || request.getDatasetTableField().getDeExtractType() == 3
                                || request.getDatasetTableField().getDeExtractType() == 4) {
                            whereValue = String.format(SQLConstants.WHERE_VALUE_BETWEEN, value.get(0), value.get(1));
                        } else {
                            whereName = String.format(SQLConstants.UNIX_TIMESTAMP, whereName);
                            whereValue = String.format(SQLConstants.WHERE_BETWEEN, Long.parseLong(value.get(0)), Long.parseLong(value.get(1)));
                        }
                    } else {
                        whereValue = String.format(SQLConstants.WHERE_BETWEEN, value.get(0), value.get(1));
                    }
                } else {
                    whereValue = String.format(SQLConstants.WHERE_VALUE_VALUE, value.get(0));
                }
                list.add(SQLObj.builder()
                        .whereField(whereName)
                        .whereTermAndValue(whereTerm + whereValue)
                        .build());
            }
            List<String> strList = new ArrayList<>();
            list.forEach(ele -> strList.add(ele.getWhereField() + " " + ele.getWhereTermAndValue()));
            meta.setExtWheres(ObjectUtils.isNotEmpty(list) ? "(" + String.join(" AND ", strList) + ")" : null);
        }
    }

}
