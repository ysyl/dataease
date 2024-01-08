package io.dataease.dataset.manage;

import io.dataease.api.chart.dto.ChartExtFilterDTO;
import io.dataease.api.chart.request.ChartExtRequest;
import io.dataease.api.dataset.dto.DatasetTableDTO;
import io.dataease.api.dataset.dto.SqlVariableDetails;
import io.dataease.api.dataset.union.*;
import io.dataease.api.dataset.union.model.SQLObj;
import io.dataease.api.permissions.auth.dto.BusiPerCheckDTO;
import io.dataease.commons.utils.SqlparserUtils;
import io.dataease.constant.AuthEnum;
import io.dataease.dataset.constant.DatasetTableType;
import io.dataease.dataset.dto.DatasourceSchemaDTO;
import io.dataease.dataset.utils.DatasetTableTypeConstants;
import io.dataease.dataset.utils.SqlUtils;
import io.dataease.dataset.utils.TableUtils;
import io.dataease.datasource.dao.auto.entity.CoreDatasource;
import io.dataease.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.dataease.datasource.server.EngineServer;
import io.dataease.dto.dataset.DatasetTableFieldDTO;
import io.dataease.engine.constant.ExtFieldConstant;
import io.dataease.engine.constant.SQLConstants;
import io.dataease.exception.DEException;
import io.dataease.i18n.Translator;
import io.dataease.system.manage.CorePermissionManage;
import io.dataease.utils.BeanUtils;
import io.dataease.utils.JsonUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Junjun
 */
@Component
public class DatasetSQLManage {

    @Resource
    private CoreDatasourceMapper coreDatasourceMapper;
    @Resource
    private EngineServer engineServer;

    @Resource
    private CorePermissionManage corePermissionManage;

    private static Logger logger = LoggerFactory.getLogger(DatasetSQLManage.class);

    private List<SqlVariableDetails> filterParameters(ChartExtRequest chartExtRequest, Long datasetTableId) {
        List<SqlVariableDetails> parameters = new ArrayList<>();
        if (chartExtRequest != null && ObjectUtils.isNotEmpty(chartExtRequest.getFilter())) {
            for (ChartExtFilterDTO filterDTO : chartExtRequest.getFilter()) {
                if (CollectionUtils.isEmpty(filterDTO.getValue())) {
                    continue;
                }
                if (ObjectUtils.isNotEmpty(filterDTO.getParameters())) {
                    for (SqlVariableDetails parameter : filterDTO.getParameters()) {
                        if (parameter.getDatasetTableId().equals(datasetTableId)) {
                            parameter.setValue(filterDTO.getValue());
                            parameter.setOperator(filterDTO.getOperator());
                            parameters.add(parameter);
                        }
                    }
                }
            }
        }
        return parameters;
    }

