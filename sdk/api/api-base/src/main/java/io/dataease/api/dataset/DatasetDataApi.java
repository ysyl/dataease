package io.dataease.api.dataset;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.dataease.api.dataset.dto.DatasetTableDTO;
import io.dataease.api.dataset.dto.PreviewSqlDTO;
import io.dataease.api.dataset.union.DatasetGroupInfoDTO;
import io.dataease.dto.dataset.DatasetTableFieldDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * @Author Junjun
 */
@Tag(name = "数据集管理:数据")
@ApiSupport(order = 978)
public interface DatasetDataApi {
    @Operation(summary = "预览数据")
    @PostMapping("previewData")
    Map<String, Object> previewData(@RequestBody DatasetGroupInfoDTO datasetGroupInfoDTO) throws Exception;

    @Operation(summary = "获取数据集节点字段")
    @PostMapping("tableField")
    List<DatasetTableFieldDTO> tableField(@RequestBody DatasetTableDTO datasetTableDTO) throws Exception;

    @Operation(summary = "SQL预览")
    @PostMapping("previewSql")
    Map<String, Object> previewSql(@RequestBody PreviewSqlDTO dto) throws Exception;

    @Operation(summary = "sql片段校验", hidden = true)
    @PostMapping("previewSqlCheck")
    Map<String, Object> previewSqlCheck(@RequestBody PreviewSqlDTO dto) throws Exception;

    @Operation(summary = "获取字段枚举值")
    @PostMapping("enumValue")
    List<String> getFieldEnum(@RequestBody List<Long> ids) throws Exception;

    @Operation(summary = "获取数据集总数据量", hidden = true)
    @PostMapping("getDatasetCount")
    Long getDatasetCount(@RequestBody DatasetGroupInfoDTO datasetGroupInfoDTO) throws Exception;
}
