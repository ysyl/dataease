package io.dataease.api.chart;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.dataease.api.chart.dto.ChartViewDTO;
import io.dataease.api.chart.request.ChartExcelRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.List;

/**
 * @Author Junjun
 */
@Tag(name = "视图管理:数据")
@ApiSupport(order = 989)
public interface ChartDataApi {
    @Operation(summary = "获取视图数据")
    @PostMapping("getData")
    ChartViewDTO getData(@RequestBody ChartViewDTO chartViewDTO) throws Exception;

    @Operation(summary = "导出数据")
    @PostMapping("innerExportDetails")
    void innerExportDetails(@RequestBody ChartExcelRequest request, HttpServletResponse response) throws Exception;

    @Operation(summary = "获取字段值")
    @PostMapping("getFieldData/{fieldId}/{fieldType}")
    List<String> getFieldData(@RequestBody ChartViewDTO view, @PathVariable Long fieldId, @PathVariable String fieldType) throws Exception;
}