    public Map<String, Object> getUnionSQLForEdit(DatasetGroupInfoDTO dataTableInfoDTO, ChartExtRequest chartExtRequest) throws Exception {
        Map<Long, DatasourceSchemaDTO> dsMap = new LinkedHashMap<>();
        List<UnionDTO> union = dataTableInfoDTO.getUnion();
        // 所有选中的字段，即select后的查询字段
        Map<String, String[]> checkedInfo = new LinkedHashMap<>();
        List<UnionParamDTO> unionList = new ArrayList<>();
        List<DatasetTableFieldDTO> checkedFields = new ArrayList<>();
        String sql = "";

        if (ObjectUtils.isEmpty(union)) {
            return null;
        }

        DatasetTableDTO currentDs = union.get(0).getCurrentDs();

        // get datasource and schema,put map
        String tableSchema = putObj2Map(dsMap, currentDs);
        // get table
        DatasetTableInfoDTO infoDTO = JsonUtil.parseObject(currentDs.getInfo(), DatasetTableInfoDTO.class);


        SQLObj tableName = getUnionTable(currentDs, infoDTO, tableSchema, 0, filterParameters(chartExtRequest, currentDs.getId()), chartExtRequest == null);

        for (int i = 0; i < union.size(); i++) {
            UnionDTO unionDTO = union.get(i);
            DatasetTableDTO datasetTable = unionDTO.getCurrentDs();
            DatasetTableInfoDTO tableInfo = JsonUtil.parseObject(datasetTable.getInfo(), DatasetTableInfoDTO.class);

            String schema;
            if (dsMap.containsKey(datasetTable.getDatasourceId())) {
                schema = dsMap.get(datasetTable.getDatasourceId()).getSchemaAlias();
            } else {
                schema = putObj2Map(dsMap, datasetTable);
            }
            SQLObj table = getUnionTable(datasetTable, tableInfo, schema, i, filterParameters(chartExtRequest, currentDs.getId()), chartExtRequest == null);

            // 获取前端传过来选中的字段
            List<DatasetTableFieldDTO> fields = unionDTO.getCurrentDsFields();
            fields = fields.stream().filter(DatasetTableFieldDTO::getChecked).collect(Collectors.toList());

            String[] array = fields.stream()
                    .map(f -> {
                        String alias;
                        if (StringUtils.isEmpty(f.getDataeaseName())) {
                            alias = TableUtils.fieldNameShort(table.getTableAlias() + "_" + f.getOriginName());
                        } else {
                            alias = f.getDataeaseName();
                        }

                        f.setFieldShortName(alias);
                        f.setDataeaseName(f.getFieldShortName());
                        f.setDatasetTableId(datasetTable.getId());
                        String prefix = "";
                        if (Objects.equals(f.getExtField(), ExtFieldConstant.EXT_NORMAL)) {
                            prefix = "`";
                        }
                        return table.getTableAlias() + "." + prefix + f.getOriginName() + prefix + " AS " + alias;
                    })
                    .toArray(String[]::new);
            checkedInfo.put(table.getTableAlias(), array);
            checkedFields.addAll(fields);
            // 获取child的fields和union
            if (!CollectionUtils.isEmpty(unionDTO.getChildrenDs())) {
                getUnionForEdit(datasetTable, table, unionDTO.getChildrenDs(), checkedInfo, unionList, checkedFields, dsMap, chartExtRequest);
            }
        }
        // build sql
        if (!CollectionUtils.isEmpty(unionList)) {
            // field
            StringBuilder field = new StringBuilder();
            for (Map.Entry<String, String[]> next : checkedInfo.entrySet()) {
                if (next.getValue().length > 0) {
                    field.append(StringUtils.join(next.getValue(), ",")).append(",");
                }
            }
            String f = subPrefixSuffixChar(field.toString());
            // join
            StringBuilder join = new StringBuilder();
            for (UnionParamDTO unionParamDTO : unionList) {
                // get join type
                String joinType = convertUnionTypeToSQL(unionParamDTO.getUnionType());

                SQLObj parentSQLObj = unionParamDTO.getParentSQLObj();
                SQLObj currentSQLObj = unionParamDTO.getCurrentSQLObj();

                String ts = "";
                String tablePrefix = "";
                if (ObjectUtils.isNotEmpty(currentSQLObj.getTableSchema())) {
                    ts = currentSQLObj.getTableSchema() + ".";
                    tablePrefix = "`";
                }
                // build join
                join.append(" ").append(joinType).append(" ")
                        .append(ts)
                        .append(tablePrefix + currentSQLObj.getTableName() + tablePrefix)
                        .append(" ").append(currentSQLObj.getTableAlias()).append(" ")
                        .append(" ON ");
                if (unionParamDTO.getUnionFields().size() == 0) {
                    DEException.throwException(Translator.get("i18n_union_field_can_not_empty"));
                }
                for (int i = 0; i < unionParamDTO.getUnionFields().size(); i++) {
                    UnionItemDTO unionItemDTO = unionParamDTO.getUnionFields().get(i);
                    // 通过field id取得field详情，并且以第一组为准，寻找dataset table
                    DatasetTableFieldDTO parentField = unionItemDTO.getParentField();
                    DatasetTableFieldDTO currentField = unionItemDTO.getCurrentField();
                    String pPrefix = "";
                    if (Objects.equals(parentField.getExtField(), ExtFieldConstant.EXT_NORMAL)) {
                        pPrefix = "`";
                    }
                    String cPrefix = "";
                    if (Objects.equals(currentField.getExtField(), ExtFieldConstant.EXT_NORMAL)) {
                        cPrefix = "`";
                    }
                    join.append(parentSQLObj.getTableAlias()).append(".")
                            .append(pPrefix + parentField.getOriginName() + pPrefix)
                            .append(" = ")
                            .append(currentSQLObj.getTableAlias()).append(".")
                            .append(cPrefix + currentField.getOriginName() + cPrefix);
                    if (i < unionParamDTO.getUnionFields().size() - 1) {
                        join.append(" AND ");
                    }
                }
            }
            if (StringUtils.isEmpty(f)) {
                DEException.throwException(Translator.get("i18n_union_ds_no_checked"));
            }
            sql = MessageFormat.format("SELECT {0} FROM {1}", f, TableUtils.getTableAndAlias(tableName)) + join.toString();
        } else {
            String f = StringUtils.join(checkedInfo.get(tableName.getTableAlias()), ",");
            if (StringUtils.isEmpty(f)) {
                DEException.throwException(Translator.get("i18n_union_ds_no_checked"));
            }
            sql = MessageFormat.format("SELECT {0} FROM {1}", f, TableUtils.getTableAndAlias(tableName));
        }
        logger.info("calcite origin sql: " + sql);
        Map<String, Object> map = new HashMap<>();
        map.put("sql", sql);
        map.put("field", checkedFields);
        map.put("join", unionList);
        map.put("dsMap", dsMap);
        return map;
    }

