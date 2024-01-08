package io.dataease.chart.server;

import io.dataease.api.chart.ChartViewApi;
import io.dataease.api.chart.dto.ChartViewDTO;
import io.dataease.api.chart.dto.ChartViewFieldDTO;
import io.dataease.chart.manage.ChartViewManege;
import io.dataease.exception.DEException;
import io.dataease.result.ResultCode;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @Author Junjun
 */
@RestController
@RequestMapping("chart")
public class ChartViewServer implements ChartViewApi {
    @Resource
    private ChartViewManege chartViewManege;

    @Override
    public ChartViewDTO getData(Long id) throws Exception {
        try {
            return chartViewManege.getChart(id);
        } catch (Exception e) {
            DEException.throwException(ResultCode.DATA_IS_WRONG.code(), e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, List<ChartViewFieldDTO>> listByDQ(Long id, Long chartId) {
        return chartViewManege.listByDQ(id, chartId);
    }

    @Override
    public ChartViewDTO save(ChartViewDTO dto) throws Exception {
        return chartViewManege.save(dto);
    }

    @Override
    public String checkSameDataSet(String viewIdSource, String viewIdTarget) {
        return chartViewManege.checkSameDataSet(viewIdSource, viewIdTarget);
    }

    @Override
    public ChartViewDTO getDetail(Long id) {
        return chartViewManege.getDetails(id);
    }


}
