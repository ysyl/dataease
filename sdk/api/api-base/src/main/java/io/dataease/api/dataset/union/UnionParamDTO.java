package io.dataease.api.dataset.union;

import io.dataease.api.dataset.dto.DatasetTableDTO;
import io.dataease.api.dataset.union.model.SQLObj;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author gin
 */
@Data
public class UnionParamDTO implements Serializable {
    private String unionType;
    private List<UnionItemDTO> unionFields;
    private DatasetTableDTO parentDs;
    private DatasetTableDTO currentDs;
    private SQLObj parentSQLObj;
    private SQLObj currentSQLObj;
}