    // 递归计算出所有子级的checkedFields和unionParam
    private void getUnionForEdit(DatasetTableDTO parentTable, SQLObj parentSQLObj,
                                 List<UnionDTO> childrenDs, Map<String, String[]> checkedInfo,
                                 List<UnionParamDTO> unionList, List<DatasetTableFieldDTO> checkedFields,
                                 Map<Long, DatasourceSchemaDTO> dsMap, ChartExtRequest chartExtRequest) throws Exception {
        for (int i = 0; i < childrenDs.size(); i++) {
            int index = unionList.size() + 1;

            UnionDTO unionDTO = childrenDs.get(i);
            DatasetTableDTO datasetTable = unionDTO.getCurrentDs();
            DatasetTableInfoDTO tableInfo = JsonUtil.parseObject(datasetTable.getInfo(), DatasetTableInfoDTO.class);

            String schema;
            if (dsMap.containsKey(datasetTable.getDatasourceId())) {
                schema = dsMap.get(datasetTable.getDatasourceId()).getSchemaAlias();
            } else {
                schema = putObj2Map(dsMap, datasetTable);
            }
            SQLObj table = getUnionTable(datasetTable, tableInfo, schema, index, filterParameters(chartExtRequest, datasetTable.getId()), chartExtRequest == null);

            List<DatasetTableFieldDTO> fields = unionDTO.getCurrentDsFields();
            fields = fields.stream().filter(DatasetTableFieldDTO::getChecked).collect(Collectors.toList());

            String[] array = fields.stream()
                    .map(f -> {
                        String alias;
                        if (StringUtils.isEmpty(f.getDataeaseName())) {
                            alias = TableUtils.fieldNameShort(table.getTableAlias() + "_" + f.getOriginName());
                        } else {
                            alias = f.getDataeaseName();
                        }

                        f.setFieldShortName(alias);
                        f.setDataeaseName(f.getFieldShortName());
                        f.setDatasetTableId(datasetTable.getId());
                        String prefix = "";
                        if (Objects.equals(f.getExtField(), ExtFieldConstant.EXT_NORMAL)) {
                            prefix = "`";
                        }
                        return table.getTableAlias() + "." + prefix + f.getOriginName() + prefix + " AS " + alias;
                    })
                    .toArray(String[]::new);
            checkedInfo.put(table.getTableAlias(), array);
            checkedFields.addAll(fields);

            UnionParamDTO unionToParent = unionDTO.getUnionToParent();
            // 设置关联关系中，两个table信息
            unionToParent.setParentDs(parentTable);
            unionToParent.setParentSQLObj(parentSQLObj);
            unionToParent.setCurrentDs(datasetTable);
            unionToParent.setCurrentSQLObj(table);
            unionList.add(unionToParent);
            if (!CollectionUtils.isEmpty(unionDTO.getChildrenDs())) {
                getUnionForEdit(datasetTable, table, unionDTO.getChildrenDs(), checkedInfo, unionList, checkedFields, dsMap, chartExtRequest);
            }
        }
    }

    public String subPrefixSuffixChar(String str) {
        while (StringUtils.startsWith(str, ",")) {
            str = str.substring(1, str.length());
        }
        while (StringUtils.endsWith(str, ",")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    private String convertUnionTypeToSQL(String unionType) {
        switch (unionType) {
            case "1:1":
            case "inner":
                return " INNER JOIN ";
            case "1:N":
            case "left":
                return " LEFT JOIN ";
            case "N:1":
            case "right":
                return " RIGHT JOIN ";
            case "N:N":
            case "full":
                return " FULL JOIN ";
            default:
                return " INNER JOIN ";
        }
    }

    private SQLObj getUnionTable(DatasetTableDTO currentDs, DatasetTableInfoDTO infoDTO, String tableSchema, int index, List<SqlVariableDetails> parameters, boolean isFromDataSet) {
        SQLObj tableObj;
        String tableAlias = String.format(SQLConstants.TABLE_ALIAS_PREFIX, index);
        if (StringUtils.equalsIgnoreCase(currentDs.getType(), DatasetTableTypeConstants.DATASET_TABLE_DB)) {
            tableObj = SQLObj.builder().tableSchema(tableSchema).tableName(infoDTO.getTable()).tableAlias(tableAlias).build();
        } else if (StringUtils.equalsIgnoreCase(currentDs.getType(), DatasetTableTypeConstants.DATASET_TABLE_SQL)) {
            // parser sql params and replace default value
            String sql = SqlparserUtils.handleVariableDefaultValue(new String(Base64.getDecoder().decode(infoDTO.getSql())), currentDs.getSqlVariableDetails(), false, isFromDataSet, parameters);
            // add table schema
            sql = SqlUtils.addSchema(sql, tableSchema);
            tableObj = SQLObj.builder().tableSchema("").tableName("(" + sql + ")").tableAlias(tableAlias).build();
        } else {
            // excel,api
            tableObj = SQLObj.builder().tableSchema(tableSchema).tableName(infoDTO.getTable()).tableAlias(tableAlias).build();
        }
        return tableObj;
    }

    private String putObj2Map(Map<Long, DatasourceSchemaDTO> dsMap, DatasetTableDTO ds) throws Exception {
        // 通过datasource id校验数据源权限
        BusiPerCheckDTO dto = new BusiPerCheckDTO();
        dto.setId(ds.getDatasourceId());
        dto.setAuthEnum(AuthEnum.READ);
        boolean checked = corePermissionManage.checkAuth(dto);
        if (!checked) {
            DEException.throwException(Translator.get("i18n_no_datasource_permission"));
        }


        String schemaAlias;
        if (StringUtils.equalsIgnoreCase(ds.getType(), DatasetTableType.DB) || StringUtils.equalsIgnoreCase(ds.getType(), DatasetTableType.SQL)) {
            CoreDatasource coreDatasource = coreDatasourceMapper.selectById(ds.getDatasourceId());
            if (coreDatasource == null) {
                DEException.throwException(Translator.get("i18n_dataset_ds_error") + ",ID:" + ds.getDatasourceId());
            }
            if (StringUtils.equalsIgnoreCase("excel", coreDatasource.getType()) || StringUtils.equalsIgnoreCase("api", coreDatasource.getType())) {
                coreDatasource = engineServer.getDeEngine();
            }
            schemaAlias = String.format(SQLConstants.SCHEMA, coreDatasource.getId());
            if (!dsMap.containsKey(coreDatasource.getId())) {
                DatasourceSchemaDTO datasourceSchemaDTO = new DatasourceSchemaDTO();
                BeanUtils.copyBean(datasourceSchemaDTO, coreDatasource);
                datasourceSchemaDTO.setSchemaAlias(schemaAlias);
                dsMap.put(coreDatasource.getId(), datasourceSchemaDTO);
            }
        } else {
            CoreDatasource coreDatasource = engineServer.getDeEngine();
            schemaAlias = String.format(SQLConstants.SCHEMA, coreDatasource.getId());
            if (!dsMap.containsKey(coreDatasource.getId())) {
                DatasourceSchemaDTO datasourceSchemaDTO = new DatasourceSchemaDTO();
                BeanUtils.copyBean(datasourceSchemaDTO, coreDatasource);
                datasourceSchemaDTO.setSchemaAlias(schemaAlias);
                dsMap.put(coreDatasource.getId(), datasourceSchemaDTO);
            }
        }
        return schemaAlias;
    }
}
